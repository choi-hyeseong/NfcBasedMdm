package com.comet.nfcbasedmdm.mdm.connection.key.repository

import com.comet.nfcbasedmdm.mdm.connection.key.dto.PublicKeyResponseDTO
import com.skydoves.sandwich.ApiResponse

// 공개키 얻는 레포지토리
interface PublicKeyRepository {

    /**
     * @param baseUrl 연결에 사용될 baseUrl입니다. http:// 미포함
     */
    suspend fun getPublicKey(baseUrl : String) : ApiResponse<PublicKeyResponseDTO>
}