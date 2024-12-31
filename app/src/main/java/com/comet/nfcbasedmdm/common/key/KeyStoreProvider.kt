package com.comet.nfcbasedmdm.common.key

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Log
import com.comet.nfcbasedmdm.getClassName
import java.security.KeyStore
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey

/**
 * Android KeyStore를 이용해서 비밀키 반환
 * KeyStore에서 생성된 키는 encode로 만들어서 String화 불가함.
 */
class KeyStoreProvider : KeyProvider {

    companion object {
        private const val KEYSTORE_NAME = "AndroidKeyStore"
        private const val KEY_ALIAS = "MDM_KEY"
    }

    override fun getKey(): SecretKey {
        val keyStore: KeyStore = KeyStore.getInstance(KEYSTORE_NAME).apply { load(null) }
        return if (keyStore.containsAlias(KEY_ALIAS)) keyStore.getKey(KEY_ALIAS, null) as SecretKey
        else generateAESKey()
    }

    // 내부적으로 사용하는 키이므로 서버와 패딩 맞출 필요 없음
    private fun generateAESKey(): SecretKey {
        // 1. KeyGenerator 초기화
        val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KEYSTORE_NAME)

        // 2. 키 생성 매개변수 설정
        val keyGenParameterSpec = KeyGenParameterSpec.Builder(
            KEY_ALIAS,  // 키 별칭
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
            .setBlockModes(KeyProperties.BLOCK_MODE_CBC)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_PKCS7)  
            .build()

        keyGenerator.init(keyGenParameterSpec)

        // 3. 키 생성 및 반환 (KeyStore에 자동 저장)
        return keyGenerator.generateKey()
    }
}
