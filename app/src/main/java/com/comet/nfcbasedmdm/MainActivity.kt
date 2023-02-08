package com.comet.nfcbasedmdm

import android.Manifest
import android.app.AppOpsManager
import android.content.*
import android.content.pm.PackageManager
import android.nfc.NfcAdapter
import android.nfc.cardemulation.CardEmulation
import android.os.*
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.comet.nfcbasedmdm.callback.ActivityCallback
import com.comet.nfcbasedmdm.util.ApduUtil
import com.comet.nfcbasedmdm.util.EncryptUtil


class MainActivity : AppCompatActivity(), ActivityCallback {

    private lateinit var service : MdmService
    private lateinit var result : ActivityResultLauncher<Intent>
    private lateinit var overlay : ActivityResultLauncher<Intent>
    private var isConnected = false
    private lateinit var messenger : Messenger


    override fun onCreate(savedInstanceState : Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        supportActionBar?.hide() //액션바 숨기기
        val intent = Intent(this, MdmService::class.java)
        if (!MdmService.isRunning)
            startForegroundService(intent)
        bindService(intent) //서비스 바인딩 (이미 위에서 실행됨)
        if (NfcAdapter.getDefaultAdapter(this) != null)
            setupCard()
        result =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
                //result == 활성화 이후 작동 + 서비스 연결이후 액티비티 결과 반환
                if (isConnected && !service.isAdminActivated()) {
                    Toast.makeText(this, getString(R.string.admin_error_text), Toast.LENGTH_LONG)
                        .show()
                    result.launch(service.getRequestAdminIntent())
                }
            }
        overlay = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            //사용시간 권한 활성화 체크
            if (!isUsageEnabled()) {
                overlay.launch(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
            }
        }


    }

    private fun isUsageEnabled() : Boolean {
        val appOps = getSystemService(APP_OPS_SERVICE) as AppOpsManager
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            AppOpsManager.MODE_ALLOWED == appOps.unsafeCheckOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), packageName)
        }
        else {
           return true //Q 미만일경우 굳이 할필요 없음.
        }
    }

    private fun setupCard() {
        val component = ComponentName(this, HostService::class.java)
        val adapter = NfcAdapter.getDefaultAdapter(this)
        val dynamic = arrayListOf("F239856324897348")
        CardEmulation.getInstance(adapter).also { it.registerAidsForService(component, "other", dynamic) }
        bindService(Intent(this, HostService::class.java), object : ServiceConnection {
            override fun onServiceConnected(name : ComponentName?, service : IBinder?) {
                messenger = Messenger(service)
            }

            override fun onServiceDisconnected(name : ComponentName?) {
                Log.i(LOG_TAG, "Messenger disconnected")
            }

        }, BIND_AUTO_CREATE)
        val filter = IntentFilter().also { it.addAction(CARD_ACTION) }
        registerReceiver(object : BroadcastReceiver() {
            override fun onReceive(context : Context?, intent : Intent?) {
                intent?.let {
                    if (it.action == CARD_ACTION) {
                        val responseMsg = Message.obtain(null, HostService.MSG_RESPONSE_APDU)
                        val response : ByteArray = if (getServerStatus() && service.isMDMRegistered()) {
                            //registered 에서 체크함
                            ApduUtil.buildApdu("${EncryptUtil.RSAEncrypt("${service.mdmData!!.uuid}|${System.currentTimeMillis()}", service.encryptKey)}|Android".toByteArray())
                        } else
                            ByteArray(0)
                        val data = Bundle().apply { putByteArray(HostService.KEY_DATA,response) }
                        responseMsg.data = data
                        try {
                            messenger.send(responseMsg)
                        }
                        catch (e : RemoteException) {
                            e.localizedMessage?.let { Log.w(LOG_TAG, it) }
                        }
                    }
                }
            }
        }, filter)
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
        if (!isUsageEnabled())
            overlay.launch(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))

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

    override fun getServerStatus() : Boolean {
        return service.isServerConnected()
    }

    override fun getMdmStatus() : Boolean {
        return service.isMDMExecuted()
    }

    override fun getContextString(resource : Int) : String {
        return getString(resource)
    }

    override fun getContextColor(resource : Int) : Int {
        return ContextCompat.getColor(this, resource)
    }
}