package com.comet.nfcbasedmdm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Switch
import androidx.fragment.app.Fragment
import com.comet.nfcbasedmdm.callback.ActivityCallback
import com.google.android.material.switchmaterial.SwitchMaterial

class MainFragment : Fragment() {

    private var callback : ActivityCallback? = null
    private lateinit var switch : SwitchMaterial
    private lateinit var serverSwitch : SwitchMaterial

    override fun onAttach(context: Context) {
        super.onAttach(context)
        callback = context as ActivityCallback
    }

    override fun onDetach() {
        super.onDetach()
        callback = null
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_main, container, false)
        switch = view.findViewById(R.id.switch1)
        serverSwitch = view.findViewById<SwitchMaterial?>(R.id.serverstat).apply { callback?.let { isChecked = it.getServerStatus() }}
        val filter = IntentFilter()
        filter.addAction(NDM_CHANGE)
        filter.addAction(NDM_SERVER_CHANGE)
        callback?.registerActivityReceiver(NdmReceiver(), filter)
        return view
    }

    private inner class NdmReceiver : BroadcastReceiver(){
        override fun onReceive(context: Context?, intent: Intent?) {
            intent?.let {
                if (intent.action == NDM_CHANGE)
                    switch.isChecked = intent.getBooleanExtra("status", false)
                else if (intent.action == NDM_SERVER_CHANGE)
                    serverSwitch.isChecked = intent.getBooleanExtra("status", false)
            }
        }

    }
}