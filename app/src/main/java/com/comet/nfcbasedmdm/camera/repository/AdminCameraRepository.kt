package com.comet.nfcbasedmdm.camera.repository

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context

/**
 * 기기 관리자를 이용해서 카메라 활성화 여부를 변경하는 레포지토리
 * @param context application Context
 */
class AdminCameraRepository(private val context : Context) : CameraRepository {

    // 정책 관리에 필요한 policy manager
    private val policy : DevicePolicyManager by lazy { context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager }
    // admin 권한에 사용되는 broadcast receiver
    private val receiver = ComponentName(context.packageName, "$context.packageName.AdminReceiver")

    override fun isCameraEnabled(): Boolean {
        return !policy.getCameraDisabled(receiver)
    }

    override fun enableCamera() {
        policy.setCameraDisabled(receiver, true)
    }

    override fun disableCamera() {
        policy.setCameraDisabled(receiver, false)
    }

}