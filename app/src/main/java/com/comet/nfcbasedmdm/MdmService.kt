package com.comet.nfcbasedmdm

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Binder
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.comet.nfcbasedmdm.handler.WebSocketHandler
import com.comet.nfcbasedmdm.model.MDMData
import com.comet.nfcbasedmdm.model.WebSocketMessage
import com.comet.nfcbasedmdm.util.EncryptUtil.Companion.AESDecrypt
import com.comet.nfcbasedmdm.util.EncryptUtil.Companion.RSAEncrypt
import com.comet.nfcbasedmdm.util.StringUtil
import com.fasterxml.jackson.databind.ObjectMapper
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.net.SocketTimeoutException
import java.util.*
import java.util.concurrent.TimeUnit

const val LOG_TAG = "NFC_MDM"
const val CHANNEL_ID = "NFC_MDM_CHANNEL"
const val NDM_CHANGE = "NDM_CHANGE"
const val TIMEOUT = 3L
const val FILE_NAME = "encrypted_file"

class MdmService : Service() {

    // 코틀린 람다는 중괄호로 묶고 (x : type, y : type) -> lambda
    private lateinit var receiver: ComponentName
    private lateinit var policy: DevicePolicyManager
    private lateinit var thread: Thread
    private lateinit var handler: WebSocketHandler
    private lateinit var encryptKey: String
    private var mdmData: MDMData? = null

    /*private val uuid = UUID.fromString("eabf6f0b-0da1-44f0-82d8-b29b33d6e33a") //임시 uuid
    private val auth = "ewvG6EQOYH"
    private val del = "iNb3HREfEm"*/
    private val mapper: ObjectMapper = ObjectMapper()
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
    private val preferences: SharedPreferences by lazy {
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

        fun getService(): MdmService {
            return this@MdmService //mdm this 접근하기 위해선 inner 사용필수.
        }
    }

    override fun onBind(p0: Intent?): IBinder {
        //다른 프로세스 접근 고려 X
        return binder
    }


    override fun onDestroy() {
        thread.interrupt() //running 취소
        save()
        isRunning = false
        (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).cancel(1) //notification 취소
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
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
                } catch (e: InterruptedException) {
                    break
                }
            }
        }.also { it.start() }
        isRunning = true

        return START_STICKY //다시 시작, 인텐트 null
    }

    override fun onUnbind(intent: Intent?): Boolean {
        return super.onUnbind(intent)
    }

    private fun disableCamera(status: Boolean) {
        policy.setCameraDisabled(receiver, status)
    }

    fun isAdminActivated(): Boolean {
        return policy.isAdminActive(receiver)
    }

    fun getRequestAdminIntent(): Intent {
        return Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).putExtra(
            DevicePolicyManager.EXTRA_DEVICE_ADMIN,
            receiver
        )
    }

    fun isMDMRegistered(): Boolean {
        //서버와 연결되어 uuid값이 존재하는가?
        return mdmData != null //초기화가 되지 않은경우 회원가입이 되지 않은것.
        //null이 아닌경우만 활성화 된거지..ㅜㅜㅜㅜㅜ
    }

    private fun save() {
        mdmData?.apply {
            preferences.edit().putString("uuid", uuid.toString()).putString("auth", auth)
                .putString("delete", delete).apply()
        }
    }

    fun save(uuid: String, auth: String, delete: String, ip : String) {
        preferences.edit().putString("uuid", uuid).putString("auth", auth)
            .putString("delete", delete).putString("ip", ip).apply()
        mdmData = MDMData(UUID.fromString(uuid), delete, auth, ip) //초기화

    }

    private fun load() {
        val uuid = preferences.getString("uuid", null)
        val auth = preferences.getString("auth", null)
        val delete = preferences.getString("delete", null)
        val ip = preferences.getString("ip", null)
        if (!uuid.isNullOrEmpty() && !auth.isNullOrEmpty() && !delete.isNullOrEmpty() && !ip.isNullOrEmpty())
            mdmData = MDMData(UUID.fromString(uuid), delete, auth, ip)

    }

    private fun run() {
        // TODO 서버 연결, sharedpref 사용하여 상태 유지하기.
        //공개키 얻는 과정
        if (!handler.isOpen) {
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
            } catch (e: SocketTimeoutException) {
                Log.w(LOG_TAG, "Request encountered timeout. Retry in 10 seconds.")
            }
        }
    }

    fun onMessage(text: String) {
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
                    val value = split[2].toBoolean()
                    if (auth != mdmData?.auth)
                        Log.w(LOG_TAG, "invalid request")
                    else {
                        disableCamera(value)
                        sendCameraChange(value)
                        handler.sendMessage(
                            mapper.writeValueAsString(
                                WebSocketMessage(
                                    WebSocketMessage.Status.RESPONSE,
                                    RSAEncrypt(
                                        auth + "|${StringUtil.generateRandomString(5)}|" + policy.getCameraDisabled(
                                            receiver
                                        ), encryptKey
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
            channel = NotificationChannel(CHANNEL_ID, title, NotificationManager.IMPORTANCE_HIGH)
            manager.createNotificationChannel(channel)
        }
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentText(getString(R.string.channel_content)).build()
        startForeground(1, notification)
    }

    private fun sendCameraChange(status: Boolean) {
        val intent = Intent().setAction(NDM_CHANGE).putExtra("status", status)
        sendBroadcast(intent)
    }


}