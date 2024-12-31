package com.comet.nfcbasedmdm.mdm.connection.websocket.usecase

import com.comet.nfcbasedmdm.mdm.connection.websocket.repository.WebsocketRepository

/**
 * returns is websocket connected
 * @see WebsocketRepository.isConnected
 */
class WebSocketCheckConnectionUseCase(private val websocketRepository: WebsocketRepository) {

    suspend operator fun invoke() : Boolean = websocketRepository.isConnected()
}