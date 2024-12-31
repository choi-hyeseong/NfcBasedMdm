package com.comet.nfcbasedmdm.camera.repository

import android.app.Service
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.util.Log
import com.comet.nfcbasedmdm.MainActivity
import com.comet.nfcbasedmdm.getClassName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Android 10이상부터는 기기관리자 사용이 어렵기 때문에, Top Application의 권한 분석해서 종료시키는 레포지토리
 */
class ThreadCameraRepository(private val context: Context) : CameraRepository {

    companion object {
        private const val DELAY = 500L // 0.5s
        private const val USAGE_BEGIN = 1000 * 10 //10s
        private val DENY_PERMISSION = listOf("android.permission.CAMERA", "android.permission.RECORD_AUDIO") //카메라나 녹음 권한
    }

    private var isRunning: Boolean = false
    private var job: Job? = null // 코루틴은 풀링이 안좋다곤 하는데.. thread랑 job중 고민됨..
    private val packageManager: PackageManager by lazy { context.packageManager } // 패키지 정보 가져오기
    private val mUsageStatsManager: UsageStatsManager by lazy { context.getSystemService(Service.USAGE_STATS_SERVICE) as UsageStatsManager } //최상단 앱 가져오기 위한 스탯

    override fun isCameraEnabled(): Boolean {
        return isRunning && job != null
    }

    override fun enableCamera() {
        isRunning = false
        job?.cancel()
        job = null
    }

    override fun disableCamera() {
        if (isRunning) return
        isRunning = true
        job = CoroutineScope(Dispatchers.IO).launch {
            while (true) {
                delay(DELAY)
                val topApplication = getTopApplicationPackage() ?: continue
                val permission = getPermissions(topApplication)
                if (permission.contains(DENY_PERMISSION[0]) || permission.contains(DENY_PERMISSION[1])) {
                    Log.w(getClassName(), "founded deny application.")
                    startMDMActivity() //MDM 화면으로 이동하기. 토스트 전달하기 위해 getString 가져오는것보단 MDM 화면 보여주는게 적합할듯
                }
            }
        }
    }

    private suspend fun startMDMActivity() {
        withContext(Dispatchers.Main) {
            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        }
    }

    private fun getPermissions(pack: String): List<String> {
        return kotlin.runCatching {
            val info = packageManager.getPackageInfo(pack, PackageManager.GET_PERMISSIONS)
            if (info.requestedPermissions != null) info.requestedPermissions.toList()
            else listOf()
        }.getOrElse { listOf() }
    }

    private fun getTopApplicationPackage(): String? {
        val time = System.currentTimeMillis()
        // 10초 간격으로 가져오기
        val stats = mUsageStatsManager.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, time - USAGE_BEGIN, time) ?: return ""
        // 사용량 가장 최신순으로
        stats.sortByDescending { it.lastTimeUsed }
        return if (stats.isEmpty()) null
        else stats[0].packageName
    }


}