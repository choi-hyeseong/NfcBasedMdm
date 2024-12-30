package com.comet.nfcbasedmdm.mdm.connection.key.api

import com.comet.nfcbasedmdm.mdm.connection.key.dto.PublicKeyResponseDTO
import com.skydoves.sandwich.ApiResponse
import retrofit2.http.GET

/**
 * RSA 공개키 얻는 API
 */
interface PublicKeyAPI {

    @GET
    suspend fun publicKey() : ApiResponse<PublicKeyResponseDTO>
}