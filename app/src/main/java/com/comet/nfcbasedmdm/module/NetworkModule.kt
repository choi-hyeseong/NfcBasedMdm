package com.comet.nfcbasedmdm.module

import com.comet.nfcbasedmdm.common.cipher.AESCrypto
import com.comet.nfcbasedmdm.common.cipher.RSACrypto
import com.comet.nfcbasedmdm.mdm.auth.api.AuthAPI
import com.comet.nfcbasedmdm.mdm.connection.key.api.PublicKeyAPI
import com.comet.nfcbasedmdm.service.serialize.MessageSerializer
import com.google.gson.Gson
import com.skydoves.sandwich.retrofit.adapters.ApiResponseCallAdapterFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
class NetworkModule {

    companion object {
        private const val TIMEOUT = 2L
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        return OkHttpClient.Builder()
            .connectTimeout(TIMEOUT, TimeUnit.SECONDS)
            .readTimeout(TIMEOUT, TimeUnit.SECONDS)
            .pingInterval(TIMEOUT, TimeUnit.SECONDS) //for ping-pong interval
            .build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(): Retrofit {
        return Retrofit.Builder()
            .baseUrl("http://127.0.0.1/")
            .addCallAdapterFactory(ApiResponseCallAdapterFactory.create())
            .addConverterFactory(GsonConverterFactory.create(Gson()))
            .build()
    }

    @Provides
    @Singleton
    fun provideMessageSerializer(rsaCrypto: RSACrypto, aesCrypto: AESCrypto) : MessageSerializer {
        return MessageSerializer(aesCrypto, rsaCrypto)
    }

    @Provides
    @Singleton
    fun provideAuthAPI(retrofit: Retrofit) : AuthAPI {
        return retrofit.create(AuthAPI::class.java)
    }

    @Provides
    @Singleton
    fun providePublicKeyAPI(retrofit: Retrofit) : PublicKeyAPI {
        return retrofit.create(PublicKeyAPI::class.java)
    }
}