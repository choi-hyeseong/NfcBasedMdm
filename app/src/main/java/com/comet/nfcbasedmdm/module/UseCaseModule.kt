package com.comet.nfcbasedmdm.module

import com.comet.nfcbasedmdm.camera.repository.CameraRepository
import com.comet.nfcbasedmdm.camera.usecase.CheckCameraEnabledUseCase
import com.comet.nfcbasedmdm.camera.usecase.DisableCameraUseCase
import com.comet.nfcbasedmdm.camera.usecase.EnableCameraUseCase
import com.comet.nfcbasedmdm.mdm.auth.repository.AuthRepository
import com.comet.nfcbasedmdm.mdm.auth.usecase.RegisterUseCase
import com.comet.nfcbasedmdm.mdm.connection.key.repository.PublicKeyRepository
import com.comet.nfcbasedmdm.mdm.connection.key.usecase.GetPublicKeyUseCase
import com.comet.nfcbasedmdm.mdm.connection.websocket.repository.WebsocketRepository
import com.comet.nfcbasedmdm.mdm.connection.websocket.usecase.WebSocketCheckConnectionUseCase
import com.comet.nfcbasedmdm.mdm.connection.websocket.usecase.WebSocketConnectUseCase
import com.comet.nfcbasedmdm.mdm.connection.websocket.usecase.WebSocketDisconnectUseCase
import com.comet.nfcbasedmdm.mdm.connection.websocket.usecase.WebSocketSendMessageUseCase
import com.comet.nfcbasedmdm.mdm.data.repository.MDMRepository
import com.comet.nfcbasedmdm.mdm.data.usecase.GetMDMDataUseCase
import com.comet.nfcbasedmdm.mdm.data.usecase.SaveMDMDataUseCase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
class UseCaseModule {

    @Provides
    @Singleton
    fun provideCheckCameraEnabledUseCase(cameraRepository: CameraRepository) : CheckCameraEnabledUseCase {
        return CheckCameraEnabledUseCase(cameraRepository)
    }

    @Provides
    @Singleton
    fun provideDisableCameraUseCase(cameraRepository: CameraRepository) : DisableCameraUseCase {
        return DisableCameraUseCase(cameraRepository)
    }


    @Provides
    @Singleton
    fun provideEnableCameraUseCase(cameraRepository: CameraRepository) : EnableCameraUseCase {
        return EnableCameraUseCase(cameraRepository)
    }

    @Provides
    @Singleton
    fun provideRegisterUseCase(authRepository: AuthRepository) : RegisterUseCase {
        return RegisterUseCase(authRepository)
    }

    @Provides
    @Singleton
    fun provideGetPublicKeyUseCase(publicKeyRepository: PublicKeyRepository) : GetPublicKeyUseCase {
        return GetPublicKeyUseCase(publicKeyRepository)
    }

    @Provides
    @Singleton
    fun provideWebSocketCheckConnectionUseCase(websocketRepository: WebsocketRepository) : WebSocketCheckConnectionUseCase {
        return WebSocketCheckConnectionUseCase(websocketRepository)
    }

    @Provides
    @Singleton
    fun provideWWebSocketConnectUseCase(websocketRepository: WebsocketRepository) : WebSocketConnectUseCase {
        return WebSocketConnectUseCase(websocketRepository)
    }

    @Provides
    @Singleton
    fun provideWebSocketDisconnectUseCase(websocketRepository: WebsocketRepository) : WebSocketDisconnectUseCase {
        return WebSocketDisconnectUseCase(websocketRepository)
    }

    @Provides
    @Singleton
    fun provideWebSocketSendMessageUseCase(websocketRepository: WebsocketRepository) : WebSocketSendMessageUseCase {
        return WebSocketSendMessageUseCase(websocketRepository)
    }

    @Provides
    @Singleton
    fun provideMDMDataUseCase(mdmRepository: MDMRepository) : GetMDMDataUseCase {
        return GetMDMDataUseCase(mdmRepository)
    }

    @Provides
    @Singleton
    fun provideSaveMDMDataUseCase(mdmRepository: MDMRepository) : SaveMDMDataUseCase {
        return SaveMDMDataUseCase(mdmRepository)
    }

}