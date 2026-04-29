package com.formbuddy.android.ui.screens.settings

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Gavel
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.formbuddy.android.BuildConfig
import com.formbuddy.android.R
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import com.formbuddy.android.data.model.FormMode
import com.formbuddy.android.ui.components.ios.DefaultFormModeSheet
import com.formbuddy.android.ui.components.ios.FillinLogo
import com.formbuddy.android.ui.components.ios.FormModeSheetOption
import com.formbuddy.android.ui.components.ios.AssistantVoiceSheet
import com.formbuddy.android.ui.components.ios.LanguageOption
import com.formbuddy.android.ui.components.ios.LanguageSheet
import com.formbuddy.android.ui.components.ios.FillinPressContainer
import com.formbuddy.android.ui.components.ios.FillinSeparator
import com.formbuddy.android.ui.components.ios.FillinShapes
import com.formbuddy.android.ui.components.ios.FillinSpacing
import com.formbuddy.android.ui.navigation.Screen
import com.formbuddy.android.ui.theme.FreePlanOutline
import com.formbuddy.android.ui.theme.GroupedCard
import com.formbuddy.android.ui.theme.IMessageBlue
import com.formbuddy.android.ui.theme.ProBadgePurple

/**
 * iOS-matching Settings screen (IMG_9047 / IMG_9048).
 *
 * Layout:
 *   - Pure-black background, status-bar inset only.
 *   - Top-left: [FillinLogo] + extra-bold "Settings" title.
 *   - "Free Plan" hero card (thin violet glow border, chevron right).
 *   - Grouped sections (no header label) with hairline separators between
 *     rows: Assistant Voice → Default Form Mode → Language → Rate us → Share.
 *   - "Help" section: Contact us → Tutorial → Restore Purchases.
 *   - "Legal" section: Terms → Privacy.
 *   - Footer: app version centered, gray.
 */
@Composable
fun SettingsScreen(
    navController: NavHostController,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val formMode by viewModel.formMode.collectAsState()
    val assistantVoice by viewModel.assistantVoice.collectAsState()
    val context = LocalContext.current
    var showFormModeSheet by remember { mutableStateOf(false) }
    var showVoiceSheet by remember { mutableStateOf(false) }
    var showLanguageSheet by remember { mutableStateOf(false) }

    val activeLocaleTag = remember {
        AppCompatDelegate.getApplicationLocales()
            .takeIf { !it.isEmpty }
            ?.get(0)
            ?.toLanguageTag()
            ?: LanguageOption.ALL.first().tag
    }

    if (showLanguageSheet) {
        LanguageSheet(
            selectedTag = activeLocaleTag,
            onSelect = { tag ->
                AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(tag))
                showLanguageSheet = false
            },
            onDismiss = { showLanguageSheet = false }
        )
    }

    if (showFormModeSheet) {
        DefaultFormModeSheet(
            selected = formMode.toSheetOption(),
            onSelect = {
                viewModel.setFormMode(it.toFormMode())
                showFormModeSheet = false
            },
            onDismiss = { showFormModeSheet = false }
        )
    }

    if (showVoiceSheet) {
        val playingVoice by viewModel.playingVoice.collectAsState()
        AssistantVoiceSheet(
            selectedVoice = assistantVoice,
            playingVoice = playingVoice,
            onSelect = {
                viewModel.setAssistantVoice(it)
                viewModel.stopVoiceSample()
                showVoiceSheet = false
            },
            onPlay = { viewModel.playVoiceSample(it) },
            onStop = { viewModel.stopVoiceSample() },
            onDismiss = {
                viewModel.stopVoiceSample()
                showVoiceSheet = false
            }
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 120.dp)
        ) {
            // Logo + bold title.
            FillinLogo(
                modifier = Modifier.padding(start = FillinSpacing.padding16, top = FillinSpacing.padding8),
                size = 38.dp
            )
            Text(
                text = stringResource(R.string.tab_settings),
                color = Color.White,
                style = MaterialTheme.typography.displayMedium.copy(fontWeight = FontWeight.Bold),
                modifier = Modifier.padding(
                    start = FillinSpacing.padding16,
                    top = FillinSpacing.padding4,
                    bottom = FillinSpacing.padding16
                )
            )

            // Free Plan hero card.
            FreePlanCard(
                onClick = { navController.navigate(Screen.Paywall.route) },
                modifier = Modifier
                    .padding(horizontal = FillinSpacing.padding16)
                    .padding(bottom = FillinSpacing.padding16)
            )

            // Primary settings group.
            GroupedCardContainer(modifier = Modifier.padding(horizontal = FillinSpacing.padding16)) {
                SettingsRow(
                    icon = Icons.Filled.GraphicEq,
                    title = "Assistant Voice",
                    trailingText = assistantVoice.replaceFirstChar { it.uppercase() },
                    onClick = { showVoiceSheet = true }
                )
                FillinSeparator(inset = 52.dp)
                SettingsRow(
                    icon = Icons.Outlined.Description,
                    title = "Default Form Mode",
                    trailingText = formMode.displayName(),
                    onClick = { showFormModeSheet = true }
                )
                FillinSeparator(inset = 52.dp)
                SettingsRow(
                    icon = Icons.Filled.Language,
                    title = "Language",
                    trailingText = LanguageOption.ALL
                        .firstOrNull { it.tag.equals(activeLocaleTag, ignoreCase = true) }
                        ?.nativeName
                        ?: activeLocaleTag.uppercase(),
                    onClick = { showLanguageSheet = true }
                )
                FillinSeparator(inset = 52.dp)
                SettingsRow(
                    icon = Icons.Filled.Star,
                    title = "Rate us",
                    onClick = { /* in-app review */ }
                )
                FillinSeparator(inset = 52.dp)
                SettingsRow(
                    icon = Icons.Filled.Share,
                    title = "Share with friends",
                    onClick = {
                        val intent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(
                                Intent.EXTRA_TEXT,
                                "Check out FormBuddy — AI-powered form filling: " +
                                    "https://play.google.com/store/apps/details?id=com.formbuddy.android"
                            )
                        }
                        context.startActivity(Intent.createChooser(intent, null))
                    }
                )
            }

            SectionHeader("Help")
            GroupedCardContainer(modifier = Modifier.padding(horizontal = FillinSpacing.padding16)) {
                SettingsRow(
                    icon = Icons.AutoMirrored.Filled.HelpOutline,
                    title = "Contact us",
                    onClick = {
                        context.startActivity(
                            Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:support@formbuddy.ai"))
                        )
                    }
                )
                FillinSeparator(inset = 52.dp)
                SettingsRow(
                    icon = Icons.Filled.PlayArrow,
                    title = "Tutorial — How to use FormBuddy",
                    onClick = {
                        context.startActivity(
                            Intent(Intent.ACTION_VIEW, Uri.parse("https://formbuddy.ai/tutorial"))
                        )
                    }
                )
                FillinSeparator(inset = 52.dp)
                SettingsRow(
                    icon = Icons.Filled.Star,
                    title = "Restore Purchases",
                    onClick = { navController.navigate(Screen.Paywall.route) }
                )
            }

            SectionHeader("Legal")
            GroupedCardContainer(modifier = Modifier.padding(horizontal = FillinSpacing.padding16)) {
                SettingsRow(
                    icon = Icons.Outlined.Gavel,
                    title = "Terms and conditions",
                    onClick = {
                        context.startActivity(
                            Intent(Intent.ACTION_VIEW, Uri.parse("https://formbuddy.ai/terms"))
                        )
                    }
                )
                FillinSeparator(inset = 52.dp)
                SettingsRow(
                    icon = Icons.Outlined.Lock,
                    title = "Privacy policy",
                    onClick = {
                        context.startActivity(
                            Intent(Intent.ACTION_VIEW, Uri.parse("https://formbuddy.ai/privacy"))
                        )
                    }
                )
                FillinSeparator(inset = 52.dp)
                SettingsRow(
                    icon = Icons.Outlined.Shield,
                    title = "Privacy audit log",
                    onClick = { navController.navigate(Screen.PrivacyAudit.route) }
                )
            }

            Spacer(Modifier.height(FillinSpacing.padding24))
            Text(
                text = "App Version ${BuildConfig.VERSION_NAME}(${BuildConfig.VERSION_CODE})",
                color = Color(0xFF6C6C72),
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = FillinSpacing.padding16),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        color = Color(0xFF6C6C72),
        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
        modifier = Modifier.padding(
            start = FillinSpacing.padding16 + FillinSpacing.padding16,
            top = FillinSpacing.padding24,
            bottom = FillinSpacing.padding8
        )
    )
}

@Composable
private fun GroupedCardContainer(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(FillinShapes.default)
            .background(GroupedCard)
    ) { content() }
}

@Composable
private fun FreePlanCard(onClick: () -> Unit, modifier: Modifier = Modifier) {
    FillinPressContainer(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .clip(FillinShapes.default)
            .background(GroupedCard)
            .border(1.dp, FreePlanOutline, FillinShapes.default)
            .padding(horizontal = FillinSpacing.padding16, vertical = FillinSpacing.padding16)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Free Plan",
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
                )
                Text(
                    text = "Upgrade to Pro to get full access to all features",
                    color = Color(0xFF9C9CA1),
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Icon(
                imageVector = Icons.Filled.ChevronRight,
                contentDescription = null,
                tint = ProBadgePurple
            )
        }
    }
}

@Composable
private fun SettingsRow(
    icon: ImageVector,
    title: String,
    trailingText: String? = null,
    onClick: () -> Unit
) {
    FillinPressContainer(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = FillinSpacing.padding16, vertical = FillinSpacing.padding12),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = IMessageBlue,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(Modifier.width(FillinSpacing.padding12))
            Text(
                text = title,
                color = Color.White,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f)
            )
            if (trailingText != null) {
                Text(
                    text = trailingText,
                    color = Color(0xFF9C9CA1),
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(Modifier.width(FillinSpacing.padding4))
            }
            Icon(
                imageVector = Icons.Filled.ChevronRight,
                contentDescription = null,
                tint = Color(0xFF6C6C72),
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

private fun FormMode.displayName(): String = when (this) {
    FormMode.CHAT -> "Chat Mode"
    FormMode.VOICE -> "Voice Mode"
    FormMode.AGENT -> "Agent Mode"
}

private fun FormMode.toSheetOption(): FormModeSheetOption = when (this) {
    FormMode.CHAT -> FormModeSheetOption.Chat
    FormMode.VOICE -> FormModeSheetOption.Voice
    FormMode.AGENT -> FormModeSheetOption.Agent
}

private fun FormModeSheetOption.toFormMode(): FormMode = when (this) {
    FormModeSheetOption.Chat -> FormMode.CHAT
    FormModeSheetOption.Voice -> FormMode.VOICE
    FormModeSheetOption.Agent -> FormMode.AGENT
}
