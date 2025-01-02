package com.comet.nfcbasedmdm.mdm.auth.repository

import com.comet.nfcbasedmdm.common.cipher.AESCrypto
import com.comet.nfcbasedmdm.common.cipher.RSACrypto
import com.comet.nfcbasedmdm.mdm.auth.api.AuthAPI
import com.comet.nfcbasedmdm.mdm.auth.dto.AuthRequestDTO
import com.comet.nfcbasedmdm.mdm.auth.model.DecryptData
import com.skydoves.sandwich.getOrThrow
import java.util.UUID

class CryptAuthRepository(private val authAPI: AuthAPI, private val rsaCrypto: RSACrypto, private val aesCrypto: AESCrypto): AuthRepository {

    override suspend fun register(baseUrl : String, uuid: UUID, key : String): Result<DecryptData> {
        val encryptUUID : String = rsaCrypto.encrypt(uuid.toString(), key)
        return kotlin.runCatching {
            val response = authAPI.register(baseUrl.plus("/auth"), AuthRequestDTO(encryptUUID)).getOrThrow()
            val decryptAuth = aesCrypto.decrypt(response.data.auth, uuid.toString())
            val decryptDelete = aesCrypto.decrypt(response.data.delete, uuid.toString())
            if (decryptAuth.isEmpty() || decryptAuth.isEmpty())
                throw IllegalStateException("복호화에 실패했습니다. AUTH : $decryptAuth, DELETE : $decryptDelete")
            DecryptData(decryptAuth, decryptDelete)
        }
    }
}