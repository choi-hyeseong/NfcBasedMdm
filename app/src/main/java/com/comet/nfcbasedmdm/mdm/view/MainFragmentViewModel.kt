package com.comet.nfcbasedmdm.mdm.view

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.comet.nfcbasedmdm.mdm.data.usecase.GetMDMDataUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * 메인 MDM화면의 정보를 담을 viewModel
 * @param getMDMDataUseCase 로컬에서 가져올 수 있는 mdm 정보는 최초 로드시 알려줌
 */
@HiltViewModel
class MainFragmentViewModel @Inject constructor(private val getMDMDataUseCase: GetMDMDataUseCase) : ViewModel() {

    // 서버의 정보를 담는 LiveData
    val serverStatusLiveData : MutableLiveData<Boolean> = MutableLiveData()
    // mdm 상태를 담는 LiveData
    val mdmStatusLiveData : MutableLiveData<Boolean> by lazy {
        // lazy하게 가져오면서 로드시 mdm 정보 로드까지.
        MutableLiveData<Boolean>().apply { value = loadInitialMdmStatus() }
    }

    fun updateServerStatus(status : Boolean) {
        serverStatusLiveData.value = status
    }

    fun updateMDMStatus(status: Boolean) {
        mdmStatusLiveData.value = status
    }

    private fun loadInitialMdmStatus() : Boolean {
        return runBlocking {
            withContext(Dispatchers.IO) {
                true == getMDMDataUseCase()?.isEnabled
            }
        }
    }
}