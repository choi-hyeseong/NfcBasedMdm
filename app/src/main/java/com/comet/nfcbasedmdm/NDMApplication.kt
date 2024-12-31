package com.comet.nfcbasedmdm

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class NDMApplication  : Application() {

    override fun onCreate() {
        super.onCreate()
    }
}