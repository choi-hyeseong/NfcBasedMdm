package com.comet.nfcbasedmdm.module

import com.comet.nfcbasedmdm.common.cipher.AESCrypto
import com.comet.nfcbasedmdm.common.cipher.RSACrypto
import com.comet.nfcbasedmdm.common.key.KeyProvider
import com.comet.nfcbasedmdm.common.key.KeyStoreProvider
import com.comet.nfcbasedmdm.common.storage.CryptoDataStore
import com.comet.nfcbasedmdm.common.storage.PreferenceDataStore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
class CryptoModule {

    @Provides
    @Singleton
    fun provideAESCrypto() : AESCrypto {
        return AESCrypto()
    }

    @Provides
    @Singleton
    fun provideRSACrypto() : RSACrypto {
        return RSACrypto()
    }

    @Provides
    @Singleton
    fun provideKeyProvider() : KeyProvider {
        return KeyStoreProvider()
    }

    @Provides
    @Singleton
    fun provideCryptoDataStore(preferenceDataStore: PreferenceDataStore, provider : KeyProvider, aesCrypto: AESCrypto) : CryptoDataStore {
        return CryptoDataStore(preferenceDataStore, provider.getKey(), aesCrypto)
    }
}