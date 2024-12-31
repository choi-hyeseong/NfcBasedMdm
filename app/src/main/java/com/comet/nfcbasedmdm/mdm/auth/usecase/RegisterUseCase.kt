package com.comet.nfcbasedmdm.mdm.auth.usecase

import com.comet.nfcbasedmdm.mdm.auth.model.DecryptData
import com.comet.nfcbasedmdm.mdm.auth.repository.AuthRepository
import java.util.UUID

/**
 * 회원가입 수행하는 유스케이스
 * @see AuthRepository.register
 */
class RegisterUseCase(private val authRepository: AuthRepository) {

    suspend operator fun invoke(url : String, uuid: UUID, key: String): Result<DecryptData> {
        return authRepository.register(url, uuid, key)
    }
}