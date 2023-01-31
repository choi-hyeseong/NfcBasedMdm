package com.comet.nfcbasedmdm.callback

import android.content.BroadcastReceiver
import android.content.IntentFilter
import androidx.fragment.app.Fragment

interface ActivityCallback {

    fun switch(frag : Fragment, replace: Boolean)

    fun saveToken(uuid : String, auth : String, delete : String, ip: String)

    fun registerActivityReceiver(receiver: BroadcastReceiver, filter: IntentFilter)

    fun runOnMainThread(r : Runnable)

    fun getServerStatus() : Boolean

    fun getMdmStatus() : Boolean

    fun getContextString(resource : Int) : String

    fun getContextColor(resource : Int) : Int
}