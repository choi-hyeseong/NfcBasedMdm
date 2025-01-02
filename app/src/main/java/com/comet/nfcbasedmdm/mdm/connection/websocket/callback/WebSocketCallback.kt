package com.comet.nfcbasedmdm.mdm.connection.websocket.callback

import com.comet.nfcbasedmdm.mdm.connection.websocket.model.WebSocketMessage

/**
 * 웹소켓에서 메시지 전달시 호출되는 콜백
 */
interface WebSocketCallback {

    fun onOpen()

    fun onMessage(message : WebSocketMessage)

    fun onClose()
}