package com.comet.nfcbasedmdm

import android.app.ActivityManager
import android.app.AppOpsManager
import android.app.admin.DevicePolicyManager
import android.content.*
import android.nfc.NfcAdapter
import android.nfc.cardemulation.CardEmulation
import android.os.*
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.comet.nfcbasedmdm.callback.ActivityCallback
import com.comet.nfcbasedmdm.databinding.ActivityMainBinding
import com.comet.nfcbasedmdm.mdm.auth.view.RegisterFragment
import com.comet.nfcbasedmdm.mdm.view.MainFragment
import com.comet.nfcbasedmdm.nfc.HostService
import com.comet.nfcbasedmdm.service.MdmService
import dagger.hilt.android.AndroidEntryPoint


@AndroidEntryPoint
class MainActivity : AppCompatActivity(), ActivityCallback {

    private val viewModel: MainViewModel by viewModels()

    // 서비스 작동여부 확인용
    private val activityManager: ActivityManager by lazy { getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager }

    // 기기 관리자 권한 허용
    private val deviceAdminLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        //result == 활성화 이후 작동 + 서비스 연결이후 액티비티 결과 반환
        if (isAdminActivated())
            checkPermissions() // 나머지 펄미션 확인
        else {
            Toast.makeText(this, R.string.device_admin_required, Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    // android 10 이상 권한 사용시간 권한 체크
    private val overlayLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        //사용시간 권한 활성화 체크
        if (isUsageEnabled())
            init()
        else {
            Toast.makeText(this, R.string.usage_required, Toast.LENGTH_SHORT).show()
        }
    }

    private val policyManager: DevicePolicyManager by lazy { getSystemService(DEVICE_POLICY_SERVICE) as DevicePolicyManager }
    private val receiver: ComponentName by lazy { ComponentName(packageName, "$packageName.AdminReceiver") }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val view: ActivityMainBinding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(view.root)
        supportActionBar?.hide() //액션바 숨기기
        checkPermissions()
    }

    private fun checkPermissions() {
        if (!isAdminActivated()) {
            deviceAdminLauncher.launch(Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, receiver))
            return
        }
        if (!isUsageEnabled()) {
            overlayLauncher.launch(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
            return
        }
        init()

    }

    private fun init() {
        if (NfcAdapter.getDefaultAdapter(this) != null)
            setupNFCInstance()
        viewModel.existMDMData().observe(this) { isExist ->
            if (isExist)
                switchToMain()
            else
                switchToRegister()
        }
    }

    private fun isAdminActivated(): Boolean {
        return policyManager.isAdminActive(receiver)
    }


    private fun isUsageEnabled(): Boolean {
        val appOps = getSystemService(APP_OPS_SERVICE) as AppOpsManager
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) AppOpsManager.MODE_ALLOWED == appOps.unsafeCheckOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), packageName)
        else return true //Q 미만일경우 굳이 할필요 없음.

    }

    private fun setupNFCInstance() {
        val component = ComponentName(this, HostService::class.java)
        val adapter = NfcAdapter.getDefaultAdapter(this)
        val dynamic = arrayListOf("F239856324897348")
        CardEmulation.getInstance(adapter).also { it.registerAidsForService(component, "other", dynamic) }
    }

    private fun isServiceRunning(serviceClass: Class<*>) : Boolean {
        return activityManager.getRunningServices(Int.MAX_VALUE)
            .find { it.service.className == serviceClass.name } != null
    }

    override fun switchToMain() {
        switch(MainFragment(), false)
        // 메인화면 전환시에 서비스 실행. << mdm 초기화전까지는 작동되선 안됨
        if (!isServiceRunning(MdmService::class.java))
            startForegroundService(Intent(this, MdmService::class.java))
    }

    override fun switchToRegister() {
        switch(RegisterFragment(), false)
    }

    // fragment 전환
    private fun switch(frag: Fragment, replace: Boolean) {
        if (replace) supportFragmentManager.beginTransaction()
            .addToBackStack(null)
            .replace(R.id.frame, frag)
            .commit()
        else supportFragmentManager.beginTransaction().replace(R.id.frame, frag).commit()
    }


}

// for logging
fun Any.getClassName(): String = this.javaClass.simpleName