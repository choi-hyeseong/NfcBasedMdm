package com.comet.nfcbasedmdm.mdm.auth.api

import com.comet.nfcbasedmdm.mdm.auth.dto.AuthRequestDTO
import com.comet.nfcbasedmdm.mdm.auth.dto.AuthResponseDTO
import com.skydoves.sandwich.ApiResponse
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Url

// 인증에 필요한 API
// OpenFeign랑 다르게 같은 endpoint라도 분리 가능
interface AuthAPI {

    @POST
    suspend fun register(@Url url : String, @Body request : AuthRequestDTO) : ApiResponse<AuthResponseDTO>

}