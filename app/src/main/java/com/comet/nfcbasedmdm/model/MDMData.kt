package com.comet.nfcbasedmdm.model

import java.util.UUID

//회원가입시 존재하는 uuid, delete, auth 데이터 저장소.
data class MDMData(val uuid : UUID, val delete : String, val auth : String, val ip: String)
