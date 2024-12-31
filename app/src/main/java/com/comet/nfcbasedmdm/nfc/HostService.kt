package com.comet.nfcbasedmdm.nfc

import android.content.Intent
import android.nfc.cardemulation.HostApduService
import android.os.Bundle
import android.util.Log
import com.comet.nfcbasedmdm.common.cipher.RSACrypto
import com.comet.nfcbasedmdm.getClassName
import com.comet.nfcbasedmdm.mdm.connection.key.usecase.GetPublicKeyUseCase
import com.comet.nfcbasedmdm.mdm.data.model.MDMData
import com.comet.nfcbasedmdm.mdm.data.usecase.GetMDMDataUseCase
import com.skydoves.sandwich.getOrThrow
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject


/**
 * NFC 태깅시 처리하는 서비스
 */
@AndroidEntryPoint
class HostService : HostApduService() {

    // for encryption
    @Inject
    lateinit var rsaCrypto: RSACrypto

    // for get auth, uuid info from data
    @Inject
    lateinit var getMDMDataUseCase: GetMDMDataUseCase

    // for get public key
    @Inject
    lateinit var getPublicKeyUseCase: GetPublicKeyUseCase

    override fun processCommandApdu(commandApdu: ByteArray?, extras: Bundle?): ByteArray {
        Log.i(getClassName(), "Reading Card Reader")
        sendEncryptedData()
        return ByteArray(0);
    }

    override fun onDeactivated(reason: Int) {
        Log.i(getClassName(), "nfc disconnected.")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    // Coroutine을 이용한 비동기로 암호화된 UUID 정보를 전송해 태깅. NFC 연결이 종료됐을경우 시도되지 않고 오류 메시지 로깅
    // 만약 이 방식이 아니라면 processCommand를 블락해서 서버에서 RSA 퍼블릭키 가져오는 로직을 수행해야함 -> 너무 오래걸리거나 하면 문제 발생가능
    // 아니면 BroadcastReceiver를 사용해서 태깅 신호를 전달하고 Bind된 현재 서비스로 전달하는 방법도 있으나, 여기서 처리하는게 좋을듯
    private fun sendEncryptedData() {
        CoroutineScope(Dispatchers.IO).launch {
            // mdm 데이터가 init되지 않은경우 전송하지 않음
            val mdmData: MDMData = getMDMDataUseCase() ?: return@launch
            kotlin.runCatching {
                // 서버에서 public key 가져올때 실패시 throw 함
                val publicKey = getPublicKeyUseCase(mdmData.ip).getOrThrow().data
                // 암호화될 메시지
                val encryptMessage = mdmData.uuid.toString()
                    .plus("|")
                    .plus(System.currentTimeMillis().toString())
                // 암호화된 결과
                val encryptResult = rsaCrypto.encrypt(encryptMessage, publicKey)
                if (encryptResult.isEmpty()) //암호화 실패시 throw
                    throw IllegalStateException("Encrypt Failed.")

                // 전송될 메시지
                val lastMessage = encryptResult.plus("|Android") // 안드로이드임을 붙여서 전달
                // apdu
                val apduMessage: ByteArray = buildApdu(lastMessage)
                sendResponseApdu(apduMessage)
            }.onFailure {
                Log.w(getClassName(), "Can't send NFC Message ${it.message}")
            }
        }
    }

    /**
     *  NFC에서만 사용하는 APDU 빌드 함수
     *  @param data APDU에 담을 데이터
     *  @return 빌드된 APDU Message
     */
    private fun buildApdu(data: String): ByteArray {
        val bytes = data.toByteArray()
        val commandApdu = ByteArray(6 + bytes.size)
        //apdu 구성 00A40400 + 데이터 + LE
        commandApdu[0] = 0x00.toByte() // CLA
        commandApdu[1] = 0xA4.toByte() // INS
        commandApdu[2] = 0x04.toByte() // P1
        commandApdu[3] = 0x00.toByte() // P2
        commandApdu[4] = (bytes.size and 0x0FF).toByte() // Lc
        System.arraycopy(bytes, 0, commandApdu, 5, bytes.size)
        commandApdu[commandApdu.size - 1] = 0x00.toByte() // Le
        return commandApdu
    }

}