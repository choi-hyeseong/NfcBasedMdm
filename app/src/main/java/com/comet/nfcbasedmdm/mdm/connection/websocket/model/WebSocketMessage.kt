package com.comet.nfcbasedmdm.mdm.connection.websocket.model

import com.comet.nfcbasedmdm.mdm.connection.websocket.type.WebSocketStatus

// websocket에서 사용되는 메시지 클래스
data class WebSocketMessage(val status : WebSocketStatus, val data : String)