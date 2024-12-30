package com.comet.nfcbasedmdm.common.util

class ApduUtil {

    companion object {
        fun buildApdu(data : ByteArray) : ByteArray {
            val commandApdu = ByteArray(6 + data.size)
            //apdu 구성 00A40400 + 데이터 + LE
            commandApdu[0] = 0x00.toByte() // CLA
            commandApdu[1] = 0xA4.toByte() // INS
            commandApdu[2] = 0x04.toByte() // P1
            commandApdu[3] = 0x00.toByte() // P2
            commandApdu[4] = (data.size and 0x0FF).toByte() // Lc
            System.arraycopy(data, 0, commandApdu, 5, data.size)
            commandApdu[commandApdu.size - 1] = 0x00.toByte() // Le
            return commandApdu
        }
    }
}