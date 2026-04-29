package com.formbuddy.android.ui.screens.docs

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Search
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.formbuddy.android.data.local.db.entity.FormReferenceEntity
import com.formbuddy.android.data.metrics.formatHuman
import com.formbuddy.android.ui.components.ios.FillinDottedRing
import com.formbuddy.android.ui.components.ios.FillinFilledButton
import com.formbuddy.android.ui.components.ios.FillinLogo
import com.formbuddy.android.ui.components.ios.FillinShapes
import com.formbuddy.android.ui.components.ios.FillinSpacing
import com.formbuddy.android.ui.navigation.Screen
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.time.Duration.Companion.seconds

/**
 * Mirrors iOS `DocsView` end-to-end: pure-black background, top-left logo,
 * inline large title, search bar (when populated), "Your Progress" stats
 * card, and a vertical list of saved forms. Empty state replicates IMG_9039
 * (dashed ring + hero icon + CTA pill).
 *
 * No `Scaffold` here because the host renders the floating tab bar on top
 * of the same root layout — same as iOS `ActionButtonTabBar`.
 */
@Composable
fun DocsScreen(
    navController: NavHostController,
    viewModel: DocsViewModel = hiltViewModel(),
    onAddDocument: () -> Unit = {}
) {
    val forms by viewModel.forms.collectAsState()
    val timeSavedSeconds by viewModel.timeSavedSeconds.collectAsState()
    var query by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .statusBarsPadding()
    ) {
        DocsHeader()

        if (forms.isEmpty()) {
            DocsEmptyState(onAddDocument = onAddDocument)
        } else {
            DocsSearchBar(value = query, onChange = { query = it })
            ProgressStatsCard(
                formsCount = forms.size,
                timeSavedSeconds = timeSavedSeconds
            )
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = FillinSpacing.padding16),
                verticalArrangement = Arrangement.spacedBy(FillinSpacing.padding16),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(
                    bottom = 100.dp // leave room for the floating tab bar
                )
            ) {
                items(
                    items = forms.filter {
                        query.isBlank() || it.documentName.contains(query, ignoreCase = true)
                    },
                    key = { it.id }
                ) { form ->
                    val formId = form.id
                    DocCard(
                        form = form,
                        onClick = { navController.navigate(Screen.Editor.createRoute(formId)) },
                        onDelete = { viewModel.deleteForm(formId) }
                    )
                }
            }
        }
    }
}

@Composable
private fun DocsHeader() {
    Column(modifier = Modifier.padding(horizontal = FillinSpacing.padding16, vertical = FillinSpacing.padding12)) {
        FillinLogo()
        Spacer(Modifier.height(FillinSpacing.padding4))
        Text(
            text = "Docs",
            style = MaterialTheme.typography.displayMedium,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
    }
}

@Composable
private fun DocsEmptyState(onAddDocument: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(bottom = 100.dp), // floating tab bar inset
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            FillinDottedRing(size = 260.dp) {
                Box(
                    modifier = Modifier
                        .size(120.dp)
                        .clip(RoundedCornerShape(28.dp))
                        .background(Color(0xFF111B36)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Description,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(56.dp)
                    )
                }
            }
            Spacer(Modifier.height(FillinSpacing.padding24))
            Text(
                text = "No documents yet",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                color = Color.White
            )
            Spacer(Modifier.height(FillinSpacing.padding8))
            Text(
                text = "Fill out your first form and it will appear here. All your\ncompleted forms are saved automatically.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(FillinSpacing.padding24))
            FillinFilledButton(
                text = "+ Add your first document",
                onClick = onAddDocument,
                modifier = Modifier.padding(horizontal = 50.dp)
            )
        }
    }
}

@Composable
private fun DocsSearchBar(value: String, onChange: (String) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = FillinSpacing.padding16, vertical = FillinSpacing.padding8)
            .height(40.dp)
            .clip(FillinShapes.capsule)
            .background(Color(0xFF1C1C1E))
            .padding(horizontal = FillinSpacing.padding12),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            Icons.Filled.Search,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(18.dp)
        )
        Spacer(Modifier.size(FillinSpacing.padding8))
        androidx.compose.foundation.text.BasicTextField(
            value = value,
            onValueChange = onChange,
            modifier = Modifier.fillMaxWidth(),
            textStyle = MaterialTheme.typography.bodyMedium.copy(color = Color.White),
            cursorBrush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.primary),
            decorationBox = { inner ->
                if (value.isEmpty()) {
                    Text(
                        text = "Search documents",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                inner()
            }
        )
    }
}

@Composable
private fun ProgressStatsCard(formsCount: Int, timeSavedSeconds: Long) {
    val savedHuman = remember(timeSavedSeconds) { timeSavedSeconds.seconds.formatHuman() }
    Column(
        modifier = Modifier
            .padding(horizontal = FillinSpacing.padding16, vertical = FillinSpacing.padding8)
            .fillMaxWidth()
            .clip(FillinShapes.default)
            .background(Color(0xFF1C1C1E))
            .padding(FillinSpacing.padding16)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Filled.AccessTime,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.size(FillinSpacing.padding6))
            Text(
                text = "Your Progress",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = Color.White
            )
        }
        Spacer(Modifier.height(FillinSpacing.padding12))
        Row(horizontalArrangement = Arrangement.spacedBy(FillinSpacing.padding12)) {
            StatTile(
                modifier = Modifier.weight(1f),
                accent = MaterialTheme.colorScheme.primary,
                value = formsCount.toString(),
                label = "Forms Filled",
                icon = Icons.Filled.Description
            )
            StatTile(
                modifier = Modifier.weight(1f),
                accent = Color(0xFF34C759),
                value = savedHuman,
                label = "Time Saved",
                icon = Icons.Filled.AccessTime
            )
        }
    }
}

@Composable
private fun StatTile(
    modifier: Modifier = Modifier,
    accent: Color,
    value: String,
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Row(
        modifier = modifier
            .clip(FillinShapes.default)
            .background(Color(0xFF2C2C2E))
            .padding(FillinSpacing.padding12),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(FillinSpacing.padding8)
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(accent.copy(alpha = 0.18f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(18.dp))
        }
        Column {
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = accent
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun DocCard(
    form: FormReferenceEntity,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()) }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .clip(FillinShapes.default)
            .background(Color(0xFF1C1C1E))
            .padding(FillinSpacing.padding12)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(0.75f)
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFF2C2C2E)),
            contentAlignment = Alignment.Center
        ) {
            if (form.thumbnailFilePath != null) {
                AsyncImage(
                    model = ImageRequest.Builder(androidx.compose.ui.platform.LocalContext.current)
                        .data(File(form.thumbnailFilePath))
                        .memoryCachePolicy(CachePolicy.ENABLED)
                        .diskCachePolicy(CachePolicy.ENABLED)
                        .crossfade(150)
                        .build(),
                    contentDescription = form.documentName,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Icon(
                    Icons.Filled.Description,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                )
            }
        }
        Spacer(Modifier.height(FillinSpacing.padding8))
        Text(
            text = form.documentName,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = Color.White,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = dateFormat.format(Date(form.modifiedAt)),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
