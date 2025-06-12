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
    // package name 잘 가져오기
    private val receiver = ComponentName(context.packageName, "${context.packageName}.AdminReceiver")

    override fun isCameraEnabled(): Boolean {
        return !policy.getCameraDisabled(receiver)
    }

    override fun enableCamera() {
        // disabled니까 false로 해야 enable 가능
        policy.setCameraDisabled(receiver, false)
    }

    override fun disableCamera() {
        // disabled니까 true로 해야지
        policy.setCameraDisabled(receiver, true)
    }

}