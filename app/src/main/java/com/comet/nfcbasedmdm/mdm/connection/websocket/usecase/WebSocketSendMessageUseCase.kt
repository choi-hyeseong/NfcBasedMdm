package com.comet.nfcbasedmdm.mdm.connection.websocket.usecase

import com.comet.nfcbasedmdm.mdm.connection.websocket.model.WebSocketMessage
import com.comet.nfcbasedmdm.mdm.connection.websocket.repository.WebsocketRepository

/**
 * send message into websocket
 * @see WebsocketRepository.sendMessage
 */
class WebSocketSendMessageUseCase(private val websocketRepository: WebsocketRepository) {

    suspend operator fun invoke(message: WebSocketMessage) {
        websocketRepository.sendMessage(message)
    }
}