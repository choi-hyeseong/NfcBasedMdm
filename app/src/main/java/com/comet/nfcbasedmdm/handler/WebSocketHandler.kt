package com.comet.nfcbasedmdm.handler

import android.util.Log
import com.comet.nfcbasedmdm.LOG_TAG
import com.comet.nfcbasedmdm.MdmService
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener

class WebSocketHandler(private val service : MdmService) : WebSocketListener() {

    var isOpen : Boolean = false
    set(value){ field = value; Log.i(LOG_TAG, "Server Status : $value") } //프로퍼티 변경시 로그
    private lateinit var webSocket : WebSocket

    override fun onMessage(webSocket: WebSocket, text: String) {
        service.onMessage(text)
    }

    override fun onOpen(webSocket: WebSocket, response: Response) {
        this.webSocket = webSocket
        isOpen = true
    }

    override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
        isOpen = false
    }

    override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
        t.localizedMessage?.let { Log.w(LOG_TAG, it) }
        isOpen = false //서버에서 끊어버림
    }

    fun sendMessage(text: String) : Boolean {
        return if (isOpen)
            webSocket.send(text)
        else
            false
    }

}