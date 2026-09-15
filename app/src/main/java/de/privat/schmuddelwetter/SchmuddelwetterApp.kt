package de.privat.schmuddelwetter

import android.app.Application
import de.privat.schmuddelwetter.di.ServiceLocator

class SchmuddelwetterApp : Application() {
    lateinit var serviceLocator: ServiceLocator
        private set

    override fun onCreate() {
        super.onCreate()
        serviceLocator = ServiceLocator(this)
    }
}
