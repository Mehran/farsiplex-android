package com.mehran.farsiplex

import android.app.Application
import com.mehran.farsiplex.data.FarsiPlexRepository
import com.mehran.farsiplex.data.Http

/** Tiny manual DI container; everything is stateless and cheap to hold. */
class AppContainer(@Suppress("UNUSED_PARAMETER") app: Application) {
    val http = Http()
    val repository = FarsiPlexRepository(http)
}

class FarsiPlexApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
