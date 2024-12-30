package com.comet.nfcbasedmdm.common.cipher

import android.util.Log
import com.comet.nfcbasedmdm.getClassName
import java.security.Key
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec


/**
 * AES 구현체
 * @see Crypto.javaClass
 */
class AESCrypto : Crypto {

    companion object {
        private const val IV_LEN = 16
    }

    // TODO CBC 블럭 모드 지원하기
    // AES의 key는 URL Encode 필요없이 문자열이면 됩니다.
    override fun encrypt(message: String, key: String): String {
        // AES는 16,24,32 바이트를 지켜야 하므로 키가 길경우 자름. - Exception 발생 가능성 있음.
        return encrypt(message, SecretKeySpec(key.substring(0, 16).toByteArray(), "AES"))
    }

    override fun encrypt(message: String, key: Key): String {
        return try {
            val iv = ByteArray(IV_LEN).also { SecureRandom().nextBytes(it) }
            val cipher: Cipher = Cipher.getInstance("AES/CBC/PKCS7Padding")
            // Android Keystore의 경우 자체 iv를 갖고 있으므로 에러가 발생하는경우 - 자체 IV로 변경
            kotlin.runCatching { cipher.init(Cipher.ENCRYPT_MODE, key, IvParameterSpec(iv)) }
                .onFailure { cipher.init(Cipher.ENCRYPT_MODE, key) }
            val encrypt = cipher.doFinal(message.toByteArray())
            // base64 encode
            Base64.getUrlEncoder().encodeToString(cipher.iv + encrypt)
        }
        catch (e: Exception) {
            Log.w(getClassName(), "Can't encrypt message : ${e.message}")
            ""
        }
    }

    // AES의 key는 URL Encode 필요없이 문자열이면 됩니다.
    override fun decrypt(encryptedMessage: String, key: String): String {
        return decrypt(encryptedMessage, SecretKeySpec(key.substring(0, 16).toByteArray(), "AES"))
    }

    override fun decrypt(encryptedMessage: String, key: Key): String {
        return try {
            // base64 decode
            val urlDecode = Base64.getUrlDecoder().decode(encryptedMessage.toByteArray())
            val iv = urlDecode.copyOfRange(0, IV_LEN)
            val encrypt = urlDecode.copyOfRange(IV_LEN, urlDecode.size)

            val cipher: Cipher = Cipher.getInstance("AES/CBC/PKCS7Padding")
                .apply { init(Cipher.DECRYPT_MODE, key, IvParameterSpec(iv)) }
            // decrypt
            val decrypt: ByteArray = cipher.doFinal(encrypt)
            String(decrypt)
        }
        catch (e: Exception) {
            Log.w(getClassName(), "Can't decrypt message : ${e.message}")
            ""
        }
    }

}