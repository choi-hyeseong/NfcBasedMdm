package com.comet.nfcbasedmdm.mdm.auth.repository

import com.comet.nfcbasedmdm.mdm.auth.dto.AuthRequestDTO
import com.comet.nfcbasedmdm.mdm.auth.model.DecryptData
import java.util.UUID

/**
 * 인증에 필요한 레포지토리
 */
interface AuthRepository {

    /**
     * 회원가입을 수행합니다. 이때 암,복호화가 이루어집니다.
     * @param baseUrl 연결할 서버의 base url입니다. - http 포함해야함.
     * @param key RSA 암호화에 필요한 키입니다. 서버로 부터 제공받아야 합니다.
     * @return API 호출 성공시 복호화가 이루어진 결과값입니다. 실패시 failure가 반환됩니다.
     */
    suspend fun register(baseUrl : String, uuid : UUID, key : String) : Result<DecryptData>
}