package com.comet.nfcbasedmdm.common.cipher

import java.security.Key

/**
 * 암,복호화를 수행하는 인터페이스 (RSA, AES 지원)
 */
interface Crypto {

    /**
     * 암호화를 수행합니다.
     * @param message 암호화할 메시지
     * @param key 암호화에 사용될 키
     * @return 암호화된 결과값입니다.
     */
    fun encrypt(message : String, key : String) : String

    /**
     * 암호화를 수행합니다. Key 자체값을 사용합니다.
     * @param key 암호화에 사용될 키
     */
    fun encrypt(message : String, key : Key) : String

    /**
     * 복호화를 수행합니다.
     * @param encryptedMessage 암호화된 문자열
     * @param key 복호화에 사용될 키
     * @return 복호화된 결과입니다.
     */
    fun decrypt(encryptedMessage: String, key : String) : String


    /**
     * 복호화를 수행합니다. Key 자체값을 사용합니다.
     * @param key 복호화에 사용될 키
     */
    fun decrypt(encryptedMessage : String, key : Key) : String
}