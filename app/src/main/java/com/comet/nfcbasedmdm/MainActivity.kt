package com.comet.nfcbasedmdm

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.NotificationCompat
import androidx.fragment.app.Fragment
import com.comet.nfcbasedmdm.callback.ActivityCallback

class MainActivity : AppCompatActivity(), ActivityCallback {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val policy = getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        val receiver = ComponentName(packageName, "$packageName.AdminReceiver")

        if (!policy.isAdminActive(receiver)) {
            val adminIntent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN)
            val result = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {}
            /** TODO 굳이 결과 필요 없으면 startActivity 써도 되지 않을까? -> 나중에 관리자 권한 활성화 여부 체크**/
            adminIntent.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, receiver)
            result.launch(adminIntent)
        }
        if (!MdmService.isRunning)
            startForegroundService(Intent(this, MdmService::class.java))
        switch(MainFragment(), false)
    }

    override fun switch(frag: Fragment, replace: Boolean) {
        if (replace)
            supportFragmentManager.beginTransaction().addToBackStack(null).replace(R.id.frame, frag)
                .commit()
        else
            supportFragmentManager.beginTransaction().addToBackStack(null).add(R.id.frame, frag)
                .commit()
    }

    override fun disableCamera(status: Boolean) {

    }
}