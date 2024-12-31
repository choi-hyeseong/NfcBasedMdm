package com.comet.nfcbasedmdm.mdm.connection.key.repository

import com.comet.nfcbasedmdm.mdm.connection.key.dto.PublicKeyResponseDTO
import com.skydoves.sandwich.ApiResponse

// 공개키 얻는 레포지토리
interface PublicKeyRepository {

    suspend fun getPublicKey(baseUrl : String) : ApiResponse<PublicKeyResponseDTO>
}