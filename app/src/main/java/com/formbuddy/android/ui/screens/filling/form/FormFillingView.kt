package com.formbuddy.android.ui.screens.filling.form

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
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.CheckBoxOutlineBlank
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
    onFieldSelected: (String) -> Unit = {}
) {
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
                    onSelected = { onFieldSelected(field.id) },
                    onValueChange = { newValue ->
                        field.userValue = newValue
                        if (field.userInputMethod == null) {
                            field.userInputMethod = null // manual edit
                        }
                        viewModel.notifyFieldChanged()
                    },
                    onCheckboxToggle = { type ->
                        field.style.checkboxType = type
                        field.userInputMethod = null
                        viewModel.notifyFieldChanged()
                    }
                )
            }
        }
    }
}

@Composable
private fun FieldRow(
    field: FormField,
    isSelected: Boolean,
    onSelected: () -> Unit,
    onValueChange: (String) -> Unit,
    onCheckboxToggle: (CheckboxType) -> Unit
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
                if (field.userInputMethod == UserInputMethod.PROFILE_AUTOFILL) {
                    Text(
                        text = " · auto-filled",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
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
