package com.comet.nfcbasedmdm.mdm.data.model

import java.util.UUID


/**
 * 회원가입시 존재하는 uuid, delete, auth 데이터 저장소.
 * @param uuid 사용자의 고유한 id입니다.
 * @param authID uuid와 함께 사용자 인증에 사용되는 id입니다.
 * @param deleteID 삭제에 사용될 id입니다.
 * @param ip 서버의 ip 정보입니다.
 * @param isEnabled mdm이 활성화 되어있는지 확인할 수 있는 정보입니다.
 */
data class MDMData(val uuid : UUID, val authID : String, val deleteID : String, val ip: String, var isEnabled : Boolean)
