package com.comet.nfcbasedmdm.mdm.connection.key.repository

import com.comet.nfcbasedmdm.mdm.connection.key.api.PublicKeyAPI
import com.comet.nfcbasedmdm.mdm.connection.key.dto.PublicKeyResponseDTO
import com.skydoves.sandwich.ApiResponse

class RemotePublicKeyRepository(private val publicKeyAPI: PublicKeyAPI) : PublicKeyRepository {

    override suspend fun getPublicKey(): ApiResponse<PublicKeyResponseDTO> {
        return publicKeyAPI.publicKey()
    }
}