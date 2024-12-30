package com.comet.nfcbasedmdm.mdm.connection.websocket.callback

import com.comet.nfcbasedmdm.mdm.connection.websocket.model.WebSocketMessage

/**
 * 웹소켓에서 메시지 전달시 호출되는 콜백
 */
interface WebSocketCallback {

    fun onOpen()

    // 서버에 의해서 종료될때만 호출됨. 사용자가 disconnect 메소드로 호출한경우 작동 X
    fun onClose(reason : String)

    fun onMessage(message : WebSocketMessage)
}