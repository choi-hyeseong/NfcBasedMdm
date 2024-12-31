package com.comet.nfcbasedmdm.module

import android.content.Context
import android.os.Build
import com.comet.nfcbasedmdm.camera.repository.AdminCameraRepository
import com.comet.nfcbasedmdm.camera.repository.CameraRepository
import com.comet.nfcbasedmdm.camera.repository.ThreadCameraRepository
import com.comet.nfcbasedmdm.common.cipher.AESCrypto
import com.comet.nfcbasedmdm.common.cipher.RSACrypto
import com.comet.nfcbasedmdm.common.storage.CryptoDataStore
import com.comet.nfcbasedmdm.mdm.auth.api.AuthAPI
import com.comet.nfcbasedmdm.mdm.auth.repository.AuthRepository
import com.comet.nfcbasedmdm.mdm.auth.repository.CryptAuthRepository
import com.comet.nfcbasedmdm.mdm.connection.key.api.PublicKeyAPI
import com.comet.nfcbasedmdm.mdm.connection.key.repository.PublicKeyRepository
import com.comet.nfcbasedmdm.mdm.connection.key.repository.RemotePublicKeyRepository
import com.comet.nfcbasedmdm.mdm.connection.websocket.repository.RemoteWebSocketRepository
import com.comet.nfcbasedmdm.mdm.connection.websocket.repository.WebsocketRepository
import com.comet.nfcbasedmdm.mdm.data.repository.EncryptMDMRepository
import com.comet.nfcbasedmdm.mdm.data.repository.MDMRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
class RepositoryModule {

    @Provides
    @Singleton
    fun provideCameraRepository(@ApplicationContext context : Context) : CameraRepository {
        // 버전별 호환성에 따른 다른 레포지토리 제공
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) ThreadCameraRepository(context)
        else AdminCameraRepository(context)
    }

    @Provides
    @Singleton
    fun provideAuthRepository(authAPI: AuthAPI, rsaCrypto: RSACrypto, aesCrypto: AESCrypto) : AuthRepository {
        return CryptAuthRepository(authAPI, rsaCrypto, aesCrypto)
    }

    @Provides
    @Singleton
    fun providePublicKeyRepository(publicKeyAPI: PublicKeyAPI) : PublicKeyRepository {
        return RemotePublicKeyRepository(publicKeyAPI)
    }

    @Provides
    @Singleton
    fun provideWebSocketRepository(okHttpClient: OkHttpClient) : WebsocketRepository {
        return RemoteWebSocketRepository(okHttpClient)
    }

    @Provides
    @Singleton
    fun provideEncryptMDMRepository(cryptoDataStore: CryptoDataStore) : MDMRepository {
        return EncryptMDMRepository(cryptoDataStore)
    }
}