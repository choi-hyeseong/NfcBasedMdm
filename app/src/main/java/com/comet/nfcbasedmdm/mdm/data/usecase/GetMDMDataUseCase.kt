package com.comet.nfcbasedmdm.mdm.data.usecase

import com.comet.nfcbasedmdm.mdm.data.model.MDMData
import com.comet.nfcbasedmdm.mdm.data.repository.MDMRepository

/**
 * GET MDM DATA
 * @see MDMRepository.getData
 */
class GetMDMDataUseCase(private val mdmRepository: MDMRepository) {

    suspend operator fun invoke() : MDMData? {
        return mdmRepository.getData()
    }
}