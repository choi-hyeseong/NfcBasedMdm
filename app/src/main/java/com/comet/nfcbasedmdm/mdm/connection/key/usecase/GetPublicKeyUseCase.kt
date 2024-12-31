package com.comet.nfcbasedmdm.mdm.connection.key.usecase

import com.comet.nfcbasedmdm.mdm.connection.key.dto.PublicKeyResponseDTO
import com.comet.nfcbasedmdm.mdm.connection.key.repository.PublicKeyRepository
import com.skydoves.sandwich.ApiResponse

/**
 * Get public key from server
 * @see PublicKeyRepository.getPublicKey
 */
class GetPublicKeyUseCase(private val repository : PublicKeyRepository) {

    suspend operator fun invoke(url : String) : ApiResponse<PublicKeyResponseDTO> {
        return repository.getPublicKey(url)
    }
}