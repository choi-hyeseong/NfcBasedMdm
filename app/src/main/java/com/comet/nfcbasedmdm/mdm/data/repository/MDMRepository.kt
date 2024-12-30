package com.comet.nfcbasedmdm.mdm.data.repository

import com.comet.nfcbasedmdm.mdm.data.model.MDMData

/**
 * MDM 정보를 저장할 레포지토리
 */
interface MDMRepository {

    // 정보 가져오기 - 없을경우 null
    suspend fun getData() : MDMData?

    // 정보 저장하기
    suspend fun saveData(data : MDMData)
}