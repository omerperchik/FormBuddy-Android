package com.formbuddy.android.ui.screens.profiles

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.formbuddy.android.R
import com.formbuddy.android.data.local.db.entity.ProfileEntity
import com.formbuddy.android.ui.components.BiometricGate
import com.formbuddy.android.ui.components.ios.FillinPressContainer
import com.formbuddy.android.ui.components.ios.FillinProfileCompletionCard
import com.formbuddy.android.ui.components.ios.FillinSection
import com.formbuddy.android.ui.components.ios.FillinSeparator
import com.formbuddy.android.ui.components.ios.FillinSpacing
import com.formbuddy.android.ui.components.ios.FillinTopBar
import com.formbuddy.android.ui.components.ios.ProPill
import com.formbuddy.android.ui.components.pathToSignatureStrokes
import com.formbuddy.android.ui.navigation.Screen
import com.formbuddy.android.ui.theme.IMessageBlue

/**
 * iOS-matching single-profile editor (Personal / Family).
 *
 * Mirrors IMG_9042 / IMG_9043 / IMG_9044:
 *   - [FillinTopBar] with chevron-left back + accent action.
 *   - [FillinProfileCompletionCard] at the top when under 100%.
 *   - Grouped sections: Personal Info / Addresses / Signature / Profile Info.
 *   - Each row is a label-on-top, value-on-bottom inline text field; rows
 *     are separated by hairline [FillinSeparator]s inset 16 dp from the
 *     leading edge.
 *   - Privacy lock toggle uses a Material `Switch` (closest to iOS look).
 */
@Composable
fun ProfileScreen(
    navController: NavHostController,
    profileId: String,
    isFamily: Boolean,
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val profile by viewModel.profile.collectAsState()

    LaunchedEffect(profileId) { viewModel.loadProfile(profileId) }

    val p = profile ?: return

    val title = if (isFamily) stringResource(R.string.profile_family) else stringResource(R.string.profile_personal)
    val gateSubtitle = stringResource(R.string.profile_biometric_description)
    val gateCancel = stringResource(R.string.action_cancel)

    BiometricGate(
        enabled = p.isPrivate,
        title = title,
        subtitle = gateSubtitle,
        cancelLabel = gateCancel,
        onCancel = { navController.popBackStack() }
    ) {
        Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
            Column(modifier = Modifier.fillMaxSize()) {
                FillinTopBar(
                    title = title,
                    onBack = { navController.popBackStack() },
                    backLabel = "Back",
                    trailing = {
                        if (isFamily) {
                            FillinPressContainer(
                                onClick = {
                                    viewModel.deleteProfile()
                                    navController.popBackStack()
                                },
                                modifier = Modifier.padding(horizontal = FillinSpacing.padding4)
                            ) {
                                androidx.compose.material3.Icon(
                                    Icons.Filled.Delete,
                                    contentDescription = "Delete",
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                )

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(bottom = 120.dp),
                    verticalArrangement = Arrangement.spacedBy(0.dp)
                ) {
                    // Profile-completion hero card (only when not a Pro family profile in iOS).
                    if (!isFamily) {
                        Box(modifier = Modifier.padding(horizontal = FillinSpacing.padding16, vertical = FillinSpacing.padding16)) {
                            FillinProfileCompletionCard(
                                percent = p.completionPercentage,
                                hintText = if (p.completionPercentage >= 1f)
                                    "Profile complete. We'll keep autofilling for you."
                                else
                                    "Almost there! Just a few more fields.",
                                nextFieldLabel = nextMissingFieldLabel(p),
                                onNextFieldClick = { /* iOS scrolls to first missing field */ }
                            )
                        }
                    }

                    FillinSection(title = "Personal Info") {
                        ProfileFieldRow(
                            label = stringResource(R.string.field_first_name),
                            value = p.firstName,
                            onChange = { viewModel.updateField { copy(firstName = it) } }
                        )
                        FillinSeparator()
                        ProfileFieldRow(
                            label = stringResource(R.string.field_middle_name),
                            value = p.middleName.orEmpty(),
                            onChange = { viewModel.updateField { copy(middleName = it.ifBlank { null }) } }
                        )
                        FillinSeparator()
                        ProfileFieldRow(
                            label = stringResource(R.string.field_last_name),
                            value = p.lastName,
                            onChange = { viewModel.updateField { copy(lastName = it) } }
                        )
                        FillinSeparator()
                        ProfileFieldRow(
                            label = stringResource(R.string.field_email),
                            value = p.email.orEmpty(),
                            keyboardType = KeyboardType.Email,
                            onChange = { viewModel.updateField { copy(email = it.ifBlank { null }) } }
                        )
                        FillinSeparator()
                        ProfileFieldRow(
                            label = stringResource(R.string.field_phone),
                            value = p.phone.orEmpty(),
                            keyboardType = KeyboardType.Phone,
                            onChange = { viewModel.updateField { copy(phone = it.ifBlank { null }) } }
                        )
                        FillinSeparator()
                        ProfileFieldRow(
                            label = stringResource(R.string.field_birth_date),
                            value = p.birthDate.orEmpty(),
                            trailing = {
                                if (!p.birthDate.isNullOrBlank()) {
                                    DateChip(text = p.birthDate!!)
                                }
                            },
                            onChange = { viewModel.updateField { copy(birthDate = it.ifBlank { null }) } }
                        )
                    }

                    FillinSection(title = "Addresses") {
                        ProfileFieldRow(
                            label = stringResource(R.string.field_home_address),
                            value = p.homeAddress.orEmpty(),
                            onChange = { viewModel.updateField { copy(homeAddress = it.ifBlank { null }) } }
                        )
                        FillinSeparator()
                        ProfileFieldRow(
                            label = stringResource(R.string.field_city),
                            value = p.city.orEmpty(),
                            onChange = { viewModel.updateField { copy(city = it.ifBlank { null }) } }
                        )
                        FillinSeparator()
                        ProfileFieldRow(
                            label = stringResource(R.string.field_state),
                            value = p.state.orEmpty(),
                            onChange = { viewModel.updateField { copy(state = it.ifBlank { null }) } }
                        )
                        FillinSeparator()
                        ProfileFieldRow(
                            label = stringResource(R.string.field_country),
                            value = p.country.orEmpty(),
                            onChange = { viewModel.updateField { copy(country = it.ifBlank { null }) } }
                        )
                        FillinSeparator()
                        ProfileFieldRow(
                            label = stringResource(R.string.field_postal_code),
                            value = p.postalCode.orEmpty(),
                            keyboardType = KeyboardType.Number,
                            onChange = { viewModel.updateField { copy(postalCode = it.ifBlank { null }) } }
                        )
                        FillinSeparator()
                        ProfileFieldRow(
                            label = stringResource(R.string.field_work_address),
                            value = p.workAddress.orEmpty(),
                            onChange = { viewModel.updateField { copy(workAddress = it.ifBlank { null }) } }
                        )
                    }

                    FillinSection(title = "Signature") {
                        FillinPressContainer(
                            onClick = { navController.navigate(Screen.Signature.createRoute(profileId)) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = FillinSpacing.padding16, vertical = FillinSpacing.padding16),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Signature",
                                    color = Color.White,
                                    style = MaterialTheme.typography.bodyLarge,
                                    modifier = Modifier.weight(1f)
                                )
                                if (!p.signaturePath.isNullOrBlank()) {
                                    Text(
                                        text = "Saved",
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    Spacer(Modifier.padding(start = FillinSpacing.padding4))
                                }
                                androidx.compose.material3.Icon(
                                    Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                    contentDescription = null,
                                    tint = Color(0xFF6C6C72)
                                )
                            }
                        }
                    }

                    FillinSection(title = "Profile Info") {
                        // Profile name row
                        ProfileFieldRow(
                            label = "Profile Name",
                            value = p.firstName,
                            onChange = { /* read-only — iOS shows "Omer" */ }
                        )
                        FillinSeparator()
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = FillinSpacing.padding16, vertical = FillinSpacing.padding12),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "Lock profile",
                                        color = Color.White,
                                        style = MaterialTheme.typography.bodyLarge
                                    )
                                    Spacer(Modifier.padding(start = FillinSpacing.padding6))
                                    ProPill()
                                }
                            }
                            Switch(
                                checked = p.isPrivate,
                                onCheckedChange = { viewModel.updateField { copy(isPrivate = it) } },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = IMessageBlue,
                                    uncheckedThumbColor = Color.White,
                                    uncheckedTrackColor = Color(0xFF3A3A3C)
                                )
                            )
                        }
                    }

                    Spacer(Modifier.height(FillinSpacing.padding24))
                }
            }
        }
    }
}

/**
 * iOS-style label-on-top, value-on-bottom inline editor row used in the
 * grouped sections. Looks like an "input cell" — no border, just hairline
 * separators between rows (drawn by the section).
 */
@Composable
private fun ProfileFieldRow(
    label: String,
    value: String,
    keyboardType: KeyboardType = KeyboardType.Text,
    trailing: (@Composable () -> Unit)? = null,
    onChange: (String) -> Unit
) {
    val focusManager = LocalFocusManager.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = FillinSpacing.padding16, vertical = FillinSpacing.padding12),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                color = Color(0xFF9C9CA1),
                style = MaterialTheme.typography.labelMedium
            )
            Spacer(Modifier.height(2.dp))
            BasicTextField(
                value = value,
                onValueChange = onChange,
                singleLine = true,
                cursorBrush = SolidColor(IMessageBlue),
                textStyle = TextStyle(
                    color = Color.White,
                    fontSize = MaterialTheme.typography.bodyLarge.fontSize
                ),
                keyboardOptions = KeyboardOptions(
                    keyboardType = keyboardType,
                    imeAction = ImeAction.Next
                ),
                modifier = Modifier.fillMaxWidth()
            )
        }
        trailing?.invoke()
    }
}

/** Right-aligned date pill chip used by the Birth Date row. */
@Composable
private fun DateChip(text: String) {
    Box(
        modifier = Modifier
            .background(
                color = Color(0xFF2C2C2E),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp)
            )
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Text(
            text = text,
            color = Color.White,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
            textAlign = TextAlign.End
        )
    }
}

/** First missing field label for the completion-card CTA. Matches iOS prioritisation. */
private fun nextMissingFieldLabel(p: ProfileEntity): String? {
    if (p.firstName.isBlank()) return "First Name"
    if (p.middleName.isNullOrBlank()) return "Middle Name"
    if (p.lastName.isBlank()) return "Last Name"
    if (p.email.isNullOrBlank()) return "Email"
    if (p.phone.isNullOrBlank()) return "Phone"
    if (p.birthDate.isNullOrBlank()) return "Birth Date"
    if (p.homeAddress.isNullOrBlank()) return "Home Address"
    if (p.city.isNullOrBlank()) return "City"
    if (p.state.isNullOrBlank()) return "State"
    if (p.country.isNullOrBlank()) return "Country"
    if (p.postalCode.isNullOrBlank()) return "Postal Code"
    return null
}
