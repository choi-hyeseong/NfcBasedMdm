package com.comet.nfcbasedmdm.common.storage

/**
 * 로컬에 저장되는 데이터를 갖고 있는 스토리지
 */
interface LocalStorage {

    suspend fun delete(key: String)

    suspend fun putInt(key: String, value: Int)

    suspend fun getInt(key: String, defaultValue: Int): Int

    suspend fun putString(key: String, value: String)

    suspend fun getString(key: String, defaultValue: String): String

    suspend fun hasKey(key: String): Boolean

    suspend fun getBoolean(key : String, defaultValue: Boolean) : Boolean

    suspend fun putBoolean(key : String, value : Boolean)

}