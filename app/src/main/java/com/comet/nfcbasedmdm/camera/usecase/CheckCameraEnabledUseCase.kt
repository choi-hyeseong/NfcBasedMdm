package com.comet.nfcbasedmdm.camera.usecase

import com.comet.nfcbasedmdm.camera.repository.CameraRepository

/**
 * Check camera isEnabled
 * @see CameraRepository.isCameraEnabled
 */
class CheckCameraEnabledUseCase(private val cameraRepository: CameraRepository){

    operator fun invoke() : Boolean = cameraRepository.isCameraEnabled()
}