package com.comet.nfcbasedmdm.callback

import android.content.BroadcastReceiver
import android.content.IntentFilter
import androidx.fragment.app.Fragment

interface ActivityCallback {

    fun switch(frag : Fragment, replace: Boolean)

    fun disableCamera(status : Boolean)

    fun registerActivityReceiver(receiver: BroadcastReceiver, filter: IntentFilter)
}