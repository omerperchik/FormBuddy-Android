package com.formbuddy.android.ui.screens.scanner

import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.NavHostController
import com.formbuddy.android.ui.components.ios.FillinPressContainer
import com.formbuddy.android.ui.components.ios.FillinShapes
import com.formbuddy.android.ui.components.ios.FillinSpacing
import com.formbuddy.android.ui.navigation.Screen
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions
import com.google.mlkit.vision.documentscanner.GmsDocumentScanning
import com.google.mlkit.vision.documentscanner.GmsDocumentScanningResult
import java.io.File

/** Walks ContextWrappers to find a real Activity. */
private fun android.content.Context.findActivity(): android.app.Activity? {
    var ctx: android.content.Context? = this
    while (ctx is android.content.ContextWrapper) {
        if (ctx is android.app.Activity) return ctx
        ctx = ctx.baseContext
    }
    return null
}

/**
 * iOS-matching document scanner (IMG_9051).
 *
 * Layout:
 *   - Full-screen camera preview.
 *   - Top-left circular X close button (status-bar inset).
 *   - Centered "Position the document in view." pill toast (dark
 *     translucent capsule with white text).
 *   - Big white circular shutter button at the bottom.
 *
 * Uses [androidx.lifecycle.compose.LocalLifecycleOwner] so we don't pull in
 * the deprecated platform variant.
 */
@Composable
fun ScannerScreen(
    navController: NavHostController
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }
    var useFallback by remember { mutableStateOf(false) }

    // Try the ML Kit document scanner first — it gives perspective-corrected
    // PDFs out-of-the-box. If launching the intent fails (e.g. AOSP without
    // GMS, China-region devices, or the dynamic module isn't available),
    // we fall back to a CameraX-driven shutter UI.
    val mlkitLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        val data = result.data
        if (data != null) {
            val scanResult = GmsDocumentScanningResult.fromActivityResultIntent(data)
            val pdfUri = scanResult?.pdf?.uri
            if (pdfUri != null) {
                navController.navigate(Screen.Filling.createRoute("scan", uri = pdfUri.toString()))
                return@rememberLauncherForActivityResult
            }
        }
        // Cancelled or empty — fall through to CameraX fallback so the
        // user isn't stuck in a loop.
        useFallback = true
    }

    LaunchedEffect(Unit) {
        if (useFallback) return@LaunchedEffect
        val options = GmsDocumentScannerOptions.Builder()
            .setGalleryImportAllowed(true)
            .setPageLimit(20)
            .setResultFormats(GmsDocumentScannerOptions.RESULT_FORMAT_PDF)
            .setScannerMode(GmsDocumentScannerOptions.SCANNER_MODE_FULL)
            .build()
        val activity = context.findActivity()
        if (activity == null) {
            useFallback = true
            return@LaunchedEffect
        }
        runCatching {
            GmsDocumentScanning.getClient(options).getStartScanIntent(activity)
                .addOnSuccessListener { intentSender ->
                    mlkitLauncher.launch(
                        androidx.activity.result.IntentSenderRequest.Builder(intentSender).build()
                    )
                }
                .addOnFailureListener { useFallback = true }
        }.onFailure { useFallback = true }
    }

    if (!useFallback) {
        // Render an empty black screen while the ML Kit scanner is loading.
        Box(modifier = Modifier.fillMaxSize().background(Color.Black))
        return
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        // Camera preview.
        AndroidView(
            factory = { ctx ->
                val previewView = PreviewView(ctx)
                val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                cameraProviderFuture.addListener({
                    val cameraProvider = cameraProviderFuture.get()
                    val preview = Preview.Builder().build().also {
                        it.setSurfaceProvider(previewView.surfaceProvider)
                    }
                    val capture = ImageCapture.Builder()
                        .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
                        .build()
                    imageCapture = capture
                    val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
                    runCatching {
                        cameraProvider.unbindAll()
                        cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector, preview, capture)
                    }
                }, ContextCompat.getMainExecutor(ctx))
                previewView
            },
            modifier = Modifier.fillMaxSize()
        )

        // Top-left X close (matches iOS — circular dark pill).
        FillinPressContainer(
            onClick = { navController.popBackStack() },
            modifier = Modifier
                .align(Alignment.TopStart)
                .statusBarsPadding()
                .padding(FillinSpacing.padding16)
                .size(36.dp)
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.55f))
        ) {
            androidx.compose.material3.Icon(
                imageVector = Icons.Filled.Close,
                contentDescription = "Close",
                tint = Color.White,
                modifier = Modifier.size(18.dp)
            )
        }

        // Centered top instruction pill toast.
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(top = FillinSpacing.padding16 + FillinSpacing.padding4)
                .clip(FillinShapes.capsule)
                .background(Color.Black.copy(alpha = 0.55f))
                .padding(horizontal = FillinSpacing.padding16, vertical = FillinSpacing.padding8)
        ) {
            Text(
                text = "Position the document in view.",
                color = Color.White,
                style = MaterialTheme.typography.bodyMedium
            )
        }

        // Big white shutter button at the bottom (~80 dp).
        FillinPressContainer(
            onClick = {
                val capture = imageCapture ?: return@FillinPressContainer
                val photoFile = File(context.cacheDir, "scan_${System.currentTimeMillis()}.jpg")
                val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()
                capture.takePicture(
                    outputOptions,
                    ContextCompat.getMainExecutor(context),
                    object : ImageCapture.OnImageSavedCallback {
                        override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                            navController.navigate(
                                Screen.Filling.createRoute("scan", uri = photoFile.toURI().toString())
                            )
                        }
                        override fun onError(exception: ImageCaptureException) {}
                    }
                )
            },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 32.dp)
                .size(78.dp)
                .clip(CircleShape)
                .background(Color.White)
        ) {
            // Inner ring effect — a slightly smaller circle outlined in black for the iOS shutter look.
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(Color.White)
            )
        }
    }
}
