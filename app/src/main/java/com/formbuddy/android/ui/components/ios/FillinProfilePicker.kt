package com.formbuddy.android.ui.components.ios

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.formbuddy.android.ui.theme.ProBadgePurple

/**
 * iOS-style profile-section picker — three pills with optional "PRO" badges.
 *
 * Mirrors `Fillin/UI/Views/Profile/ProfileSectionPicker` (the Personal /
 * Family / Business segmented control). The selected pill fills with the
 * accent blue and shows white text + icon. Unselected pills are dark gray.
 *
 * The "PRO" badge is rendered as a small purple pill INSIDE the segment
 * label, placed after the text — exactly as in IMG_9042 / IMG_9043.
 */
data class FillinProfileSection(
    val key: String,
    val title: String,
    val icon: ImageVector,
    val isPro: Boolean = false
)

object FillinProfileSections {
    val Personal = FillinProfileSection("personal", "Personal", Icons.Filled.Person, isPro = false)
    val Family = FillinProfileSection("family", "Family", Icons.Filled.Group, isPro = true)
    val Business = FillinProfileSection("business", "Business", Icons.Filled.Business, isPro = true)
    val all = listOf(Personal, Family, Business)
}

@Composable
fun FillinProfilePicker(
    sections: List<FillinProfileSection>,
    selectedKey: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(scrollState)
            .padding(horizontal = FillinSpacing.padding16, vertical = FillinSpacing.padding8),
        horizontalArrangement = Arrangement.spacedBy(FillinSpacing.padding8),
        verticalAlignment = Alignment.CenterVertically
    ) {
        for (section in sections) {
            val selected = section.key == selectedKey
            FillinPressContainer(
                onClick = { onSelect(section.key) },
                modifier = Modifier
                    .height(36.dp)
                    .clip(FillinShapes.capsule)
                    .background(if (selected) MaterialTheme.colorScheme.primary else Color(0xFF1C1C1E))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = FillinSpacing.padding12),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = section.icon,
                        contentDescription = null,
                        tint = if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(FillinSpacing.padding6))
                    Text(
                        text = section.title,
                        color = if (selected) Color.White else MaterialTheme.colorScheme.onSurface,
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold)
                    )
                    if (section.isPro) {
                        Spacer(Modifier.width(FillinSpacing.padding6))
                        ProPill()
                    }
                }
            }
        }
    }
}

/** Inline "PRO" purple pill used inside picker segments and inside form-mode rows. */
@Composable
fun ProPill(modifier: Modifier = Modifier) {
    Text(
        text = "PRO",
        color = Color.White,
        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
        modifier = modifier
            .wrapContentSize()
            .clip(RoundedCornerShape(4.dp))
            .background(ProBadgePurple)
            .padding(horizontal = 6.dp, vertical = 1.dp)
    )
}
