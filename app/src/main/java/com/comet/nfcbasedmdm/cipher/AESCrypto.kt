package com.comet.nfcbasedmdm.cipher

import android.R.id.input
import android.annotation.SuppressLint
import android.util.Log
import com.comet.nfcbasedmdm.getClassName
import java.security.Key
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec


/**
 * AES 구현체
 * @see Crypto.javaClass
 */
class AESCrypto : Crypto {


    override fun encrypt(encryptedMessage: String, key: String): String {
        return try {
            // AES는 16,24,32 바이트를 지켜야 하므로 키가 길경우 자름. - Exception 발생 가능성 있음.
            val secretKeySpec: Key = SecretKeySpec(key.substring(0, 16).toByteArray(), "AES")
            val cipher: Cipher = Cipher.getInstance("AES").apply { init(Cipher.ENCRYPT_MODE, secretKeySpec) }
            val encrypt = cipher.doFinal(encryptedMessage.toByteArray())
            // base64 encode
            Base64.getUrlEncoder().encodeToString(encrypt)
        }
        catch (e: Exception) {
            Log.w(getClassName(), "Can't encrypt message : ${e.message}")
            ""
        }
    }

    @SuppressLint("GetInstance")
    override fun decrypt(encryptedMessage: String, key: String): String {
        return try {
            val secretKeySpec: Key = SecretKeySpec(key.substring(0, 16).toByteArray(), "AES")
            val cipher: Cipher = Cipher.getInstance("AES").apply { init(Cipher.DECRYPT_MODE, secretKeySpec) }
            // base64 decode
            val urlDecode = Base64.getUrlDecoder().decode(encryptedMessage.toByteArray())
            // decrypt
            val decrypt: ByteArray = cipher.doFinal(urlDecode)
            String(decrypt)
        }
        catch (e: Exception) {
            Log.w(getClassName(), "Can't decrypt message : ${e.message}")
            ""
        }
    }

}