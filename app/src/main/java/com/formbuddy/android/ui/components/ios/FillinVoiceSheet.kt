package com.formbuddy.android.ui.components.ios

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.formbuddy.android.ui.theme.IMessageBlue

/**
 * iOS-matching Assistant Voice picker (IMG_9049).
 *
 * The 10 OpenAI TTS voice presets the iOS app exposes:
 *   alloy · ash · ballad · coral · echo · fable · nova · onyx · sage · shimmer
 *
 * Each row is a circular blue play/stop button + the voice name + a blue
 * checkmark on the currently selected row. Tapping the play button asks
 * the host to play a sample (it should call the Firebase TTS callable
 * with `voice = id` and a short canned line); tapping the row name
 * selects the voice.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AssistantVoiceSheet(
    selectedVoice: String,
    playingVoice: String?,
    onSelect: (String) -> Unit,
    onPlay: (String) -> Unit,
    onStop: () -> Unit,
    onDismiss: () -> Unit,
    sheetState: SheetState = rememberModalBottomSheetState()
) {
    ModalBottomSheet(
        onDismissRequest = {
            onStop()
            onDismiss()
        },
        sheetState = sheetState,
        containerColor = Color(0xFF1C1C1E),
        contentColor = Color.White,
        dragHandle = { SheetHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = FillinSpacing.padding16)
        ) {
            Text(
                text = "Assistant Voice",
                color = Color.White,
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = FillinSpacing.padding8)
            )
            Text(
                text = "Tap ▶ to preview each voice. Tap the name to select.",
                color = Color(0xFF9C9CA1),
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp, bottom = FillinSpacing.padding16)
            )
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xFF2C2C2E))
            ) {
                items(VoiceOption.ALL, key = { it.id }) { voice ->
                    VoiceRow(
                        voice = voice,
                        isSelected = voice.id.equals(selectedVoice, ignoreCase = true),
                        isPlaying = voice.id.equals(playingVoice, ignoreCase = true),
                        onPlayToggle = {
                            if (voice.id.equals(playingVoice, ignoreCase = true)) onStop()
                            else onPlay(voice.id)
                        },
                        onSelect = { onSelect(voice.id) }
                    )
                    if (voice != VoiceOption.ALL.last()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 56.dp)
                                .height(0.5.dp)
                                .background(Color(0xFF3A3A3C))
                        )
                    }
                }
            }
            Spacer(Modifier.height(FillinSpacing.padding24))
        }
    }
}

@Composable
private fun VoiceRow(
    voice: VoiceOption,
    isSelected: Boolean,
    isPlaying: Boolean,
    onPlayToggle: () -> Unit,
    onSelect: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = FillinSpacing.padding12, vertical = FillinSpacing.padding8),
        verticalAlignment = Alignment.CenterVertically
    ) {
        FillinPressContainer(
            onClick = onPlayToggle,
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(IMessageBlue.copy(alpha = if (isPlaying) 1f else 0.18f))
        ) {
            Icon(
                imageVector = if (isPlaying) Icons.Filled.Stop else Icons.Filled.PlayArrow,
                contentDescription = if (isPlaying) "Stop" else "Play",
                tint = if (isPlaying) Color.White else IMessageBlue,
                modifier = Modifier.size(18.dp)
            )
        }
        Spacer(Modifier.width(FillinSpacing.padding12))
        FillinPressContainer(
            onClick = onSelect,
            modifier = Modifier.weight(1f)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = voice.displayName,
                    color = Color.White,
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                    modifier = Modifier.weight(1f)
                )
                if (isSelected) {
                    Icon(
                        Icons.Filled.Check,
                        contentDescription = "Selected",
                        tint = IMessageBlue,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

/** OpenAI TTS voice presets — same set the iOS app uses on the Firebase callable. */
data class VoiceOption(val id: String, val displayName: String) {
    companion object {
        val ALL: List<VoiceOption> = listOf(
            VoiceOption("alloy",   "Alloy"),
            VoiceOption("ash",     "Ash"),
            VoiceOption("ballad",  "Ballad"),
            VoiceOption("coral",   "Coral"),
            VoiceOption("echo",    "Echo"),
            VoiceOption("fable",   "Fable"),
            VoiceOption("nova",    "Nova"),
            VoiceOption("onyx",    "Onyx"),
            VoiceOption("sage",    "Sage"),
            VoiceOption("shimmer", "Shimmer")
        )
    }
}
