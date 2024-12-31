package com.comet.nfcbasedmdm.mdm.connection.websocket.usecase

import com.comet.nfcbasedmdm.mdm.connection.websocket.repository.WebsocketRepository

/**
 * disconnect from websocket
 * @see WebsocketRepository.disconnect
 */
class WebSocketDisconnectUseCase(private val websocketRepository: WebsocketRepository) {

    suspend operator fun invoke() {
        websocketRepository.disconnect()
    }
}