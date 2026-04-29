package com.formbuddy.android.ui.screens.filling.form

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDropUp
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.CheckBoxOutlineBlank
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.formbuddy.android.R
import com.formbuddy.android.data.model.CheckboxType
import com.formbuddy.android.data.model.FieldType
import com.formbuddy.android.data.model.FormField
import com.formbuddy.android.data.model.FormPage
import com.formbuddy.android.data.model.FormTemplate
import com.formbuddy.android.data.model.UserInputMethod
import com.formbuddy.android.ui.theme.AddressFieldColor
import com.formbuddy.android.ui.theme.CheckboxFieldColor
import com.formbuddy.android.ui.theme.DateFieldColor
import com.formbuddy.android.ui.theme.EmailFieldColor
import com.formbuddy.android.ui.theme.NumberFieldColor
import com.formbuddy.android.ui.theme.PhoneFieldColor
import com.formbuddy.android.ui.theme.SignatureFieldColor
import com.formbuddy.android.ui.theme.TextFieldColor
import com.formbuddy.android.ui.screens.filling.FillingViewModel

/**
 * Mirrors iOS `FormFillingView` — renders the form as a flat scrolling list grouped
 * by page, with a per-type editor for each field. Editing flows immediately back into
 * the [FillingViewModel] so other tabs (Document, Chat) stay in sync.
 */
@Composable
fun FormFillingView(
    template: FormTemplate,
    viewModel: FillingViewModel,
    selectedFieldId: String? = null,
    onFieldSelected: (String) -> Unit = {},
    editorMode: Boolean = false
) {
    // Read the system clipboard once when the view enters composition.
    val context = androidx.compose.ui.platform.LocalContext.current
    val clipboardText = remember { com.formbuddy.android.util.ClipboardSnapshot.read(context) }
    val clipboardSuggestion: Pair<FormField, String>? = remember(clipboardText, template) {
        if (clipboardText == null) null
        else template.allFields.firstOrNull { f ->
            f.isEmpty && com.formbuddy.android.util.ClipboardSnapshot.matches(clipboardText, f)
        }?.let { it to clipboardText }
    }
    var dismissedClipboard by remember(clipboardText) { mutableStateOf(false) }
    if (template.allFields.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                stringResource(R.string.form_no_fields),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        return
    }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Clipboard fall-through — top-of-list chip when an address-/email-/
        // phone-shaped snippet is on the clipboard and there's a matching empty
        // field on the form. Tap to fill, X to dismiss.
        clipboardSuggestion?.let { suggestion ->
            val target = suggestion.first
            val text = suggestion.second
            if (!dismissedClipboard) {
                item(key = "clipboard-chip") {
                    ClipboardChip(
                        snippet = text,
                        targetLabel = target.label.ifBlank { target.fieldType.name },
                        onUse = {
                            target.userValue = text
                            target.userInputMethod = UserInputMethod.ACCEPTED_SUGGESTION
                            viewModel.notifyFieldChanged()
                            viewModel.recordManualEdit(target, text)
                            dismissedClipboard = true
                        },
                        onDismiss = { dismissedClipboard = true }
                    )
                }
            }
        }

        template.pages.forEach { page ->
            item(key = "page-${page.index}") {
                Text(
                    text = stringResource(R.string.form_page_label, page.index + 1),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
            items(page.fields, key = { it.id }) { field ->
                FieldRow(
                    field = field,
                    isSelected = field.id == selectedFieldId,
                    editorMode = editorMode,
                    onSelected = { onFieldSelected(field.id) },
                    onValueChange = { newValue ->
                        field.userValue = newValue
                        viewModel.notifyFieldChanged()
                        viewModel.recordManualEdit(field, newValue)
                    },
                    onCheckboxToggle = { type ->
                        field.style.checkboxType = type
                        field.userInputMethod = null
                        viewModel.notifyFieldChanged()
                    },
                    onMoveUp = { viewModel.moveField(field, -1) },
                    onMoveDown = { viewModel.moveField(field, +1) },
                    onRemove = { viewModel.removeField(field) }
                )
            }
        }
    }
}

@Composable
private fun FieldRow(
    field: FormField,
    isSelected: Boolean,
    editorMode: Boolean,
    onSelected: () -> Unit,
    onValueChange: (String) -> Unit,
    onCheckboxToggle: (CheckboxType) -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onRemove: () -> Unit
) {
    val accent = field.fieldType.color()
    Card(
        onClick = onSelected,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected)
                MaterialTheme.colorScheme.primaryContainer
            else
                MaterialTheme.colorScheme.surface
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                ) {
                    androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
                        drawCircle(accent)
                    }
                }
                Text(
                    text = field.label.ifBlank { field.fieldType.name.lowercase().replaceFirstChar { it.uppercase() } },
                    modifier = Modifier.padding(start = 8.dp),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                ProvenanceBadge(field.userInputMethod, field.userValue.isNullOrBlank())
                if (editorMode) {
                    androidx.compose.foundation.layout.Spacer(Modifier.weight(1f))
                    IconButton(onClick = onMoveUp) {
                        Icon(Icons.Filled.ArrowDropUp, contentDescription = "Move up")
                    }
                    IconButton(onClick = onMoveDown) {
                        Icon(Icons.Filled.ArrowDropDown, contentDescription = "Move down")
                    }
                    if (field.isUserGenerated) {
                        IconButton(onClick = onRemove) {
                            Icon(Icons.Filled.Close, contentDescription = "Remove")
                        }
                    }
                }
            }

            when (field.fieldType) {
                FieldType.CHECKBOX -> {
                    val isChecked = field.style.checkboxType != CheckboxType.NONE
                    IconButton(onClick = {
                        onCheckboxToggle(if (isChecked) CheckboxType.NONE else CheckboxType.CHECKMARK)
                    }) {
                        Icon(
                            imageVector = if (isChecked) Icons.Filled.CheckBox else Icons.Filled.CheckBoxOutlineBlank,
                            contentDescription = null,
                            tint = if (isChecked) accent else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                FieldType.SIGNATURE -> {
                    Text(
                        text = if (field.userValue.isNullOrBlank())
                            "Tap to add a signature"
                        else
                            "Signature captured",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(top = 8.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                else -> {
                    OutlinedTextField(
                        value = field.displayValue,
                        onValueChange = onValueChange,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        placeholder = { Text(field.label) },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = when (field.fieldType) {
                                FieldType.NUMBER -> KeyboardType.Number
                                FieldType.EMAIL -> KeyboardType.Email
                                FieldType.PHONE -> KeyboardType.Phone
                                FieldType.DATE -> KeyboardType.Number
                                else -> KeyboardType.Text
                            },
                            imeAction = ImeAction.Next
                        ),
                        singleLine = field.fieldType !in setOf(FieldType.TEXT, FieldType.ADDRESS)
                    )
                }
            }
        }
    }
}

@Composable
private fun ClipboardChip(
    snippet: String,
    targetLabel: String,
    onUse: () -> Unit,
    onDismiss: () -> Unit
) {
    val preview = if (snippet.length > 32) snippet.take(32) + "…" else snippet
    androidx.compose.foundation.layout.Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Color(0xFF1183FE).copy(alpha = 0.16f),
                androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
            )
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = androidx.compose.material.icons.Icons.Filled.ContentPaste,
            contentDescription = null,
            tint = Color(0xFF1183FE)
        )
        androidx.compose.foundation.layout.Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Paste \"$preview\" into $targetLabel?",
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFFEBEBF5)
            )
            Text(
                text = "From your clipboard",
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFF9C9CA1)
            )
        }
        androidx.compose.material3.TextButton(onClick = onUse) {
            Text("Use", color = Color(0xFF1183FE))
        }
        IconButton(onClick = onDismiss) {
            Icon(
                imageVector = androidx.compose.material.icons.Icons.Filled.Close,
                contentDescription = "Dismiss",
                tint = Color(0xFF9C9CA1)
            )
        }
    }
}

private fun FieldType.color(): Color = when (this) {
    FieldType.TEXT -> TextFieldColor
    FieldType.NUMBER -> NumberFieldColor
    FieldType.EMAIL -> EmailFieldColor
    FieldType.PHONE -> PhoneFieldColor
    FieldType.CHECKBOX -> CheckboxFieldColor
    FieldType.DATE -> DateFieldColor
    FieldType.SIGNATURE -> SignatureFieldColor
    FieldType.ADDRESS -> AddressFieldColor
}

/**
 * Diff'd-review chip — surfaces *where* a value came from so the user can
 * scan the form by trust level instead of by field-by-field correctness:
 *   - profile  → gray "Profile" pill (auto-filled from saved profile)
 *   - voice    → blue "Voice" pill (filled by speaking)
 *   - chat     → blue "Suggested" pill (accepted suggestion)
 *   - manual   → no chip (user typed it themselves; the cleanest case)
 *   - empty    → orange "Needs you" pill so empty required fields stick out
 */
@Composable
private fun ProvenanceBadge(method: UserInputMethod?, isEmpty: Boolean) {
    val (label, fg, bg) = when {
        isEmpty && method == null -> Triple("Needs you", Color(0xFFFF9500), Color(0xFFFF9500).copy(alpha = 0.16f))
        method == UserInputMethod.PROFILE_AUTOFILL -> Triple("Profile", Color(0xFF9C9CA1), Color(0xFF2C2C2E))
        method == UserInputMethod.VOICE_FILLED -> Triple("Voice", Color(0xFF1183FE), Color(0xFF1183FE).copy(alpha = 0.16f))
        method == UserInputMethod.ACCEPTED_SUGGESTION -> Triple("Suggested", Color(0xFF1183FE), Color(0xFF1183FE).copy(alpha = 0.16f))
        else -> return // manual fill — no chip, "trustworthy by default"
    }
    androidx.compose.foundation.layout.Box(
        modifier = Modifier
            .padding(start = 6.dp)
            .background(bg, androidx.compose.foundation.shape.RoundedCornerShape(6.dp))
            .padding(horizontal = 6.dp, vertical = 1.dp)
    ) {
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = fg)
    }
}
