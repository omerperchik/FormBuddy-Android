package com.formbuddy.android

import android.app.Application
import android.os.StrictMode
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@HiltAndroidApp
class FormBuddyApp : Application() {
    override fun onCreate() {
        super.onCreate()

        if (BuildConfig.STRICT_MODE) {
            enableStrictMode()
        }

        // Defer heavy PDFBox init to background — not needed until user opens a form
        ProcessLifecycleOwner.get().lifecycleScope.launch(Dispatchers.IO) {
            com.tom_roush.pdfbox.android.PDFBoxResourceLoader.init(applicationContext)
        }
    }

    private fun enableStrictMode() {
        StrictMode.setThreadPolicy(
            StrictMode.ThreadPolicy.Builder()
                .detectDiskReads()
                .detectDiskWrites()
                .detectNetwork()
                .penaltyLog()
                .build()
        )
        StrictMode.setVmPolicy(
            StrictMode.VmPolicy.Builder()
                .detectLeakedSqlLiteObjects()
                .detectLeakedClosableObjects()
                .detectActivityLeaks()
                .penaltyLog()
                .build()
        )
    }
}
