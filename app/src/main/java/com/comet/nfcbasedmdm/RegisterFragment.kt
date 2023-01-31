package com.comet.nfcbasedmdm

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.comet.nfcbasedmdm.callback.ActivityCallback
import com.comet.nfcbasedmdm.util.EncryptUtil
import com.fasterxml.jackson.databind.ObjectMapper
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.*
import java.util.concurrent.TimeUnit

class RegisterFragment : Fragment() {

    private var callback : ActivityCallback? = null
    private val client = OkHttpClient.Builder().connectTimeout(TIMEOUT, TimeUnit.SECONDS)
        .readTimeout(TIMEOUT, TimeUnit.SECONDS).build()
    private lateinit var imageView : ImageView
    private var isRequested = false
    set(value) { field = value
        if (value) //활성화 된경우
            imageView.visibility = ImageView.VISIBLE
        else //비활성화 된경우
            imageView.visibility = ImageView.INVISIBLE
    }
    private lateinit var thread : Thread

    override fun onAttach(context : Context) {
        super.onAttach(context)
        callback = context as ActivityCallback
    }

    override fun onDetach() {
        super.onDetach()
        callback = null
    }

    override fun onCreateView(
        inflater : LayoutInflater,
        container : ViewGroup?,
        savedInstanceState : Bundle?
    ) : View? {
        val view = inflater.inflate(R.layout.register_layout, container, false)
        val editText = view.findViewById<EditText>(R.id.server)
        imageView = view.findViewById(R.id.loading)
        Glide.with(this).load(R.drawable.loading).into(imageView)
        view.findViewById<Button>(R.id.submit).apply {
            setOnClickListener {
                if (!isRequested) {
                    isRequested = true
                    thread = Thread {
                        try {
                            val ip = editText.text
                            val mapper = ObjectMapper()
                            val uuid = UUID.randomUUID() //uuid 랜덤 생성
                            val encryptKey = mapper.readTree(client.newCall(Request.Builder()
                                                                                .url("$ip/encrypt")
                                                                                .build()
                            ).execute().body.string()
                            ).get("data").textValue()
                            val json = JSONObject().put(
                                "id",
                                EncryptUtil.RSAEncrypt(uuid.toString(), encryptKey)
                            )
                            val request = Request.Builder().url("${ip}/auth")
                                .header("Content-type", "application/json")
                                .post(json.toString().toRequestBody()).build()

                            val response = client.newCall(request).execute()
                            if (!response.isSuccessful)
                                isRequested = false
                            else {
                                val data = response.body.string()
                                if (data.isEmpty()) {
                                    Log.w(LOG_TAG, "Response error : $response.code")
                                    sendToast(getString(R.string.response_error))
                                }
                                else {
                                    //json 오브젝트로 받기.
                                    val readData = JSONObject(data).getJSONObject("data")
                                    //복호화
                                    val decryptAuth = EncryptUtil.AESDecrypt(
                                        readData.get("auth").toString(),
                                        uuid.toString()
                                    )
                                    val decryptDelete = EncryptUtil.AESDecrypt(
                                        readData.get("delete").toString(),
                                        uuid.toString()
                                    )
                                    if (decryptAuth.length == 10 && decryptDelete.length == 10) {
                                        //올바른 데이터
                                        callback?.saveToken(
                                            uuid.toString(),
                                            decryptAuth,
                                            decryptDelete,
                                            ip.toString().replace("http://", "") //http제거후 추가.
                                        )
                                        callback?.switch(MainFragment(), false)
                                    }
                                }
                            }
                            /*.enqueue(object : Callback {
                                    override fun onFailure(call: Call, e: IOException) {
                                        isRequested = false
                                        sendToast(getString(R.string.response_error))
                                        e.localizedMessage?.also { Log.e(LOG_TAG, it) }
                                    }

                                    override fun onResponse(call: Call, response: Response) {
                                        isRequested = false
                                        val data = response.body.string()
                                        if(data.isEmpty()) {
                                            Log.w(LOG_TAG, "Response error : $response.code")
                                            sendToast(getString(R.string.response_error))
                                            return
                                        }
                                        //json 오브젝트로 받기.
                                        val readData = JSONObject(data).getJSONObject("data")
                                        //복호화
                                        val decryptAuth = EncryptUtil.AESDecrypt(
                                            readData.get("auth").toString(),
                                            uuid.toString()
                                        )
                                        val decryptDelete = EncryptUtil.AESDecrypt(
                                            readData.get("delete").toString(),
                                            uuid.toString()
                                        )
                                        if(decryptAuth.length == 10 && decryptDelete.length == 10) {
                                            //올바른 데이터
                                            callback?.saveToken(
                                                uuid.toString(),
                                                decryptAuth,
                                                decryptDelete,
                                                editText.text.toString()
                                                    .replace("http://", "") //http제거후 추가.
                                            )
                                            callback?.switch(MainFragment(), false)
                                        }
                                        else
                                            sendToast(getString(R.string.response_error))
                                    }

                                }) //비동기
                                */
                        }
                        catch (e : Exception) {
                            e.localizedMessage?.also { Log.e(LOG_TAG, it) }
                            sendToast(getString(R.string.response_error))
                            isRequested = false
                        }
                    }.also { it.start() }
                }
                else
                    callback?.runOnMainThread {
                        sendToast(getString(R.string.wait_response))
                    }

            }
        }
        return view
    }

    private fun sendToast(str : String) {
        callback?.runOnMainThread {
            Toast.makeText(context, str, Toast.LENGTH_LONG).show()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (isRequested)
            thread.interrupt()
    }
}