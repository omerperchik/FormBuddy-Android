package com.formbuddy.android.ui.components

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

/**
 * Wraps [content] behind a biometric (fingerprint/face) or device-credential prompt.
 *
 * If [enabled] is `false`, content shows immediately. Otherwise we run a
 * [BiometricPrompt] every time we enter the composable, and only show [content]
 * after the user authenticates. On unsupported devices the prompt is bypassed
 * with a warning so the user is never permanently locked out of their data.
 *
 * Mirrors iOS behaviour where `SDProfile.isPrivate == true` requires Face ID /
 * Touch ID / passcode before opening.
 */
@Composable
fun BiometricGate(
    enabled: Boolean,
    title: String,
    subtitle: String,
    cancelLabel: String,
    onCancel: () -> Unit,
    content: @Composable () -> Unit
) {
    if (!enabled) {
        content()
        return
    }

    val context = LocalContext.current
    val activity = remember(context) { context.findFragmentActivity() }
    var authenticated by remember(enabled, activity) { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(activity, enabled) {
        if (activity == null) {
            // Without a FragmentActivity host we can't show BiometricPrompt; let the
            // user through but log a warning. This shouldn't happen in production.
            authenticated = true
            return@LaunchedEffect
        }
        val available = BiometricManager.from(context).canAuthenticate(ALLOWED_AUTHENTICATORS)
        if (available != BiometricManager.BIOMETRIC_SUCCESS) {
            authenticated = true
            return@LaunchedEffect
        }
        val executor = ContextCompat.getMainExecutor(context)
        val prompt = BiometricPrompt(
            activity,
            executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    authenticated = true
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    error = errString.toString()
                }
            }
        )
        prompt.authenticate(
            BiometricPrompt.PromptInfo.Builder()
                .setTitle(title)
                .setSubtitle(subtitle)
                .setNegativeButtonText(cancelLabel)
                .setAllowedAuthenticators(ALLOWED_AUTHENTICATORS)
                .build()
        )
    }

    if (authenticated) {
        content()
    } else {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                Icons.Filled.Lock,
                contentDescription = null,
                modifier = Modifier.height(48.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(title, style = MaterialTheme.typography.titleLarge)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                error ?: subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(24.dp))
            Button(onClick = onCancel) { Text(cancelLabel) }
        }
    }
}

private const val ALLOWED_AUTHENTICATORS =
    BiometricManager.Authenticators.BIOMETRIC_WEAK or
        BiometricManager.Authenticators.DEVICE_CREDENTIAL

private fun Context.findFragmentActivity(): FragmentActivity? {
    var ctx: Context? = this
    while (ctx is ContextWrapper) {
        if (ctx is FragmentActivity) return ctx
        if (ctx is Activity) return null
        ctx = ctx.baseContext
    }
    return null
}
