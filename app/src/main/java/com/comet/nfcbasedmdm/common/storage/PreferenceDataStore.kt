package com.comet.nfcbasedmdm.common.storage

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking

/**
 * PreferenceDataStore로 구현한 스토리지.
 * 기존 LocalDataStorage를 사용하기 위해 global한 keyStore 사용
 *
 * @property context application Context
 * @property globalKeyMap String을 key로 하는 글로벌한 키맵. 키의 제네릭 정보가 묻히긴 하지만, 적절하게 get할때 복호화 하기
 */
open class PreferenceDataStore(private val context: Context) : LocalStorage {

    //global key map
    private val globalKeyMap: MutableMap<String, Preferences.Key<*>> = mutableMapOf()

    init {
        // 20:54 - globalKeyMap은 메모리상에 존재하므로 앱 종료후 다시 시작되면 초기화됨..
        // class init시 파일에 저장된 키값을 메모리에 로드하기
        runBlocking {
            val keySet = context.dataStore.data.map { it.asMap().keys }
                .firstOrNull() ?: return@runBlocking //키 - 값 리스트 불러오기. 없을경우 return
            val keyPairMap = keySet.map { it.name to it } //name - key 매핑
            globalKeyMap.putAll(keyPairMap) //맵에 집어넣기
        }
    }

    override suspend fun delete(key: String) {
        val preferenceKey = globalKeyMap[key] ?: return
        context.dataStore.edit { preference -> preference.remove(preferenceKey) }
    }

    override suspend fun putInt(key: String, value: Int) {
        putObject(key, value)
    }

    override suspend fun getInt(key: String, defaultValue: Int): Int {
        return getObject(key) ?: defaultValue
    }

    override suspend fun putString(key: String, value: String) {
        putObject(key, value)
    }

    override suspend fun getString(key: String, defaultValue: String): String {
        return getObject(key) ?: defaultValue
    }

    override suspend fun getBoolean(key : String, defaultValue: Boolean) : Boolean {
        return getObject(key) ?: defaultValue
    }

    override suspend fun putBoolean(key : String, value : Boolean) {
        putObject(key, value)
    }

    override suspend fun hasKey(key: String): Boolean {
        return true == context.dataStore.data.map { it.asMap().keys.map { key -> key.name } }
            .firstOrNull()
            ?.contains(key)
    }

    // object 가져올때 map에 저장된 키값 가져옴. 캐스팅 실패거나 값이 없을경우 null 리턴
    // unchecked cast... 제네릭정보가 런타임에는 날아가므로, Cast Exception 발생
    private suspend fun <T> getObject(key: String): T? {
        val preferenceKey: Preferences.Key<*> = globalKeyMap[key] ?: return null
        val result: T? = context.dataStore.data.map { preference ->
            // casting exception 발생한 이유. T가 Int인데 String값이 들어오면 map 함수에서도 Int로 캐스팅 시도 -> 에러.
            // 따라서 Any로 우선 받고, as?를 이용해서 안전한 캐스팅 수행
            // 17:23 - get할때 얘도 uncheckedCasting 수행함.
            val value = preference.get(preferenceKey as Preferences.Key<Any>)
            value as? T// 캐스팅 예외 대비 as? 사용
        }.firstOrNull()

        return result
    }

    // string key값을 value값에 따라 Preference Key로 적절히 변환한 후 global 한 키스토어 및 data store에 저장
    private suspend fun <T> putObject(key: String, value: T) {
        val preferenceKey: Preferences.Key<T> = providePreferenceKey(key, value) as Preferences.Key<T> //type recasting = 하지 않으면 Any로 받아서 수행
        context.dataStore.edit { preference -> preference[preferenceKey] = value } //update
        globalKeyMap[key] = preferenceKey //insert key
    }

    // value값에 따라 적합한 키 제공
    private fun <T> providePreferenceKey(key: String, value: T): Preferences.Key<*> {
        return when (value) {
            is Int -> intPreferencesKey(key)
            is Double -> doublePreferencesKey(key)
            is Float -> floatPreferencesKey(key)
            is Boolean -> booleanPreferencesKey(key)
            is String -> stringPreferencesKey(key)
            is Long -> longPreferencesKey(key)
            is List<*> -> stringSetPreferencesKey(key)
            else -> throw IllegalArgumentException("NOT SUPPORTED") //미지원 value 값
        }
    }
}

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "mdm_storage")