package com.comet.nfcbasedmdm.camera.repository

/**
 * 카메라 활성화 여부 조절할 수 있는 레포지토리
 */
interface CameraRepository {

    // 카메라의 활성화 여부를 반환합니다.
    fun isCameraEnabled() : Boolean

    // 카메라를 활성화 합니다.
    fun enableCamera()

    // 카메라를 비활성화 합니다.
    fun disableCamera()
}