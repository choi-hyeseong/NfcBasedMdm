package com.comet.nfcbasedmdm.service.model

/**
 * MDM 요청을 주고받을때 생성될 모델
 * @property authID 요청된 auth ID입니다.
 * @property time 서버에서 요청한 시간입니다.
 * @property isMDMRequested mdm request (카메라 비허용 요청) 부분입니다.
 */
data class MDMMessage constructor(val authID : String, val time : Long, val isMDMRequested : Boolean)