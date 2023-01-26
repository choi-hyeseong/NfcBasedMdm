package com.comet.nfcbasedmdm.model

class WebSocketMessage() {
    lateinit var status : Status
    lateinit var data : String

    constructor(status : Status, data : String) : this() {
        this.status = status
        this.data = data
    }

    enum class Status {
        HAND_SHAKE, PING, PONG, EXECUTE_MDM, RESPONSE
    }
}