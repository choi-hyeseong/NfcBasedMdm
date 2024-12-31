package com.comet.nfcbasedmdm.common.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * BroadcastReceiver에서 수행되는 intent 체크로직등을 간략화한 추상리시버
 */
abstract class AbstractBroadcastReceiver : BroadcastReceiver() {

    /**
     * 해당 리시버가 액션을 지원하는지 리턴하는 함수
     * @param action 인텐트 액션입니다.
     * @return 해당 액션을 지원하는지 여부입니다.
     */
    abstract fun isSupport(action : String) : Boolean

    /**
     * 위 isSupport를 통과한 Action만 핸들링하는 함수입니다. onReceive를 구현할 필요가 없습니다.
     */
    abstract fun handleIntent(context: Context, intent: Intent)

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        // action이 없는경우 리턴
        if (action.isNullOrEmpty())
            return
        // 지원하지 않는경우 리턴
        if (!isSupport(action))
            return
        //핸들
        handleIntent(context, intent)
    }


}