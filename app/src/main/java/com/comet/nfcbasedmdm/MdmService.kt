package com.comet.nfcbasedmdm

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.comet.nfcbasedmdm.handler.WebSocketHandler
import com.comet.nfcbasedmdm.model.WebSocketMessage
import com.comet.nfcbasedmdm.util.StringUtil
import com.fasterxml.jackson.databind.ObjectMapper
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.SocketTimeoutException
import java.nio.charset.StandardCharsets
import java.security.Key
import java.security.KeyFactory
import java.security.PublicKey
import java.security.spec.X509EncodedKeySpec
import java.util.*
import java.util.concurrent.TimeUnit
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec

const val LOG_TAG = "NFC_MDM"
const val CHANNEL_ID = "NFC_MDM_CHANNEL"
const val NDM_CHANGE = "NDM_CHANGE"
const val TIMEOUT = 3L

class MdmService : Service() {

    private lateinit var receiver: ComponentName
    private lateinit var policy: DevicePolicyManager
    private lateinit var thread: Thread
    private lateinit var handler: WebSocketHandler
    private val mapper: ObjectMapper = ObjectMapper()
    private lateinit var encryptKey: String
    private val uuid = UUID.fromString("eabf6f0b-0da1-44f0-82d8-b29b33d6e33a") //임시 uuid
    private val auth = "ewvG6EQOYH"
    private val del = "iNb3HREfEm"
    private val client = OkHttpClient.Builder().connectTimeout(TIMEOUT, TimeUnit.SECONDS)
        .readTimeout(TIMEOUT, TimeUnit.SECONDS).build() //2초 정도로 지정
    private val request = Request.Builder().url("ws://192.168.56.1:8080/mdm").build()
    private val keyRequest = Request.Builder().url("http://192.168.56.1:8080/encrypt").build()

    companion object {
        var isRunning = false
    }

    override fun onBind(p0: Intent?): IBinder? {
        TODO("Not yet implemented")
    }


    override fun onDestroy() {
        thread.interrupt() //running 취소
        isRunning = false
        (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).cancel(1) //notification 취소

    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.i(LOG_TAG, "SERVICE STARTED")
        policy = getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        receiver = ComponentName(packageName, "$packageName.AdminReceiver")

        handler = WebSocketHandler(this)
        if (policy.isAdminActive(receiver)) {
            //어드민 권한 흭득했을경우.
            // TODO pref 데이터 있는지 확인.
            initChannel() //notification 실행
            thread = Thread {
                while (!Thread.interrupted()) {
                    try {
                        run()
                        Thread.sleep(10000) //connection 유지
                    }
                    catch (e: InterruptedException) {
                        break
                    }
                }
            }.also { it.start() }
            isRunning = true
        } else
            stopSelf()
        return START_STICKY //다시 시작, 인텐트 null
    }

    override fun onUnbind(intent: Intent?): Boolean {
        return super.onUnbind(intent)
    }

    private fun disableCamera(status: Boolean) {
        policy.setCameraDisabled(receiver, status)
    }

    private fun run() {
        // TODO 서버 연결, sharedpref 사용하여 상태 유지하기.
        //공개키 얻는 과정
        if (!handler.isOpen) {
            try {
                encryptKey = mapper.readTree(
                    client.newCall(keyRequest).execute().body.string()
                ).get("data").textValue()
                val socket = client.newWebSocket(request, handler)
                val message = WebSocketMessage(
                    WebSocketMessage.Status.HAND_SHAKE,
                    encryptWithServer(uuid.toString())
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
        Log.i(LOG_TAG, text)
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
                val decrypt = decrypt(data, uuid.toString())
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
                    if (auth != this.auth)
                        Log.w(LOG_TAG, "invalid request")
                    else {
                        disableCamera(value)
                        sendCameraChange(value)
                        handler.sendMessage(
                            mapper.writeValueAsString(
                                WebSocketMessage(
                                    WebSocketMessage.Status.RESPONSE,
                                    encryptWithServer(
                                        auth + "|${StringUtil.generateRandomString(5)}|" + policy.getCameraDisabled(
                                            receiver
                                        )
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

    private fun encryptWithServer(input: String): String {
        return try {
            val keyFactory = KeyFactory.getInstance("RSA")
            val bytePublicKey: ByteArray = Base64.getUrlDecoder().decode(encryptKey)
            val publicKeySpec = X509EncodedKeySpec(bytePublicKey)
            val publicKey: PublicKey = keyFactory.generatePublic(publicKeySpec)
            val cipher: Cipher = Cipher.getInstance("RSA/NONE/PKCS1Padding")
                .apply { init(Cipher.ENCRYPT_MODE, publicKey) } //패딩 문제로 맞춰줘야됨.
            val encryptByte: ByteArray = cipher.doFinal(input.toByteArray(StandardCharsets.UTF_8))
            Base64.getUrlEncoder()
                .encodeToString(encryptByte)
        } catch (e: Exception) {
            e.printStackTrace()
            ""
        }
    }

    private fun decrypt(input: String, key: String): String {
        val secretKeySpec: Key = SecretKeySpec(key.substring(0, 16).toByteArray(), "AES")
        return try {
            val cipher: Cipher =
                Cipher.getInstance("AES").apply { init(Cipher.DECRYPT_MODE, secretKeySpec) }
            val urlDecode = Base64.getUrlDecoder().decode(input.toByteArray())
            val decrypt: ByteArray = cipher.doFinal(urlDecode)
            String(decrypt)
        } catch (e: Exception) {
            ""
        }
    }
    // 코틀린 람다는 중괄호로 묶고 (x : type, y : type) -> lambda


}