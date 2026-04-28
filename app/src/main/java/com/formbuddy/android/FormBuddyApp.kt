package com.formbuddy.android

import android.app.Application
import android.os.StrictMode
import androidx.hilt.work.HiltWorkerFactory
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.work.Configuration
import com.formbuddy.android.data.billing.BillingManager
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class FormBuddyApp : Application(), Configuration.Provider {

    @Inject lateinit var billingManager: BillingManager
    @Inject lateinit var workerFactory: HiltWorkerFactory

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .setMinimumLoggingLevel(android.util.Log.INFO)
            .build()

    override fun onCreate() {
        super.onCreate()

        if (BuildConfig.STRICT_MODE) {
            enableStrictMode()
        }

        // Defer heavy PDFBox init to background — not needed until user opens a form
        ProcessLifecycleOwner.get().lifecycleScope.launch(Dispatchers.IO) {
            com.tom_roush.pdfbox.android.PDFBoxResourceLoader.init(applicationContext)
        }

        // Connect to Play Billing so the Pro state is hot when the paywall opens.
        billingManager.start()
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
