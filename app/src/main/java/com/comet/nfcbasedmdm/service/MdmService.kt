package com.comet.nfcbasedmdm.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.comet.nfcbasedmdm.R
import com.comet.nfcbasedmdm.camera.usecase.CheckCameraEnabledUseCase
import com.comet.nfcbasedmdm.camera.usecase.DisableCameraUseCase
import com.comet.nfcbasedmdm.camera.usecase.EnableCameraUseCase
import com.comet.nfcbasedmdm.common.cipher.AESCrypto
import com.comet.nfcbasedmdm.common.cipher.RSACrypto
import com.comet.nfcbasedmdm.getClassName
import com.comet.nfcbasedmdm.mdm.connection.key.usecase.GetPublicKeyUseCase
import com.comet.nfcbasedmdm.mdm.connection.websocket.callback.WebSocketCallback
import com.comet.nfcbasedmdm.mdm.connection.websocket.model.WebSocketMessage
import com.comet.nfcbasedmdm.mdm.connection.websocket.type.WebSocketStatus
import com.comet.nfcbasedmdm.mdm.connection.websocket.usecase.WebSocketConnectUseCase
import com.comet.nfcbasedmdm.mdm.connection.websocket.usecase.WebSocketDisconnectUseCase
import com.comet.nfcbasedmdm.mdm.connection.websocket.usecase.WebSocketSendMessageUseCase
import com.comet.nfcbasedmdm.mdm.data.model.MDMData
import com.comet.nfcbasedmdm.mdm.data.usecase.GetMDMDataUseCase
import com.comet.nfcbasedmdm.mdm.data.usecase.SaveMDMDataUseCase
import com.comet.nfcbasedmdm.mdm.view.MainFragment.Companion.NDM_CHANGE
import com.comet.nfcbasedmdm.mdm.view.MainFragment.Companion.NDM_SERVER_CHANGE
import com.skydoves.sandwich.getOrNull
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MdmService : Service(), WebSocketCallback {

    companion object {
        private const val CHANNEL_ID = "NFC_MDM_CHANNEL" // notification timeout
        private const val TIMEOUT = 2L // websocket message timeout
    }

    // hilt inject

    // get mdm data
    @Inject
    lateinit var getMDMDataUseCase: GetMDMDataUseCase

    @Inject
    lateinit var saveMDMDataUseCase: SaveMDMDataUseCase

    // camera enable / disable / get info
    @Inject
    lateinit var enableCameraUseCase: EnableCameraUseCase

    @Inject
    lateinit var disableCameraUseCase: DisableCameraUseCase

    @Inject
    lateinit var checkCameraEnabledUseCase: CheckCameraEnabledUseCase

    // websocket
    @Inject
    lateinit var webSocketConnectUseCase: WebSocketConnectUseCase

    @Inject
    lateinit var webSocketDisconnectUseCase: WebSocketDisconnectUseCase

    @Inject
    lateinit var webSocketSendMessageUseCase: WebSocketSendMessageUseCase

    // for crypt
    @Inject
    lateinit var rsaCrypto: RSACrypto

    @Inject
    lateinit var aesCrypto: AESCrypto

    // public key retrieve
    @Inject
    lateinit var getPublicKeyUseCase: GetPublicKeyUseCase

    // 맨처음 초기화될 mdmData
    lateinit var mdmData: MDMData


    private fun startNotification() {
        val title = getString(R.string.app_name)
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        var channel = manager.getNotificationChannel(CHANNEL_ID)
        if (channel == null) {
            channel = NotificationChannel(CHANNEL_ID, title, NotificationManager.IMPORTANCE_HIGH)
            manager.createNotificationChannel(channel)
        }
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentText(getString(R.string.channel_content))
            .build()
        startForeground(1, notification)
    }

    private fun stopNotification() {
        (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).cancel(1) //notification 취소
    }

    // 서비스 시작
    private fun startService() {
        Log.i(getClassName(), "SERVICE STARTED")
        startNotification() //notification 실행
        CoroutineScope(Dispatchers.IO).launch {
            val data = getMDMDataUseCase()
            if (data == null) {
                Log.e(getClassName(), "MDM Data isn't initialized.")
                stopService()
                return@launch
            }

            mdmData = data
            initCamera() // 카메라 금지여부 설정
            webSocketConnectUseCase(data.ip, this@MdmService) // websocket connect
        }
    }

    // 최초 서비스 실행시 카메라 상태 변경
    private fun initCamera() {
        val isMDMEnabled = mdmData.isEnabled
        // 설정상태와 허용상태가 반전된경우 (true - false / false - true)
        if (isMDMEnabled == !checkCameraEnabledUseCase()) return
        changeCameraEnabledStatus(!isMDMEnabled)
    }

    override fun onMessage(message: WebSocketMessage) {
        val status = message.status
        val data = message.data
        when (status) {
            WebSocketStatus.PING -> {
                //PING 요청 - heart beat
                CoroutineScope(Dispatchers.IO).launch {
                    kotlin.runCatching {
                        webSocketSendMessageUseCase(WebSocketMessage(WebSocketStatus.PONG, "RESPONSE PING"))
                    }.onSuccess {
                        Log.i(getClassName(), "pong success")
                    }.onFailure {
                        Log.e(getClassName(), "pong failure")
                    }
                }
            }

            WebSocketStatus.EXECUTE_MDM -> {
                //MDM 요청
                // uuid decrypt
                val decrypt = aesCrypto.decrypt(data, mdmData.uuid.toString())
                if (decrypt.isEmpty()) {
                    Log.w(getClassName(), "can't execute mdm")
                    return
                }
                // 올바른 요청인지 확인
                val split = decrypt.split("|")
                if (split.size != 3) {
                    Log.w(getClassName(), "invalid mdm data")
                    return
                }
                val authId = split[0]
                val time = split[1].toLong()
                val cameraDenied = split[2].toBoolean()
                if (authId != mdmData.authID && !checkTimeValid(time)) {
                    Log.w(getClassName(), "invalid request")
                    return
                }
                changeCameraEnabledStatus(!cameraDenied)
                sendCameraStatusChange(!cameraDenied)
                CoroutineScope(Dispatchers.IO).launch {
                    val rsaKey = getPublicKeyUseCase(mdmData.ip).getOrNull()
                    if (rsaKey == null) {
                        // 만약 RSA 퍼블릭키를 못가져온경우 일단 로깅.
                        // MDM DATA 초기화시 얘도 초기화 안함? -> 그 사이에 서버 껏다 켜져서 퍼블릭키 바뀌면?
                        Log.e(getClassName(), "Can't retrieve RSA Public Key..")
                        return@launch
                    }
                    val response: String = authId.plus("|")
                        .plus(System.currentTimeMillis().toString())
                        .plus("|")
                        .plus((!checkCameraEnabledUseCase()).toString())
                    val encryptResponse: String = rsaCrypto.encrypt(response, rsaKey.data)
                    if (encryptResponse.isEmpty()) {
                        // 암호화 실패한경우 리턴
                        Log.e(getClassName(), "Can't encrypt response Message..")
                        return@launch
                    }
                    webSocketSendMessageUseCase(WebSocketMessage(WebSocketStatus.RESPONSE, encryptResponse))
                }
            }

            else -> return
        }
    }

    private fun checkTimeValid(time: Long): Boolean {
        // 2초보다 작은 경우 올바른 응답
        return (System.currentTimeMillis() - time) <= (TIMEOUT * 2000)
    }

    // 서비스 종료
    private fun stopService() {
        Log.i(getClassName(), "SERVICE STOPPED")
        stopNotification()
        CoroutineScope(Dispatchers.IO).launch {
            webSocketDisconnectUseCase()
        }
    }


    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        startService()
        return START_STICKY //다시 시작, 인텐트 null
    }

    override fun onDestroy() {
        super.onDestroy()
        stopService()
    }

    // bind 안씀
    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    // 카메라 허용여부 변경
    private fun changeCameraEnabledStatus(enabled: Boolean) {
        if (enabled) //허용
            enableCameraUseCase()
        else // 비허용
            disableCameraUseCase()
        mdmData.isEnabled = enabled
        CoroutineScope(Dispatchers.IO).launch {
            saveMDMDataUseCase(mdmData)
        }
    }

    // 카메라 잠금여부 변환 알려줌
    private fun sendCameraStatusChange(status: Boolean) {
        sendBroadcast(Intent(NDM_CHANGE).putExtra("status", status))
    }

    // 서버 연결 여부 알려줌
    private fun sendServerStatusChange(status: Boolean) {
        sendBroadcast(Intent(NDM_SERVER_CHANGE).putExtra("status", status))
    }


    override fun onOpen() {
        // 서버 오픈 알림
        Log.i(getClassName(), "Socket Opened")
        sendServerStatusChange(true)
        CoroutineScope(Dispatchers.IO).launch {
            // 웹소켓 연결시 hand shake 시행
            val rsaKey = getPublicKeyUseCase(mdmData.ip).getOrNull()
            if (rsaKey == null) {
                Log.e(getClassName(), "Can't retrieve rsa key")
                return@launch
            }
            webSocketSendMessageUseCase(WebSocketMessage(WebSocketStatus.HAND_SHAKE, rsaCrypto.encrypt(mdmData.uuid.toString(), rsaKey.data)))
        }

    }

    override fun onClose(reason: String) {
        // 서버 닫힘 알림
        sendServerStatusChange(false)
    }


}