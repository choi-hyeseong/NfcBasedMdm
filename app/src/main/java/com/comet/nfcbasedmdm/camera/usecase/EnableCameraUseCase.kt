package com.comet.nfcbasedmdm.camera.usecase

import com.comet.nfcbasedmdm.camera.repository.CameraRepository

/**
 * Enable camera usecase
 * @see CameraRepository.enableCamera
 */
class EnableCameraUseCase(private val cameraRepository: CameraRepository) {

    operator fun invoke() {
        cameraRepository.enableCamera()
    }
}