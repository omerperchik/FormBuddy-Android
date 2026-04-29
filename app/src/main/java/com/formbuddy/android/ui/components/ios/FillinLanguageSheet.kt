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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
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
import java.util.Locale

/**
 * iOS-matching language picker (sibling of [DefaultFormModeSheet]).
 *
 * Lists the 12 locales the iOS app ships with. Each row shows the locale's
 * native display name (left) and English name (right, gray); the currently
 * selected locale gets a leading blue check.
 *
 * Tapping a row calls [onSelect] with the BCP-47 tag (e.g. `pt-BR`) which
 * the host should hand to `AppCompatDelegate.setApplicationLocales(...)`.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LanguageSheet(
    selectedTag: String?,
    onSelect: (String) -> Unit,
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
                .padding(horizontal = FillinSpacing.padding16)
        ) {
            Text(
                text = "Language",
                color = Color.White,
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = FillinSpacing.padding8)
            )
            Text(
                text = "FormBuddy already speaks all 12. Pick the one you want.",
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
                items(LanguageOption.ALL, key = { it.tag }) { option ->
                    LanguageRow(
                        option = option,
                        isSelected = option.tag == selectedTag,
                        onClick = { onSelect(option.tag) }
                    )
                    if (option != LanguageOption.ALL.last()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = FillinSpacing.padding16)
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
private fun LanguageRow(
    option: LanguageOption,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    FillinPressContainer(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = FillinSpacing.padding16, vertical = FillinSpacing.padding14),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(modifier = Modifier.size(20.dp), contentAlignment = Alignment.Center) {
                if (isSelected) {
                    Icon(
                        Icons.Filled.Check,
                        contentDescription = "Selected",
                        tint = IMessageBlue,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            Spacer(Modifier.width(FillinSpacing.padding12))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = option.nativeName,
                    color = Color.White,
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold)
                )
                if (option.englishName != option.nativeName) {
                    Text(
                        text = option.englishName,
                        color = Color(0xFF9C9CA1),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}

private val FillinSpacing.padding14 get() = 14.dp

/** The 12 locales iOS ships, with native + English display names. */
data class LanguageOption(
    val tag: String,
    val nativeName: String,
    val englishName: String,
    val locale: Locale
) {
    companion object {
        val ALL: List<LanguageOption> = listOf(
            LanguageOption("en",    "English",    "English",            Locale("en")),
            LanguageOption("he",    "עברית",      "Hebrew",             Locale("he")),
            LanguageOption("es",    "Español",    "Spanish",            Locale("es")),
            LanguageOption("fr",    "Français",   "French",             Locale("fr")),
            LanguageOption("de",    "Deutsch",    "German",             Locale("de")),
            LanguageOption("pt-BR", "Português (Brasil)",   "Portuguese (Brazil)",   Locale("pt", "BR")),
            LanguageOption("pt-PT", "Português (Portugal)", "Portuguese (Portugal)", Locale("pt", "PT")),
            LanguageOption("ru",    "Русский",    "Russian",            Locale("ru")),
            LanguageOption("ja",    "日本語",      "Japanese",           Locale("ja")),
            LanguageOption("zh-CN", "简体中文",    "Chinese (Simplified)", Locale("zh", "CN")),
            LanguageOption("ar",    "العربية",    "Arabic",             Locale("ar")),
            LanguageOption("hi",    "हिन्दी",     "Hindi",              Locale("hi"))
        )
    }
}
