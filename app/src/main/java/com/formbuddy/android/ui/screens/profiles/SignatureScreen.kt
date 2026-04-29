package com.formbuddy.android.ui.screens.profiles

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBackIos
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.formbuddy.android.ui.components.SignatureCanvas
import com.formbuddy.android.ui.components.SignatureStroke
import com.formbuddy.android.ui.components.ios.FillinPressContainer
import com.formbuddy.android.ui.components.ios.FillinShapes
import com.formbuddy.android.ui.components.ios.FillinSpacing
import com.formbuddy.android.ui.components.pathToSignatureStrokes
import com.formbuddy.android.ui.components.signatureStrokesToPath
import com.formbuddy.android.ui.theme.DarkSurface
import com.formbuddy.android.ui.theme.DestructiveRed

/**
 * iOS-matching `Edit Signature` screen (IMG_9045).
 *
 * Layout:
 *   - Pure-black background.
 *   - Round dark back-chevron pill (top-left, status-bar inset).
 *   - "Edit Signature" — extra-bold display title (~36 sp), top-left.
 *   - Centered signature canvas card (dark gray rounded rect, ~70% width).
 *   - "Clear" link in destructive red, centered below the canvas.
 *   - Saving is implicit: tapping the back chevron persists.
 */
@Composable
fun SignatureScreen(
    navController: NavHostController,
    profileId: String,
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val profile by viewModel.profile.collectAsState()
    var strokes by remember { mutableStateOf<List<SignatureStroke>>(emptyList()) }

    LaunchedEffect(profileId) { viewModel.loadProfile(profileId) }
    LaunchedEffect(profile) {
        profile?.signaturePath?.let { strokes = pathToSignatureStrokes(it) }
    }

    val saveAndPop: () -> Unit = {
        val pathString = signatureStrokesToPath(strokes)
        viewModel.updateField { copy(signaturePath = pathString.ifBlank { null }) }
        navController.popBackStack()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        Column(modifier = Modifier.fillMaxSize().statusBarsPadding()) {

            // Back chevron pill (top-left).
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = FillinSpacing.padding16, vertical = FillinSpacing.padding8)
            ) {
                FillinPressContainer(
                    onClick = saveAndPop,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF1F1F1F))
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBackIos,
                        contentDescription = "Back",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Text(
                text = "Edit Signature",
                color = Color.White,
                style = MaterialTheme.typography.displayMedium.copy(fontWeight = FontWeight.Bold),
                modifier = Modifier.padding(
                    start = FillinSpacing.padding16,
                    end = FillinSpacing.padding16,
                    top = FillinSpacing.padding4,
                    bottom = FillinSpacing.padding24
                )
            )

            Spacer(Modifier.weight(0.6f))

            // Centered signature canvas card. The existing SignatureCanvas hosts
            // the drawing logic; we wrap it in a dark-gray rounded container.
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = FillinSpacing.padding16)
                    .height(220.dp)
                    .clip(FillinShapes.large)
                    .background(DarkSurface)
            ) {
                SignatureCanvas(
                    strokes = strokes,
                    onStrokesChanged = { strokes = it },
                    backgroundColor = Color(0xFF3A3A3C),
                    strokeColor = Color.Black,
                    modifier = Modifier.fillMaxSize()
                )
            }

            Spacer(Modifier.weight(0.6f))

            // Clear link.
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 80.dp),
                contentAlignment = Alignment.Center
            ) {
                FillinPressContainer(
                    onClick = { strokes = emptyList() }
                ) {
                    Text(
                        text = "Clear",
                        color = DestructiveRed,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
                    )
                }
            }
        }
    }
}
