package com.formbuddy.android.ui.components.ios

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.UploadFile
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.formbuddy.android.ui.theme.IMessageBlue

/**
 * iOS-style "Default Form Filling Mode" bottom sheet (IMG_9050).
 *
 * Three large rows: Chat / Voice / Agent. The first is selected (blue tint
 * border, check on the right), the other two carry a PRO pill.
 *
 * Mirrors `Fillin/UI/Views/Settings/Views/DefaultFormModeSheetView.swift`.
 */
enum class FormModeSheetOption { Chat, Voice, Agent }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DefaultFormModeSheet(
    selected: FormModeSheetOption,
    onSelect: (FormModeSheetOption) -> Unit,
    onDismiss: () -> Unit,
    sheetState: SheetState = rememberModalBottomSheetState()
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color(0xFF1C1C1E),
        contentColor = Color.White,
        dragHandle = { SheetHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = FillinSpacing.padding16, vertical = FillinSpacing.padding8),
            verticalArrangement = Arrangement.spacedBy(FillinSpacing.padding8)
        ) {
            Text(
                text = "Default Form Filling Mode",
                color = Color.White,
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = FillinSpacing.padding8),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            Text(
                text = "Choose how you want to fill forms",
                color = Color(0xFF9C9CA1),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = FillinSpacing.padding16),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            ModeRow(
                option = FormModeSheetOption.Chat,
                title = "Chat Mode",
                subtitle = "Fill forms with AI-powered suggestions",
                icon = Icons.Filled.Description,
                isPro = false,
                isSelected = selected == FormModeSheetOption.Chat,
                onSelect = onSelect
            )
            ModeRow(
                option = FormModeSheetOption.Voice,
                title = "Voice Mode",
                subtitle = "Speak to fill your forms hands-free",
                icon = Icons.Filled.Mic,
                isPro = true,
                isSelected = selected == FormModeSheetOption.Voice,
                onSelect = onSelect
            )
            ModeRow(
                option = FormModeSheetOption.Agent,
                title = "Agent Mode",
                subtitle = "AI auto-fills the entire form for you",
                icon = Icons.Filled.AutoAwesome,
                isPro = true,
                isSelected = selected == FormModeSheetOption.Agent,
                onSelect = onSelect
            )
            Spacer(Modifier.height(FillinSpacing.padding24))
        }
    }
}

@Composable
private fun ModeRow(
    option: FormModeSheetOption,
    title: String,
    subtitle: String,
    icon: ImageVector,
    isPro: Boolean,
    isSelected: Boolean,
    onSelect: (FormModeSheetOption) -> Unit
) {
    val borderColor = if (isSelected) IMessageBlue else Color.Transparent
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(if (isSelected) IMessageBlue.copy(alpha = 0.18f) else Color(0xFF2C2C2E))
            .androidx_borderHack(borderColor)
    ) {
        FillinPressContainer(
            onClick = { onSelect(option) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = FillinSpacing.padding16, vertical = FillinSpacing.padding16),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(22.dp))
                Spacer(Modifier.width(FillinSpacing.padding12))
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = title,
                            color = Color.White,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
                        )
                        if (isPro) {
                            Spacer(Modifier.width(FillinSpacing.padding6))
                            ProPill()
                        }
                    }
                    Text(
                        text = subtitle,
                        color = Color(0xFF9C9CA1),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                if (isSelected) {
                    Icon(
                        Icons.Filled.Check,
                        contentDescription = null,
                        tint = IMessageBlue,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }
    }
}

/**
 * iOS-style "Upload" bottom sheet (IMG_9052) — small icon header with
 * "Upload" title plus two big rows: Upload File / Upload Photo.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UploadFormSheet(
    onPickFile: () -> Unit,
    onPickPhoto: () -> Unit,
    onDismiss: () -> Unit,
    sheetState: SheetState = rememberModalBottomSheetState()
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color(0xFF1C1C1E),
        contentColor = Color.White,
        dragHandle = { SheetHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = FillinSpacing.padding16, vertical = FillinSpacing.padding8),
            verticalArrangement = Arrangement.spacedBy(FillinSpacing.padding12),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(IMessageBlue.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Filled.UploadFile,
                    contentDescription = null,
                    tint = IMessageBlue,
                    modifier = Modifier.size(22.dp)
                )
            }
            Text(
                text = "Upload",
                color = Color.White,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
            )
            Spacer(Modifier.height(FillinSpacing.padding4))
            UploadRow(
                title = "Upload File",
                subtitle = "PDF, DOC, or image",
                icon = Icons.Filled.UploadFile,
                tint = IMessageBlue,
                onClick = onPickFile
            )
            UploadRow(
                title = "Upload Photo",
                subtitle = "Choose from your photo library",
                icon = Icons.Filled.PhotoLibrary,
                tint = Color(0xFF34C759),
                onClick = onPickPhoto
            )
            Spacer(Modifier.height(FillinSpacing.padding24))
        }
    }
}

@Composable
private fun UploadRow(
    title: String,
    subtitle: String,
    icon: ImageVector,
    tint: Color,
    onClick: () -> Unit
) {
    FillinPressContainer(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFF2C2C2E))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = FillinSpacing.padding16, vertical = FillinSpacing.padding16),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(tint),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.width(FillinSpacing.padding12))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
                )
                Text(
                    text = subtitle,
                    color = Color(0xFF9C9CA1),
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

/** Drag handle shared by every Fillin bottom sheet in this package. */
@Composable
internal fun SheetHandle() {
    Box(
        modifier = Modifier
            .padding(top = FillinSpacing.padding8, bottom = FillinSpacing.padding4)
            .fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .width(36.dp)
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(Color(0xFF6C6C72))
        )
    }
}

/** Adds a subtle border without requiring a foundation.border import everywhere. */
private fun Modifier.androidx_borderHack(color: Color): Modifier =
    this.then(border(width = 1.dp, color = color, shape = RoundedCornerShape(14.dp)))
