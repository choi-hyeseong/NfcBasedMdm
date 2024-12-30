package com.comet.nfcbasedmdm.mdm.auth.api

import com.comet.nfcbasedmdm.mdm.auth.dto.AuthRequestDTO
import com.comet.nfcbasedmdm.mdm.auth.dto.AuthResponseDTO
import com.skydoves.sandwich.ApiResponse
import retrofit2.http.POST

// 인증에 필요한 API
// OpenFeign랑 다르게 같은 endpoint라도 분리 가능
interface AuthAPI {

    @POST
    suspend fun register(request : AuthRequestDTO) : ApiResponse<AuthResponseDTO>

}