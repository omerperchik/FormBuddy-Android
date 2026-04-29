package com.formbuddy.android.ui.screens.library

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import com.formbuddy.android.data.repository.LibraryFormReference
import com.formbuddy.android.ui.components.ios.FillinPressContainer
import com.formbuddy.android.ui.components.ios.FillinSeparator
import com.formbuddy.android.ui.components.ios.FillinShapes
import com.formbuddy.android.ui.components.ios.FillinSpacing
import com.formbuddy.android.ui.navigation.Screen
import com.formbuddy.android.ui.theme.IMessageBlue

/**
 * iOS-matching Forms Library (IMG_9055).
 *
 * Layout:
 *   - Pure-black sheet, status-bar inset.
 *   - Top-right circular X close button.
 *   - Bold "Forms Library" title (left-aligned, ~32 sp).
 *   - Pill-shaped search field below the title.
 *   - Single-column list of rows: small thumbnail, document name in white,
 *     `AGENCY · domain · LANG · N pages` meta line in gray, file size on
 *     the next line.
 */
@Composable
fun FormsLibraryScreen(
    navController: NavHostController,
    viewModel: FormsLibraryViewModel = hiltViewModel()
) {
    val forms by viewModel.forms.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    var searchQuery by remember { mutableStateOf("") }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        Column(modifier = Modifier.fillMaxSize().statusBarsPadding()) {
            // Top-right X close.
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = FillinSpacing.padding16, vertical = FillinSpacing.padding8),
                horizontalArrangement = Arrangement.End
            ) {
                FillinPressContainer(
                    onClick = { navController.popBackStack() },
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF1F1F1F))
                ) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = "Close",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Text(
                text = "Forms Library",
                color = Color.White,
                style = MaterialTheme.typography.displayMedium.copy(fontWeight = FontWeight.Bold),
                modifier = Modifier.padding(
                    start = FillinSpacing.padding16,
                    end = FillinSpacing.padding16,
                    bottom = FillinSpacing.padding16
                )
            )

            // iOS-style pill search field.
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = FillinSpacing.padding16, vertical = FillinSpacing.padding4)
                    .height(40.dp)
                    .clip(FillinShapes.capsule)
                    .background(Color(0xFF1C1C1E))
                    .padding(horizontal = FillinSpacing.padding12)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxSize()) {
                    Icon(
                        Icons.Filled.Search,
                        contentDescription = null,
                        tint = Color(0xFF8E8E93),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(FillinSpacing.padding8))
                    Box(modifier = Modifier.weight(1f)) {
                        if (searchQuery.isEmpty()) {
                            Text(
                                text = "Search forms",
                                color = Color(0xFF8E8E93),
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                        BasicTextField(
                            value = searchQuery,
                            onValueChange = {
                                searchQuery = it
                                viewModel.search(it)
                            },
                            singleLine = true,
                            cursorBrush = SolidColor(IMessageBlue),
                            textStyle = TextStyle(
                                color = Color.White,
                                fontSize = MaterialTheme.typography.bodyLarge.fontSize
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            Spacer(Modifier.height(FillinSpacing.padding12))

            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = IMessageBlue)
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = FillinSpacing.padding16)
                        .clip(FillinShapes.default)
                        .background(Color(0xFF1C1C1E)),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    items(forms, key = { it.id }) { form ->
                        LibraryFormRow(
                            form = form,
                            onClick = {
                                viewModel.downloadAndOpen(form) { uri ->
                                    navController.navigate(
                                        Screen.Filling.createRoute("library", uri = uri)
                                    )
                                }
                            }
                        )
                        FillinSeparator(inset = 80.dp)
                    }
                }
            }
        }
    }
}

@Composable
private fun LibraryFormRow(
    form: LibraryFormReference,
    onClick: () -> Unit
) {
    FillinPressContainer(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = FillinSpacing.padding12, vertical = FillinSpacing.padding12),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Thumbnail tile.
            Box(
                modifier = Modifier
                    .size(width = 56.dp, height = 56.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color.White)
            ) {
                if (!form.thumbnailUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = form.thumbnailUrl,
                        contentDescription = form.documentName,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(
                        Icons.Filled.Description,
                        contentDescription = null,
                        tint = Color.Gray,
                        modifier = Modifier
                            .padding(12.dp)
                            .fillMaxSize()
                    )
                }
            }
            Spacer(Modifier.width(FillinSpacing.padding12))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = form.documentName,
                    color = Color.White,
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = buildString {
                        append(form.agency)
                        append("  ·  ")
                        append(form.language.uppercase())
                        if (form.pagesCount > 0) {
                            append("  ·  ${form.pagesCount} page" + if (form.pagesCount != 1) "s" else "")
                        }
                    },
                    color = Color(0xFF8E8E93),
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (form.bytes > 0) {
                    Text(
                        text = humanBytes(form.bytes),
                        color = Color(0xFF8E8E93),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}

private fun humanBytes(bytes: Long): String = when {
    bytes >= 1_048_576 -> "%.1f MB".format(bytes / 1_048_576f)
    bytes >= 1024 -> "${bytes / 1024} kB"
    else -> "$bytes B"
}
