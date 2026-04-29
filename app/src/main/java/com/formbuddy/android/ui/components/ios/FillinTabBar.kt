package com.formbuddy.android.ui.components.ios

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * iOS-style action-button tab bar — a floating capsule pill on the left
 * containing the three primary tabs (Docs / Profiles / Settings) plus a
 * separate circular FAB on the right.
 *
 * Mirrors `Fillin/UI/Views/ActionButtonTabBar/ActionButtonTabBar.swift` from
 * the iOS app. The Material 3 `NavigationBar` is intentionally NOT used —
 * iOS uses a floating pill instead, and a real 1:1 needs that exact shape.
 *
 * Tapping the FAB rotates "+" into "×" and tells the caller the action menu
 * is expanded. The host overlays the [FillinActionMenu] above the tab bar
 * when expanded.
 */
data class FillinTab(
    val key: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)

@Composable
fun FillinTabBar(
    tabs: List<FillinTab>,
    selectedKey: String,
    isMenuExpanded: Boolean,
    onTabSelected: (String) -> Unit,
    onFabTap: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = FillinSpacing.padding16, vertical = FillinSpacing.padding12),
        horizontalArrangement = Arrangement.spacedBy(FillinSpacing.padding12),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Capsule pill containing the 3 tabs.
        Row(
            modifier = Modifier
                .weight(1f)
                .height(BAR_HEIGHT)
                .clip(FillinShapes.capsule)
                .background(BarBackground),
            verticalAlignment = Alignment.CenterVertically
        ) {
            for (tab in tabs) {
                val selected = tab.key == selectedKey
                FillinPressContainer(
                    onClick = { onTabSelected(tab.key) },
                    modifier = Modifier
                        .weight(1f)
                        .height(BAR_HEIGHT)
                        .padding(4.dp)
                        .clip(FillinShapes.capsule)
                        .background(if (selected) SelectedTabBackground else Color.Transparent)
                ) {
                    val iconTint = if (selected) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    val labelColor = iconTint
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                            contentDescription = tab.label,
                            tint = iconTint,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = tab.label,
                            color = labelColor,
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium
                            )
                        )
                    }
                }
            }
        }

        // Standalone circular FAB.
        val rotation by animateFloatAsState(
            targetValue = if (isMenuExpanded) 45f else 0f,
            animationSpec = spring(stiffness = Spring.StiffnessMedium),
            label = "fab-rotation"
        )
        FillinPressContainer(
            onClick = onFabTap,
            modifier = Modifier
                .size(BAR_HEIGHT)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary)
        ) {
            Icon(
                imageVector = if (isMenuExpanded) Icons.Filled.Close else Icons.Filled.Add,
                contentDescription = if (isMenuExpanded) "Close" else "Add",
                tint = Color.White,
                modifier = Modifier
                    .size(28.dp)
                    .rotate(if (isMenuExpanded) 0f else rotation)
            )
        }
    }
}

/** Bar height matches `ActionButtonTabBar.barHeight` from iOS CoreUI (~52pt). */
val BAR_HEIGHT: Dp = 52.dp

private val BarBackground = Color(0xFF1C1C1E)
private val SelectedTabBackground = Color(0xFF2C2C2E)

/**
 * Arc-style action menu shown when the FAB is expanded — three labelled
 * mini-FABs (Scan / Upload / Library) appearing to the upper-left of the
 * primary FAB. Mirrors iOS `ArcMenu` with the same staggered fade-in.
 */
@Composable
fun FillinActionMenu(
    visible: Boolean,
    onScan: () -> Unit,
    onUpload: () -> Unit,
    onLibrary: () -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(animationSpec = tween(FillinAnimationMs.fast)) +
            scaleIn(initialScale = 0.7f, animationSpec = tween(FillinAnimationMs.fast)),
        exit = fadeOut(animationSpec = tween(FillinAnimationMs.fast)) +
            scaleOut(targetScale = 0.7f, animationSpec = tween(FillinAnimationMs.fast)),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(end = FillinSpacing.padding16, bottom = 80.dp),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.Bottom
        ) {
            ArcMenuItem(label = "Library", onClick = onLibrary)
            Spacer(Modifier.width(FillinSpacing.padding16))
            ArcMenuItem(label = "Upload", onClick = onUpload)
            Spacer(Modifier.width(FillinSpacing.padding16))
            ArcMenuItem(label = "Scan", onClick = onScan)
        }
    }
}

@Composable
private fun ArcMenuItem(label: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(56.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surface),
        contentAlignment = Alignment.Center
    ) {
        FillinPressContainer(
            onClick = onClick,
            modifier = Modifier.size(56.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
