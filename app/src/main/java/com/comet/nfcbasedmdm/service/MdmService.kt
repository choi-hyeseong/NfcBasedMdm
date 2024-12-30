package com.comet.nfcbasedmdm.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.app.admin.DevicePolicyManager
import android.app.usage.UsageStats
import android.app.usage.UsageStatsManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager.GET_PERMISSIONS
import android.content.pm.PackageManager.NameNotFoundException
import android.os.*
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.comet.nfcbasedmdm.R
import com.comet.nfcbasedmdm.handler.WebSocketHandler
import com.comet.nfcbasedmdm.mdm.data.model.MDMData
import com.comet.nfcbasedmdm.mdm.connection.websocket.model.WebSocketMessage
import com.comet.nfcbasedmdm.common.util.EncryptUtil.Companion.AESDecrypt
import com.comet.nfcbasedmdm.common.util.EncryptUtil.Companion.RSAEncrypt
import com.fasterxml.jackson.databind.ObjectMapper
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.*
import java.util.concurrent.TimeUnit


const val LOG_TAG = "NFC_MDM"
const val CHANNEL_ID = "NFC_MDM_CHANNEL"
const val NDM_CHANGE = "NDM_CHANGE"
const val NDM_SERVER_CHANGE = "NDM_SERVER_CHANGE"
const val TIMEOUT = 2L
const val FILE_NAME = "encrypted_file"
val DENY_PERMISSION = listOf("android.permission.CAMERA", "android.permission.RECORD_AUDIO")

class MdmService : Service() {

    // 코틀린 람다는 중괄호로 묶고 (x : type, y : type) -> lambda
    private lateinit var receiver : ComponentName
    private lateinit var policy : DevicePolicyManager
    private lateinit var thread : Thread
    private lateinit var handler : WebSocketHandler
    private var appThread : Thread? = null //P이상 버젼 전용 쓰레드 변수
    lateinit var encryptKey : String
    var mdmData : MDMData? = null

    /*private val uuid = UUID.fromString("eabf6f0b-0da1-44f0-82d8-b29b33d6e33a") //임시 uuid
    private val auth = "ewvG6EQOYH"
    private val del = "iNb3HREfEm"*/
    private val mapper : ObjectMapper = ObjectMapper()
    private val client = OkHttpClient.Builder().connectTimeout(TIMEOUT, TimeUnit.SECONDS)
        .readTimeout(TIMEOUT, TimeUnit.SECONDS).build() //2초 정도로 지정
    private val request : Request by lazy {
        Request.Builder().url("ws://${mdmData?.ip}/mdm").build() //어처피 데이터 사용하는 경우는 mdm이 null이 아닐때만.
    }
    private val keyRequest : Request by lazy {
        Request.Builder().url("http://${mdmData?.ip}/encrypt").build()
    }
    private val binder = LocalBinder()

    //암호화된 preference
    private val preferences : SharedPreferences by lazy {
        val masterKey = MasterKey.Builder(applicationContext, MasterKey.DEFAULT_MASTER_KEY_ALIAS)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build()

        EncryptedSharedPreferences.create(
            applicationContext,
            FILE_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    companion object {
        var isRunning = false
    }

    inner class LocalBinder : Binder() {

        fun getService() : MdmService {
            return this@MdmService //mdm this 접근하기 위해선 inner 사용필수.
        }
    }

    override fun onBind(p0 : Intent?) : IBinder {
        //다른 프로세스 접근 고려 X
        return binder
    }


    override fun onDestroy() {
        thread.interrupt() //running 취소
        save()
        isRunning = false
        appThread?.interrupt() //앱 확인 취소
        appThread = null
        (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).cancel(1) //notification 취소
    }

    override fun onStartCommand(intent : Intent?, flags : Int, startId : Int) : Int {
        Log.i(LOG_TAG, "SERVICE STARTED")
        policy = getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        receiver = ComponentName(packageName, "$packageName.AdminReceiver")
        handler = WebSocketHandler(this)
        load()
        initChannel() //notification 실행

        thread = Thread {
            while (!Thread.interrupted()) {
                try {
                    //mdm 서비스가 실행가능한 경우
                    if (isMDMRegistered() && isAdminActivated())
                        run()
                    Thread.sleep(10000) //connection 유지
                }
                catch (e : InterruptedException) {
                    break
                }
            }
        }.also { it.start() }
        if (isMDMRegistered() && mdmData?.isEnabled!!)
            runMDMThread()
        isRunning = true

        return START_STICKY //다시 시작, 인텐트 null
    }

    @Suppress("DEPRECATION")
    //api 33이상
    private fun getPermissions(pack : String) : List<String> {
        val list = ArrayList<String>()
        try {
            val packInfo = packageManager.getPackageInfo(pack, GET_PERMISSIONS)
            if (packInfo.requestedPermissions != null)
                list.addAll(packInfo.requestedPermissions)
        }
        catch (e : NameNotFoundException) {
            return list
        }
        return list
    }

    private fun getTopApplicationPackage() : String {
        var topPackageName = ""
        val mUsageStatsManager = getSystemService(USAGE_STATS_SERVICE) as UsageStatsManager
        val time = System.currentTimeMillis()
        // We get usage stats for the last 10 seconds
        val stats = mUsageStatsManager.queryUsageStats(UsageStatsManager.INTERVAL_DAILY,
                                                       time - 1000 * 10,
                                                       time)
        // Sort the stats by the last time used
        if (stats != null) {
            val mySortedMap : SortedMap<Long, UsageStats> = TreeMap()
            for (usageStats in stats) {
                mySortedMap[usageStats.lastTimeUsed] = usageStats
            }
            if (!mySortedMap.isEmpty()) {
                topPackageName = mySortedMap[mySortedMap.lastKey()]!!.packageName

            }
        }
        return topPackageName
    }

    override fun onUnbind(intent : Intent?) : Boolean {
        return super.onUnbind(intent)
    }

    fun isMDMExecuted() : Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
            appThread != null
        else
            policy.getCameraDisabled(receiver)

    }

    private fun runMDMThread() {
        appThread = Thread {
            while (!Thread.interrupted()) {
                try {
                    Thread.sleep(500)
                    val permission = getPermissions(getTopApplicationPackage())
                    if (permission.contains(DENY_PERMISSION[0]) || permission.contains(
                            DENY_PERMISSION[1])) { //펄미션 인식 안되면 튕기게 해놓고 왜 버그 찾고있냐 ㅋㅋ....
                        Log.w(LOG_TAG, "founded deny application.")
                        Handler(Looper.getMainLooper()).post {
                            startActivity(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
                                              .apply {
                                                  flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                              })
                        }


                    }
                }
                catch (e : InterruptedException) {
                    break
                }
            }

        }.also { it.start() }
    }
    private fun disableCamera(status : Boolean) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            //10이상일경우
            preferences.edit().putBoolean("mdm", status).apply()
            if (status && appThread == null) {
                //현재 실행중이 아닐경우.
                runMDMThread()
            }
            else {
                appThread?.interrupt()
                appThread = null
            }
        }
        else {
            policy.setCameraDisabled(receiver, status)
        }
        //policy.setUninstallBlocked(receiver, packageName, status) <- profile owner 설정필요
    }

    fun isAdminActivated() : Boolean {
        return policy.isAdminActive(receiver)
    }

    fun getRequestAdminIntent() : Intent {
        return Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).putExtra(
            DevicePolicyManager.EXTRA_DEVICE_ADMIN,
            receiver
        )
    }

    fun isMDMRegistered() : Boolean {
        //서버와 연결되어 uuid값이 존재하는가?
        return mdmData != null //초기화가 되지 않은경우 회원가입이 되지 않은것.
        //null이 아닌경우만 활성화 된거지..ㅜㅜㅜㅜㅜ
    }

    private fun save() {
        mdmData?.apply {
            preferences.edit().putString("uuid", uuid.toString()).putString("auth", authID)
                .putString("delete", deleteID).apply()
        }
    }

    fun save(uuid : String, auth : String, delete : String, ip : String) {
        preferences.edit().putString("uuid", uuid).putString("auth", auth)
            .putString("delete", delete).putString("ip", ip).apply()
        mdmData = MDMData(UUID.fromString(uuid), delete, auth, ip, false) //초기화

    }

    private fun load() {
        val uuid = preferences.getString("uuid", null)
        val auth = preferences.getString("auth", null)
        val delete = preferences.getString("delete", null)
        val ip = preferences.getString("ip", null)
        val isEnabled = preferences.getBoolean("mdm", false)
        if (!uuid.isNullOrEmpty() && !auth.isNullOrEmpty() && !delete.isNullOrEmpty() && !ip.isNullOrEmpty())
            mdmData = MDMData(UUID.fromString(uuid), delete, auth, ip, isEnabled)

    }


    fun isServerConnected() : Boolean {
        return handler.isOpen
    }

    private fun run() {
        //공개키 얻는 과정
        if (!isServerConnected()) {
            try {
                val result = client.newCall(keyRequest).execute().body.string()
                encryptKey = JSONObject(
                    result
                ).get("data").toString()

                val socket = client.newWebSocket(request, handler)
                val message = WebSocketMessage(
                    WebSocketMessage.Status.HAND_SHAKE,
                    RSAEncrypt(mdmData?.uuid.toString(), encryptKey)
                ) //handshake
                socket.send(mapper.writeValueAsString(message))
            }
            catch (e : Exception) {
                Log.w(LOG_TAG, "Request encountered timeout. Retry in 10 seconds.")
            }
        }
    }


    fun onMessage(text : String) {
        val message = mapper.readValue(text, WebSocketMessage::class.java) //직렬화 문제잖아..
        val status = message.status
        val data = message.data
        when (status) {
            WebSocketMessage.Status.PING -> {
                //PING 요청
                val result = handler.sendMessage(
                    mapper.writeValueAsString(
                        WebSocketMessage(
                            WebSocketMessage.Status.PONG,
                            "RESPONSE PING"
                        )
                    )
                )
                Log.i(LOG_TAG, "ping result : $result")
            }
            WebSocketMessage.Status.EXECUTE_MDM -> {
                //MDM 요청
                val decrypt = AESDecrypt(data, mdmData?.uuid.toString())
                if (decrypt.isEmpty())
                    Log.w(LOG_TAG, "can't execute mdm")
                else {
                    val split = decrypt.split("|")
                    if (split.size != 3) {
                        Log.w(LOG_TAG, "invalid data")
                        return
                    }

                    val auth = split[0]
                    val time = split[1].toLong()
                    val value = split[2].toBoolean()
                    if (auth != mdmData?.authID && !checkTimeValid(time))
                        Log.w(LOG_TAG, "invalid request")
                    else {
                        disableCamera(value)
                        sendCameraChange(value)
                        handler.sendMessage(
                            mapper.writeValueAsString(
                                WebSocketMessage(
                                    WebSocketMessage.Status.RESPONSE,
                                    RSAEncrypt(
                                        "$auth|${System.currentTimeMillis()}|${
                                            isMDMExecuted()
                                        }", encryptKey
                                    )
                                )
                            )
                        )
                    }

                }
            }
            else -> return
        }


    }

    private fun initChannel() {
        val title = getString(R.string.app_name)
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        var channel = manager.getNotificationChannel(CHANNEL_ID)
        if (channel == null) {
            channel =
                NotificationChannel(CHANNEL_ID, title, NotificationManager.IMPORTANCE_HIGH)
            manager.createNotificationChannel(channel)
        }
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentText(getString(R.string.channel_content)).build()
        startForeground(1, notification)
    }

    private fun sendCameraChange(status : Boolean) {
        sendBroadcast(Intent().setAction(NDM_CHANGE).putExtra("status", status))
    }

    fun sendServerStatChange(status : Boolean) {
        sendBroadcast(Intent().setAction(NDM_SERVER_CHANGE).putExtra("status", status))
    }

    private fun checkTimeValid(time : Long) : Boolean {
        // 2초보다 작은 경우 올바른 응답
        return (System.currentTimeMillis() - time) <= (TIMEOUT * 2000)
    }


}