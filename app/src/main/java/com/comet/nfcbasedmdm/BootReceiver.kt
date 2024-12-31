package com.comet.nfcbasedmdm

import android.content.Context
import android.content.Intent
import android.util.Log
import com.comet.nfcbasedmdm.common.receiver.AbstractBroadcastReceiver
import dagger.hilt.android.AndroidEntryPoint

/**
 * 부팅 감지 리시버
 */
@AndroidEntryPoint
class BootReceiver : AbstractBroadcastReceiver() {

    override fun isSupport(action: String): Boolean {
        return action == "android.intent.action.BOOT_COMPLETED"
    }

    // 부팅시 mdm 시작
    override fun handleIntent(context: Context, intent: Intent) {
        Log.i(getClassName(), "BOOT RECEIVED")
        context.startActivity(Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        })
    }
}