package com.comet.nfcbasedmdm.mdm.connection.websocket.usecase

import com.comet.nfcbasedmdm.mdm.connection.websocket.callback.WebSocketCallback
import com.comet.nfcbasedmdm.mdm.connection.websocket.repository.WebsocketRepository

/**
 *  connect to websocket
 *  @see WebsocketRepository.connect
 */
class WebSocketConnectUseCase(private val websocketRepository: WebsocketRepository) {

    suspend operator fun invoke(url : String, callback : WebSocketCallback) {
        websocketRepository.connect(url, callback)
    }
}