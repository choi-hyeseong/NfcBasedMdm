package com.comet.nfcbasedmdm.module

import android.content.Context
import com.comet.nfcbasedmdm.common.storage.CryptoDataStore
import com.comet.nfcbasedmdm.common.storage.PreferenceDataStore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
class CommonModule {


    @Provides
    @Singleton
    fun providePreferenceDataStore(@ApplicationContext context : Context) : PreferenceDataStore {
        return PreferenceDataStore(context)
    }

}