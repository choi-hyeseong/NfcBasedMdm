package com.comet.nfcbasedmdm.mdm.connection.key.repository

import com.comet.nfcbasedmdm.mdm.connection.key.api.PublicKeyAPI
import com.comet.nfcbasedmdm.mdm.connection.key.dto.PublicKeyResponseDTO
import com.skydoves.sandwich.ApiResponse

class RemotePublicKeyRepository(private val publicKeyAPI: PublicKeyAPI) : PublicKeyRepository {

    override suspend fun getPublicKey(baseUrl : String): ApiResponse<PublicKeyResponseDTO> {
        //baseurl을 기준으로 encrypt end point 접근
        return publicKeyAPI.publicKey("http://$baseUrl/encrypt")
    }
}