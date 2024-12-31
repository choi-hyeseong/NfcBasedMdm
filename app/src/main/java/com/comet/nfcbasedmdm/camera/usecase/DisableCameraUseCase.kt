package com.comet.nfcbasedmdm.camera.usecase

import com.comet.nfcbasedmdm.camera.repository.CameraRepository

/**
 * disable camera usecase
 * @see CameraRepository.disableCamera
 */
class DisableCameraUseCase(private val cameraRepository: CameraRepository) {

    operator fun invoke() {
        cameraRepository.disableCamera()
    }
}