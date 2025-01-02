package com.comet.nfcbasedmdm.service.serialize

import android.util.Log
import com.comet.nfcbasedmdm.common.cipher.AESCrypto
import com.comet.nfcbasedmdm.common.cipher.RSACrypto
import com.comet.nfcbasedmdm.getClassName
import com.comet.nfcbasedmdm.mdm.data.model.MDMData
import com.comet.nfcbasedmdm.service.model.MDMMessage

/**
 * MDMMessage를 직렬화, 역직렬화 하는 클래스
 * 만약 더 분리한다면 validator로 나눌수도 있을듯
 */
class MessageSerializer(private val aesCrypto: AESCrypto, private val rsaCrypto: RSACrypto) {

    companion object {
        private const val TIMEOUT = 2L // websocket message timeout
    }

    /**
     * 해당 websocket 문자열을 역직렬화 합니다. 만약 책임 분리가 필요하다면 deserializer 구현해도 될듯.
     * @param message 수신된 메시지입니다.
     * @param mdmData 검증과정에서 필요한 유저 정보입니다.
     * @return 검증 성공시 올바른 값, 실패시 null을 리턴합니다.
     */
    fun deserialize(message: String, mdmData: MDMData): MDMMessage? {
        // 올바른 요청인지 확인
        val decrypt = aesCrypto.decrypt(message, mdmData.uuid.toString())
        if (decrypt.isEmpty()) {
            Log.w(getClassName(), "can't execute mdm")
            return null
        }

        val split = message.split("|")
        if (split.size != 3) {
            Log.w(getClassName(), "invalid mdm data")
            return null
        }
        val authId = split[0]
        val time = split[1].toLong()
        val cameraDenied = split[2].toBoolean()

        if (authId != mdmData.authID && !checkTimeValid(time)) {
            Log.w(getClassName(), "invalid request")
            return null
        }
        return MDMMessage(authId, time, cameraDenied)
    }

    /**
     * 결과값을 담은 MDMMessage를 역직렬화 합니다.
     * @param mdmMessage 결과값을 담은 모델입니다.
     * @param publicKey 암호화할 공개키입니다.
     * @return 직렬화 성공시 직렬화(암호화)된 문자열, 실패시 null을 리턴합니다.
     */
    fun serialize(mdmMessage: MDMMessage, publicKey: String): String? {
        val response: String = mdmMessage.authID.plus("|")
            .plus(System.currentTimeMillis().toString())
            .plus("|")
            .plus((!mdmMessage.isMDMRequested).toString())
        val encryptResponse: String = rsaCrypto.encrypt(response, publicKey)
        if (encryptResponse.isEmpty()) {
            // 암호화 실패한경우 리턴
            Log.e(getClassName(), "Can't encrypt response Message..")
            return null
        }
        return encryptResponse
    }

    private fun checkTimeValid(time: Long): Boolean {
        // 2초보다 작은 경우 올바른 응답
        return (System.currentTimeMillis() - time) <= (MessageSerializer.TIMEOUT * 2000)
    }
}
