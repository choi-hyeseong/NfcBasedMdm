package com.comet.nfcbasedmdm.common.util

import android.annotation.SuppressLint
import java.nio.charset.StandardCharsets
import java.security.Key
import java.security.KeyFactory
import java.security.PublicKey
import java.security.spec.X509EncodedKeySpec
import java.util.*
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec

class EncryptUtil {

    companion object {
        fun RSAEncrypt(input: String, key: String): String {
            return try {
                val keyFactory = KeyFactory.getInstance("RSA")
                val bytePublicKey: ByteArray = Base64.getUrlDecoder().decode(key)
                val publicKeySpec = X509EncodedKeySpec(bytePublicKey)
                val publicKey: PublicKey = keyFactory.generatePublic(publicKeySpec)
                val cipher: Cipher = Cipher.getInstance("RSA/NONE/PKCS1Padding")
                    .apply { init(Cipher.ENCRYPT_MODE, publicKey) } //패딩 문제로 맞춰줘야됨.
                val encryptByte: ByteArray = cipher.doFinal(input.toByteArray(StandardCharsets.UTF_8))
                Base64.getUrlEncoder()
                    .encodeToString(encryptByte)
            } catch (e: Exception) {
                e.printStackTrace()
                ""
            }
        }

        //패딩이나 암호화 코드 없다고 Lint가 알려주는데, 추가하면 서버랑 통신시 오류 생겼던걸로..
        @SuppressLint("GetInstance")
        fun AESDecrypt(input: String, key: String): String {
            val secretKeySpec: Key = SecretKeySpec(key.substring(0, 16).toByteArray(), "AES")
            return try {
                val cipher: Cipher =
                    Cipher.getInstance("AES").apply { init(Cipher.DECRYPT_MODE, secretKeySpec) }
                val urlDecode = Base64.getUrlDecoder().decode(input.toByteArray())
                val decrypt: ByteArray = cipher.doFinal(urlDecode)
                String(decrypt)
            } catch (e: Exception) {
                ""
            }
        }
    }
}