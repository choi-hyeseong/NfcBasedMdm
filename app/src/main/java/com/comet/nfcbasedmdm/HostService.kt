package com.comet.nfcbasedmdm


import android.content.Intent
import android.nfc.cardemulation.HostApduService

import android.os.Bundle
import android.util.Log

const val CARD_ACTION = "ACTION_CARD_ACTION"

class HostService : HostApduService() {

    companion object {
        const val MSG_RESPONSE_APDU = 1
        const val KEY_DATA = "data"
    }

    override fun processCommandApdu(commandApdu : ByteArray?, extras : Bundle?) : ByteArray {
        Log.i(LOG_TAG, "Reading Card Reader")
        sendBroadcast(Intent().setAction(CARD_ACTION))
        return ByteArray(0);
    }

    override fun onDeactivated(reason : Int) {
        Log.i(LOG_TAG, "nfc disconnected.")
    }

    override fun onStartCommand(intent : Intent?, flags : Int, startId : Int) : Int {
        return START_STICKY
    }

}