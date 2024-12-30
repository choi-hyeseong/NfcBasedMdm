package com.comet.nfcbasedmdm.common.cipher

import android.util.Log
import com.comet.nfcbasedmdm.getClassName
import java.nio.charset.StandardCharsets
import java.security.Key
import java.security.KeyFactory
import java.security.PrivateKey
import java.security.PublicKey
import java.security.spec.PKCS8EncodedKeySpec
import java.security.spec.X509EncodedKeySpec
import java.util.Base64
import javax.crypto.Cipher

/**
 * RSA 구현체
 * @see Crypto.javaClass
 */
class RSACrypto : Crypto {

    // key는 다음과 같이 encode 되어야 합니다.
    // Base64.getUrlEncoder().encodeToString(publicKey.encoded)
    override fun encrypt(message: String, key: String): String {
        val keyFactory = KeyFactory.getInstance("RSA")
        val bytePublicKey: ByteArray = Base64.getUrlDecoder().decode(key)
        val publicKeySpec = X509EncodedKeySpec(bytePublicKey)
        val publicKey: PublicKey = keyFactory.generatePublic(publicKeySpec)
        return encrypt(message, publicKey)
    }

    override fun encrypt(message: String, key: Key): String {
        return try {
            val cipher: Cipher = Cipher.getInstance("RSA/NONE/PKCS1Padding")
                .apply { init(Cipher.ENCRYPT_MODE, key) } //패딩 문제로 맞춰줘야됨.

            val encryptByte: ByteArray = cipher.doFinal(message.toByteArray(StandardCharsets.UTF_8))
            Base64.getUrlEncoder().encodeToString(encryptByte)
        }
        catch (e: Exception) {
            Log.w(getClassName(), "Can't encrypt message ${e.message}")
            ""
        }
    }

    // key는 다음과 같이 encode 되어야 합니다.
    // Base64.getUrlEncoder().encodeToString(privateKey.encoded)
    override fun decrypt(encryptedMessage: String, key: String): String {
        val keyFactory = KeyFactory.getInstance("RSA")
        val bytePrivateKey: ByteArray = Base64.getUrlDecoder().decode(key)
        val privateKeySpec = PKCS8EncodedKeySpec(bytePrivateKey)
        val privateKey: PrivateKey = keyFactory.generatePrivate(privateKeySpec)
        return decrypt(encryptedMessage, privateKey)
    }

    override fun decrypt(encryptedMessage: String, key: Key): String {
        return try {
            // RSA Cipher 초기화
            val cipher: Cipher = Cipher.getInstance("RSA/NONE/PKCS1Padding")
                .apply { init(Cipher.DECRYPT_MODE, key) }

            // Base64 디코딩 후 복호화 수행
            val decryptedBytes: ByteArray = cipher.doFinal(
                Base64.getUrlDecoder().decode(encryptedMessage))

            // 복호화된 바이트 배열을 UTF-8 문자열로 변환
            String(decryptedBytes, StandardCharsets.UTF_8)
        }
        catch (e: Exception) {
            Log.w(getClassName(), "Can't decrypt message: ${e.message}")
            ""

        }
    }

}