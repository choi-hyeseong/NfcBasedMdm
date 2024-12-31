package com.comet.nfcbasedmdm.callback

import android.content.BroadcastReceiver
import android.content.IntentFilter
import androidx.fragment.app.Fragment

interface ActivityCallback {

    // 메인 화면으로 이동
    fun switchToMain()

    // 회원가입 화면으로 이동
    fun switchToRegister()

}