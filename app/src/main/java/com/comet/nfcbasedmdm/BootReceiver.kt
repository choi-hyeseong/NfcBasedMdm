package com.comet.nfcbasedmdm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.comet.nfcbasedmdm.service.LOG_TAG
import com.comet.nfcbasedmdm.service.MdmService

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        intent?.let {
            val action = intent.action
            if (action.equals("android.intent.action.BOOT_COMPLETED")) {
                //부팅 실행시
                Log.i(LOG_TAG, "BOOT RECEIVED")
                if (!MdmService.isRunning) //서비스가 실행중이 아니라면.
                    context?.startForegroundService(Intent(context, MdmService::class.java))
            }
        }

    }
}