package com.comet.nfcbasedmdm.mdm.data.usecase

import com.comet.nfcbasedmdm.mdm.data.model.MDMData
import com.comet.nfcbasedmdm.mdm.data.repository.MDMRepository

/**
 * Save MDM Data
 * @see MDMRepository.saveData
 */
class SaveMDMDataUseCase(private val mdmRepository: MDMRepository) {

    suspend operator fun invoke(mdmData: MDMData) {
        mdmRepository.saveData(mdmData)
    }
}