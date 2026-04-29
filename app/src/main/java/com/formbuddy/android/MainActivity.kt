package com.formbuddy.android

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.fragment.app.FragmentActivity
import com.formbuddy.android.data.share.DeepLink
import com.formbuddy.android.ui.navigation.FormBuddyNavHost
import com.formbuddy.android.ui.theme.FormBuddyTheme
import dagger.hilt.android.AndroidEntryPoint

// Extends FragmentActivity (which extends ComponentActivity) so the AndroidX
// BiometricPrompt API can attach its dialog fragment for private-profile gating.
@AndroidEntryPoint
class MainActivity : FragmentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Prevent screenshots/screen recording — form data is sensitive
        if (!BuildConfig.DEBUG) {
            window.setFlags(
                WindowManager.LayoutParams.FLAG_SECURE,
                WindowManager.LayoutParams.FLAG_SECURE
            )
        }

        val payload = unpackIntent(intent)

        setContent {
            FormBuddyTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    FormBuddyNavHost(
                        importUri = payload.contentUri,
                        importMimeType = payload.contentMimeType,
                        deepLink = payload.deepLink
                    )
                }
            }
        }
    }

    /** Parses the launch intent into the three things the NavHost cares about. */
    private fun unpackIntent(intent: Intent?): Payload {
        intent ?: return Payload()
        val action = intent.action
        // ACTION_SEND with image/* or application/pdf — shared from another app.
        if (action == Intent.ACTION_SEND) {
            val stream = intent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM)
            if (stream != null) return Payload(contentUri = stream, contentMimeType = intent.type)
        }
        // ACTION_VIEW — either a PDF MIME, an https://formbuddy.app/* AppLink,
        // or a formbuddy:// custom-scheme link.
        if (action == Intent.ACTION_VIEW) {
            val data = intent.data
            val deep = DeepLink.parse(data)
            if (deep !is DeepLink.Parsed.None) return Payload(deepLink = deep)
            if (data != null) return Payload(contentUri = data, contentMimeType = intent.type)
        }
        return Payload()
    }

    private data class Payload(
        val contentUri: Uri? = null,
        val contentMimeType: String? = null,
        val deepLink: DeepLink.Parsed = DeepLink.Parsed.None
    )
}
