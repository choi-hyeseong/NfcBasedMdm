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

const val LOG_TAG = "NFC_MDM"
const val CHANNEL_ID = "NFC_MDM_CHANNEL"

class MdmService : Service() {

    private lateinit var receiver: ComponentName
    private lateinit var policy : DevicePolicyManager
    private lateinit var thread: Thread

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
        if (policy.isAdminActive(receiver)) {
            //어드민 권한 흭득했을경우.
            initChannel() //notification 실행
            thread = Thread {
                while (!Thread.interrupted()) {
                    try {
                        // TODO 서버 연결, sharedpref 사용하여 상태 유지하기.
                        Thread.sleep(10000)
                        disableCamera(!policy.getCameraDisabled(receiver))
                        Log.i(LOG_TAG, "${policy.getCameraDisabled(receiver)}")
                    } catch (e: InterruptedException) {
                        break
                    }
                }
            }.also { it.start() }
            isRunning = true
        }
        else
            stopSelf()
        return START_STICKY //다시 시작, 인텐트 null
    }

    override fun onUnbind(intent: Intent?): Boolean {
        return super.onUnbind(intent)
    }

    fun disableCamera(status: Boolean) {
        policy.setCameraDisabled(receiver, status)
    }

    private fun initChannel() {
        val title = getString(R.string.app_name)
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        var channel = manager.getNotificationChannel(CHANNEL_ID)
        if (channel == null) {
            channel = NotificationChannel(CHANNEL_ID, title, NotificationManager.IMPORTANCE_HIGH)
            manager.createNotificationChannel(channel)
        }
        val notification = NotificationCompat.Builder(this, CHANNEL_ID).setSmallIcon(R.drawable.ic_launcher_foreground).setContentText(getString(R.string.channel_content)).build()
        startForeground(1, notification)
    }


}