package com.comet.nfcbasedmdm.mdm.connection.websocket.repository

import com.comet.nfcbasedmdm.mdm.connection.websocket.callback.WebSocketCallback
import com.comet.nfcbasedmdm.mdm.connection.websocket.model.WebSocketMessage

/**
 * 웹소켓 연결을 구성하는 레포지토리
 */
interface WebsocketRepository {

    // 연결시 웹소켓 메시지 핸들링할 콜백과 함께 전달
    suspend fun connect(url : String, callback : WebSocketCallback)

    suspend fun disconnect()

    suspend fun sendMessage(message : WebSocketMessage)

    suspend fun isConnected() : Boolean
}