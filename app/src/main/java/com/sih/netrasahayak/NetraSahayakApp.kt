package com.sih.netrasahayak

import android.app.Application
import com.sih.netrasahayak.di.ServiceLocator

class NetraSahayakApp : Application() {
    override fun onCreate() {
        super.onCreate()
        ServiceLocator.init(this)
    }
}
