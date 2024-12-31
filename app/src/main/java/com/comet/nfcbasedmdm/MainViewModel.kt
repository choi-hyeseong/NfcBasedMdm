package com.comet.nfcbasedmdm

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.comet.nfcbasedmdm.mdm.data.usecase.GetMDMDataUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(private val getMDMDataUseCase: GetMDMDataUseCase) : ViewModel() {

    // 단순 함수에서 호출하는 방식으로 구현
    // val property로 선언해서 lazy하게 로드시켜도 됨
    fun existMDMData() : LiveData<Boolean> {
        val liveData : MutableLiveData<Boolean> = MutableLiveData()
        CoroutineScope(Dispatchers.IO).launch {
            val result = getMDMDataUseCase()
            if (result == null)
                liveData.postValue(false)
            else
                liveData.postValue(true)
        }
        return liveData
    }
}