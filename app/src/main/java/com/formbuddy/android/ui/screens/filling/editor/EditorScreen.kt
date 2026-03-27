package com.formbuddy.android.ui.screens.filling.editor

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.formbuddy.android.R
import com.formbuddy.android.data.model.FieldType
import com.formbuddy.android.data.model.FormField
import com.formbuddy.android.ui.components.FieldTypeBadge
import com.formbuddy.android.ui.components.ZoomableBox
import com.formbuddy.android.ui.components.fieldTypeIconAndColor

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(
    navController: NavHostController,
    formId: String,
    viewModel: EditorViewModel = hiltViewModel()
) {
    val formTemplate by viewModel.formTemplate.collectAsState()
    val pageBitmaps by viewModel.pageBitmaps.collectAsState()
    val isEditing by viewModel.isEditing.collectAsState()
    var selectedPageIndex by remember { mutableIntStateOf(0) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var editingField by remember { mutableStateOf<FormField?>(null) }
    val context = LocalContext.current

    LaunchedEffect(formId) {
        viewModel.loadForm(formId)
    }

    val template = formTemplate ?: return

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = template.documentName,
                        modifier = Modifier.clickable { showRenameDialog = true }
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.action_back))
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.toggleEditing() }) {
                        Icon(
                            Icons.Filled.Edit,
                            contentDescription = stringResource(R.string.action_edit),
                            tint = if (isEditing) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
                    }
                    IconButton(onClick = { showDeleteDialog = true }) {
                        Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.action_delete))
                    }
                }
            )
        },
        bottomBar = {
            BottomAppBar {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    IconButton(onClick = {
                        viewModel.saveForm()
                    }) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Filled.Save, contentDescription = null)
                            Text(stringResource(R.string.action_save), style = MaterialTheme.typography.labelSmall)
                        }
                    }
                    IconButton(onClick = {
                        viewModel.exportPdf { file ->
                            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                            val intent = Intent(Intent.ACTION_SEND).apply {
                                type = "application/pdf"
                                putExtra(Intent.EXTRA_STREAM, uri)
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }
                            context.startActivity(Intent.createChooser(intent, null))
                        }
                    }) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Filled.Share, contentDescription = null)
                            Text(stringResource(R.string.action_share), style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Page thumbnails
            if (template.pages.size > 1) {
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    itemsIndexed(pageBitmaps) { index, bitmap ->
                        Card(
                            modifier = Modifier
                                .width(60.dp)
                                .clickable { selectedPageIndex = index },
                            border = if (index == selectedPageIndex)
                                CardDefaults.outlinedCardBorder()
                            else null,
                            elevation = CardDefaults.cardElevation(
                                defaultElevation = if (index == selectedPageIndex) 4.dp else 1.dp
                            )
                        ) {
                            if (bitmap != null) {
                                androidx.compose.foundation.Image(
                                    bitmap = bitmap.asImageBitmap(),
                                    contentDescription = "Page ${index + 1}",
                                    modifier = Modifier.aspectRatio(0.75f)
                                )
                            }
                        }
                    }
                }
            }

            // Document view with field overlays
            ZoomableBox(modifier = Modifier.weight(1f)) {
                Box(modifier = Modifier.fillMaxSize()) {
                    var canvasSize by remember { mutableStateOf(IntSize.Zero) }

                    val currentBitmap = pageBitmaps.getOrNull(selectedPageIndex)
                    if (currentBitmap != null) {
                        androidx.compose.foundation.Image(
                            bitmap = currentBitmap.asImageBitmap(),
                            contentDescription = null,
                            modifier = Modifier
                                .fillMaxWidth()
                                .onSizeChanged { canvasSize = it }
                        )
                    }

                    // Field overlays
                    val pageFields = template.pages.getOrNull(selectedPageIndex)?.fields ?: emptyList()
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        pageFields.forEach { field ->
                            val rect = field.boundingBox.scaledTo(
                                canvasSize.width.toFloat(),
                                canvasSize.height.toFloat()
                            )
                            val (_, color) = fieldTypeIconAndColor(field.fieldType)
                            drawRect(
                                color = color.copy(alpha = 0.15f),
                                topLeft = Offset(rect.left, rect.top),
                                size = Size(rect.width(), rect.height())
                            )
                            drawRect(
                                color = color.copy(alpha = 0.6f),
                                topLeft = Offset(rect.left, rect.top),
                                size = Size(rect.width(), rect.height()),
                                style = Stroke(width = 1.5f)
                            )
                        }
                    }

                    // Clickable field areas for editing
                    if (isEditing) {
                        pageFields.forEach { field ->
                            val rect = field.boundingBox.scaledTo(
                                canvasSize.width.toFloat(),
                                canvasSize.height.toFloat()
                            )
                            Box(
                                modifier = Modifier
                                    .padding(
                                        start = rect.left.dp,
                                        top = rect.top.dp
                                    )
                                    .clickable { editingField = field }
                            )
                        }
                    }
                }
            }
        }
    }

    // Rename dialog
    if (showRenameDialog) {
        var name by remember { mutableStateOf(template.documentName) }
        AlertDialog(
            onDismissRequest = { showRenameDialog = false },
            title = { Text(stringResource(R.string.editor_rename)) },
            text = {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.editor_document_name)) }
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.renameDocument(name)
                    showRenameDialog = false
                }) {
                    Text(stringResource(R.string.action_save))
                }
            },
            dismissButton = {
                TextButton(onClick = { showRenameDialog = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }

    // Delete dialog
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text(stringResource(R.string.editor_delete_title)) },
            text = { Text(stringResource(R.string.editor_delete_message)) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteForm()
                    showDeleteDialog = false
                    navController.popBackStack()
                }) {
                    Text(stringResource(R.string.action_delete), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }

    // Field editing dialog
    editingField?.let { field ->
        var value by remember { mutableStateOf(field.displayValue) }
        AlertDialog(
            onDismissRequest = { editingField = null },
            title = { Text(field.label.ifBlank { field.fieldType.name }) },
            text = {
                OutlinedTextField(
                    value = value,
                    onValueChange = { value = it },
                    label = { Text(stringResource(R.string.editor_field_value)) }
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.updateFieldValue(field.id, value)
                    editingField = null
                }) {
                    Text(stringResource(R.string.action_save))
                }
            },
            dismissButton = {
                TextButton(onClick = { editingField = null }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }
}
