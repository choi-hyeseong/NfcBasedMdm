package com.comet.nfcbasedmdm.mdm.auth.dto

/**
 * 회원가입 결과에 따라 응답되는 DTO
 */
data class AuthResponseDTO(val message : String, val data : Data)

/**
 * @param auth 인증에 필요한 문자열
 * @param delete mdm 삭제에 필요한 문자열
 */
data class Data(val auth : String, val delete : String)