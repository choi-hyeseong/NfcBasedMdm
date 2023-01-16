package com.comet.nfcbasedmdm

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CompoundButton
import android.widget.CompoundButton.OnCheckedChangeListener
import android.widget.Switch
import androidx.fragment.app.Fragment
import com.comet.nfcbasedmdm.callback.ActivityCallback
import com.google.android.material.switchmaterial.SwitchMaterial

class MainFragment : Fragment() {

    private var callback : ActivityCallback? = null

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
        val switch = view.findViewById<SwitchMaterial>(R.id.switch1)
        switch.setOnCheckedChangeListener { _, isEnabled ->
            callback?.disableCamera(isEnabled)
        }
        return view
    }
}