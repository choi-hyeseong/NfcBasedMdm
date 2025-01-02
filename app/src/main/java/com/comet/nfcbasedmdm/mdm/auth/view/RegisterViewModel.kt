package com.comet.nfcbasedmdm.mdm.auth.view

import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.comet.nfcbasedmdm.getClassName
import com.comet.nfcbasedmdm.mdm.auth.type.ResponseType
import com.comet.nfcbasedmdm.mdm.auth.usecase.RegisterUseCase
import com.comet.nfcbasedmdm.mdm.connection.key.usecase.GetPublicKeyUseCase
import com.comet.nfcbasedmdm.mdm.data.model.MDMData
import com.comet.nfcbasedmdm.mdm.data.usecase.SaveMDMDataUseCase
import com.skydoves.sandwich.getOrNull
import com.skydoves.sandwich.getOrThrow
import com.skydoves.sandwich.suspendMapSuccess
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

/**
 * 회원가입에 사용되는 VM
 * @param registerUseCase 회원가입 유스케이스
 * @param getPublicKeyUseCase RSA 퍼블릭키 가져오는 유스케이스
 * @param saveMDMDataUseCase mdm 정보 저장용 유스케이스
 */
@HiltViewModel
class RegisterViewModel @Inject constructor(private val registerUseCase: RegisterUseCase, private val getPublicKeyUseCase: GetPublicKeyUseCase, private val saveMDMDataUseCase: SaveMDMDataUseCase) : ViewModel() {

    // 요청 중복 처리 방지를 위한 LiveData
    val requestProcessingLiveData : MutableLiveData<Boolean> = MutableLiveData()
    // 서버의 요청 결과
    val responseLiveData : MutableLiveData<ResponseType> = MutableLiveData()

    // 회원가입
    fun register(url : String) {
        requestProcessingLiveData.value = true //Main thread
        CoroutineScope(Dispatchers.IO).launch {
            val baseUrl = url.split("//")[1]
            val publicKey = getPublicKeyUseCase(baseUrl).getOrNull()
            // public key를 못가져온경우
            if (publicKey == null) {
                postError(ResponseType.PUBLIC_KEY_ERROR)
                return@launch
            }

            // random uuid
            val uuid = UUID.randomUUID()
            registerUseCase(url, uuid, publicKey.data).onSuccess {
                // http:// 이후 url 저장
                saveMDMDataUseCase(MDMData(uuid, it.auth, it.delete, baseUrl, false))
                postOk()
            }.onFailure {
                postError(ResponseType.INTERNAL_ERROR)
            }
        }
    }

    private fun postOk() {
        responseLiveData.postValue(ResponseType.OK)
        requestProcessingLiveData.postValue(false)
    }

    private fun postError(type : ResponseType) {
        responseLiveData.postValue(type)
        requestProcessingLiveData.postValue(false) // request 초기화
    }
}