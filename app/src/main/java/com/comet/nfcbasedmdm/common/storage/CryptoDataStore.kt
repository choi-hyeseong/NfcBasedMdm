package com.comet.nfcbasedmdm.common.storage

import com.comet.nfcbasedmdm.common.cipher.AESCrypto
import javax.crypto.SecretKey

/**
 * 기존 DataStore에 저장되는 데이터를 암-복호화후 관리하는 저장소
 * 기존 PreferenceDataStore를 위임을 이용해 처리했으나, 데이터 충돌의 문제가 있음 (암호화한 데이터 저장한곳에 일반 DataStore접근시 암호화된 데이터만 볼수 있음 < 당연한거긴 함..)
 * @param preferenceDataStore 데이터가 저장될 Storage - 내부 구조 변경이 있기 때문에 상속보단 aggregation이 맞을듯 (getInt를 getString을 이용해서 사용둥)
 * @param secretKey 암호화에 사용될 키
 * @param aesCrypto aes 암호화에 사용될 키
 */
class CryptoDataStore(private val preferenceDataStore: PreferenceDataStore, private val secretKey: SecretKey, private val aesCrypto: AESCrypto) : LocalStorage {

    override suspend fun getString(key: String, defaultValue: String): String {
        val encryptString = preferenceDataStore.getString(key, defaultValue)
        val result = aesCrypto.decrypt(encryptString, secretKey)
        return result.ifEmpty { defaultValue }

    }

    override suspend fun hasKey(key: String): Boolean {
        return preferenceDataStore.hasKey(key)
    }

    override suspend fun getBoolean(key: String, defaultValue: Boolean): Boolean {
        return kotlin.runCatching {
            getString(key, defaultValue.toString()).toBoolean()
        }.getOrElse { defaultValue }
    }

    override suspend fun putBoolean(key: String, value: Boolean) {
        putString(key, value.toString())
    }

    override suspend fun putString(key: String, value: String) {
        val encryptValue = aesCrypto.encrypt(value, secretKey)
        preferenceDataStore.putString(key, encryptValue)
    }

    override suspend fun getInt(key: String, defaultValue: Int): Int {
        return kotlin.runCatching {
            getString(key, defaultValue.toString()).toInt()
        }.getOrElse { defaultValue }
    }

    override suspend fun delete(key: String) {
        preferenceDataStore.delete(key)
    }

    override suspend fun putInt(key: String, value: Int) {
        putString(key, value.toString())
    }
}