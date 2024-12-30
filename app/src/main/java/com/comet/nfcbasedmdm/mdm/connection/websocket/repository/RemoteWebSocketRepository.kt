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
import okio.ByteString
import java.nio.charset.Charset

class RemoteWebSocketRepository(private val okHttpClient: OkHttpClient) : WebsocketRepository {

    private var webSocket : WebSocket? = null
    private val gson = Gson()

    override suspend fun connect(url : String, callback: WebSocketCallback) {
        val request = Request.Builder().url("ws://$url/mdm").build()
        webSocket = okHttpClient.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                callback.onOpen()
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                callback.onClose(reason)
                this@RemoteWebSocketRepository.webSocket = null
            }

            override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
                kotlin.runCatching {
                   gson.fromJson(bytes.string(Charset.defaultCharset()), WebSocketMessage::class.java)
                }.onSuccess {
                    callback.onMessage(it)
                }.onFailure {
                    Log.e(getClassName(), "Can't Deserialize WebSocket Message ${it.message}")
                }
            }
        })
    }

    override suspend fun disconnect() {
        webSocket = null
    }

    override suspend fun sendMessage(message: WebSocketMessage) {
        if (!isConnected())
            return
        webSocket?.send(gson.toJson(message))
    }

    override suspend fun isConnected(): Boolean {
        return webSocket != null
    }

}