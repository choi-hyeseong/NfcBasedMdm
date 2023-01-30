package com.comet.nfcbasedmdm

import android.app.Application
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Intent
import android.content.IntentFilter
import android.content.ServiceConnection
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import com.comet.nfcbasedmdm.callback.ActivityCallback
import kotlin.system.exitProcess

class MainActivity : AppCompatActivity(), ActivityCallback {

    private lateinit var service : MdmService
    private lateinit var result : ActivityResultLauncher<Intent>
    private var isConnected = false


    override fun onCreate(savedInstanceState : Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val intent = Intent(this, MdmService::class.java)
        if (!MdmService.isRunning)
            startForegroundService(intent)
        bindService(intent) //서비스 바인딩 (이미 위에서 실행됨)
        result =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
                //result == 활성화 이후 작동 + 서비스 연결이후 액티비티 결과 반환
                if (isConnected && !service.isAdminActivated()) {
                    Toast.makeText(this, getString(R.string.admin_error_text), Toast.LENGTH_LONG)
                        .show()
                    result.launch(service.getRequestAdminIntent())
                }
            }


    }

    private fun bindService(intent : Intent) {
        bindService(intent, object : ServiceConnection {
            override fun onServiceConnected(name : ComponentName?, service : IBinder?) {
                this@MainActivity.service = (service as MdmService.LocalBinder).getService()
                isConnected = true
                main()
            }

            override fun onServiceDisconnected(name : ComponentName?) {
                isConnected = false
            }

        }, BIND_AUTO_CREATE)
    }

    private fun main() {
        if (!service.isAdminActivated()) {
            val adminIntent = service.getRequestAdminIntent()
            result.launch(adminIntent)
        }
        if (!service.isMDMRegistered())
            switch(RegisterFragment(), false)
        else
            switch(MainFragment(), false)
    }

    override fun switch(frag : Fragment, replace : Boolean) {
        if (replace)
            supportFragmentManager.beginTransaction().addToBackStack(null).replace(R.id.frame, frag)
                .commit()
        else
            supportFragmentManager.beginTransaction().replace(R.id.frame, frag)
                .commit()
    }

    override fun saveToken(uuid : String, auth : String, delete : String, ip : String) {
        if (isConnected)
            service.save(uuid, auth, delete, ip)
    }


    override fun registerActivityReceiver(receiver : BroadcastReceiver, filter : IntentFilter) {
        registerReceiver(receiver, filter)
    }

    override fun runOnMainThread(r : Runnable) {
        runOnUiThread(r)
    }
}