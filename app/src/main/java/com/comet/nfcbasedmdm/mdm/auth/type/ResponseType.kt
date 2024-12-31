package com.comet.nfcbasedmdm.mdm.auth.type

/**
 * 서버의 요청결과에 따른 enum타입
 * @property OK 성공
 * @property PUBLIC_KEY_ERROR 퍼블릭키를 가져오지 못한경우
 * @property INTERNAL_ERROR 서버 내부 에러
 */
enum class ResponseType {
    OK, PUBLIC_KEY_ERROR, INTERNAL_ERROR
}