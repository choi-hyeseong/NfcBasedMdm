package com.comet.nfcbasedmdm.mdm.connection.websocket.repository

import android.util.Log
import com.comet.nfcbasedmdm.getClassName
import com.comet.nfcbasedmdm.mdm.connection.websocket.callback.WebSocketCallback
import com.comet.nfcbasedmdm.mdm.connection.websocket.model.WebSocketMessage
import com.google.gson.Gson
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener

class RemoteWebSocketRepository(private val okHttpClient: OkHttpClient) : WebsocketRepository {

    private var webSocket: WebSocket? = null
    private val gson = Gson()

    override suspend fun connect(baseUrl: String, callback: WebSocketCallback) {
        val request = Request.Builder().url("ws://$baseUrl/mdm").build()
        webSocket = okHttpClient.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                Log.i(getClassName(), "Websocket open")
                callback.onOpen()
            }

            // 서버에서 close를 해도 onFailure에서 핸들링됨.
            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                super.onFailure(webSocket, t, response)
                Log.e(this@RemoteWebSocketRepository.getClassName(), "onFailure")
                callback.onClose() // 따라서 callback의 onClose를 여기서 핸들링
                this@RemoteWebSocketRepository.webSocket = null
            }


            // 웨 string이면서 byteString이라....
            override fun onMessage(webSocket: WebSocket, text: String) {
                kotlin.runCatching {
                    gson.fromJson(text, WebSocketMessage::class.java)
                }.onSuccess {
                    callback.onMessage(it)
                }.onFailure {
                    Log.e(getClassName(), "Can't Deserialize WebSocket Message ${it.message}")
                }
            }
        })
    }

    override suspend fun disconnect() {
        webSocket?.cancel()
        webSocket = null
    }

    override suspend fun sendMessage(message: WebSocketMessage) {
        if (!isConnected()) return
        webSocket?.send(gson.toJson(message))
    }

    override suspend fun isConnected(): Boolean {
        return webSocket != null
    }

}