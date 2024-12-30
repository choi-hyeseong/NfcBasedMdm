package com.comet.nfcbasedmdm.common.key

import javax.crypto.SecretKey

/**
 * AES Key를 제공하는 인터페이스
 */
interface KeyProvider {

    /**
     * AES에 사용되는 비밀키 반환
     */
    fun getKey() : SecretKey
}