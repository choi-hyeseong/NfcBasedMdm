package com.comet.nfcbasedmdm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.comet.nfcbasedmdm.callback.ActivityCallback

class MainFragment : Fragment() {

    private var callback : ActivityCallback? = null
    private lateinit var serverText : TextView
    private lateinit var ndmText : TextView
    private lateinit var lockImage : ImageView

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
        serverText = view.findViewById(R.id.serverStat)
        ndmText = view.findViewById(R.id.ndmText)
        lockImage = view.findViewById(R.id.lockImage)
        val filter = IntentFilter()
        filter.addAction(NDM_CHANGE)
        filter.addAction(NDM_SERVER_CHANGE)
        changeServerStatus(callback?.getServerStatus()!!)
        changeMdmStatus(callback?.getMdmStatus()!!)
        callback?.registerActivityReceiver(NdmReceiver(), filter)
        return view
    }

    private inner class NdmReceiver : BroadcastReceiver(){
        override fun onReceive(context: Context?, intent: Intent?) {
            intent?.let {
                if (intent.action == NDM_CHANGE)
                    changeMdmStatus(intent.getBooleanExtra("status", false))
                else if (intent.action == NDM_SERVER_CHANGE)
                    changeServerStatus(intent.getBooleanExtra("status", false))
            }
        }
    }

    private fun changeServerStatus(status : Boolean) {
        if (status) {
            //connected
            serverText.text = callback?.getContextString(R.string.ndm_server_connected)
            serverText.setTextColor(callback?.getContextColor(R.color.primaryNotifyColor)!!)
        }
        else {
            serverText.text = callback?.getContextString(R.string.ndm_server_not_connected)
            serverText.setTextColor(callback?.getContextColor(R.color.primaryAlertColor)!!)
        }
    }

    private fun changeMdmStatus(status : Boolean) {
        if (status) {
            ndmText.text = callback?.getContextString(R.string.ndm_executed)
            serverText.setTextColor(callback?.getContextColor(R.color.primaryNotifyColor)!!)
            lockImage.setImageResource(R.drawable.lockmain)
        }
        else {
            ndmText.text = callback?.getContextString(R.string.ndm_not_executed)
            serverText.setTextColor(callback?.getContextColor(R.color.primaryAlertColor)!!)
            lockImage.setImageResource(R.drawable.unlock)
        }
    }
}