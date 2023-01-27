package com.comet.nfcbasedmdm

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
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import com.comet.nfcbasedmdm.callback.ActivityCallback

class MainActivity : AppCompatActivity(), ActivityCallback {

    private lateinit var service: MdmService
    private var isConnected = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val intent = Intent(this, MdmService::class.java)
        if (!MdmService.isRunning)
            startForegroundService(intent)
        bindService(intent) //서비스 바인딩 (이미 위에서 실행됨)
        val result =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {} //create 시작시 바로 초기화 해야됨.
        Handler(Looper.getMainLooper()).postDelayed({
            if (!service.isAdminActivated()) {
                val adminIntent = service.getRequestAdminIntent()

                /** TODO 굳이 결과 필요 없으면 startActivity 써도 되지 않을까? -> 나중에 관리자 권한 활성화 여부 체크**/
                result.launch(adminIntent)
            }
            if (!service.isMDMRegistered())
                switch(RegisterFragment(), false)
            else
                switch(MainFragment(), false)
        }, 2000L) //서비스 바인딩까지 2초 기다리기.


    }

    private fun bindService(intent: Intent) {
        bindService(intent, object : ServiceConnection {
            override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
                this@MainActivity.service = (service as MdmService.LocalBinder).getService()
                isConnected = true
            }

            override fun onServiceDisconnected(name: ComponentName?) {
                isConnected = false
            }

        }, BIND_AUTO_CREATE)
    }

    override fun switch(frag: Fragment, replace: Boolean) {
        if (replace)
            supportFragmentManager.beginTransaction().addToBackStack(null).replace(R.id.frame, frag)
                .commit()
        else
            supportFragmentManager.beginTransaction().replace(R.id.frame, frag)
                .commit()
    }

    override fun saveToken(uuid: String, auth: String, delete: String, ip: String) {
        if (isConnected)
            service.save(uuid, auth, delete, ip)
    }


    override fun registerActivityReceiver(receiver: BroadcastReceiver, filter: IntentFilter) {
        registerReceiver(receiver, filter)
    }

    override fun runOnMainThread(r: Runnable) {
        runOnUiThread(r)
    }
}