package com.expira.app

import android.app.Application

class ExpiraApplication : Application() {
    val container: AppContainer by lazy { AppContainer(this) }
}
