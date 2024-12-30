package com.comet.nfcbasedmdm.mdm.data.repository

import com.comet.nfcbasedmdm.common.storage.CryptoDataStore
import com.comet.nfcbasedmdm.common.util.toUUID
import com.comet.nfcbasedmdm.mdm.data.model.MDMData

/**
 * Crypto DataStore를 이용한 안전한 데이터 저장
 */
class EncryptMDMRepository(private val cryptoDataStore: CryptoDataStore) : MDMRepository {

    companion object {
        private const val UUID_KEY = "MDM_UUID"
        private const val IP_KEY = "MDM_IP"
        private const val AUTH_KEY = "MDM_AUTH"
        private const val DELETE_KEY = "MDM_DELETE"
        private const val ENABLED_KEY = "MDM_ENABLED"
    }

    override suspend fun getData(): MDMData? {
        val uuid = cryptoDataStore.getString(UUID_KEY, "").toUUID()
        val auth = cryptoDataStore.getString(AUTH_KEY, "")
        val delete = cryptoDataStore.getString(DELETE_KEY, "")
        val ip = cryptoDataStore.getString(IP_KEY, "")
        val enabled = cryptoDataStore.getBoolean(ENABLED_KEY, false)
        return if (uuid == null || auth.isEmpty() || delete.isEmpty() || ip.isEmpty()) null
        else MDMData(uuid = uuid, authID = auth, deleteID = delete, ip = ip, isEnabled = enabled)
    }

    override suspend fun saveData(data: MDMData) {
        cryptoDataStore.run {
            putString(UUID_KEY, data.uuid.toString())
            putString(AUTH_KEY, data.authID)
            putString(DELETE_KEY, data.deleteID)
            putString(IP_KEY, data.ip)
            putBoolean(ENABLED_KEY, data.isEnabled)
        }
    }

}