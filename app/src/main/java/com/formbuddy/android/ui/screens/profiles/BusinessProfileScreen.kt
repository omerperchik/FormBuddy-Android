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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Icon
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.formbuddy.android.R
import com.formbuddy.android.ui.components.BiometricGate
import com.formbuddy.android.ui.components.ios.FillinPressContainer
import com.formbuddy.android.ui.components.ios.FillinSection
import com.formbuddy.android.ui.components.ios.FillinSeparator
import com.formbuddy.android.ui.components.ios.FillinSpacing
import com.formbuddy.android.ui.components.ios.FillinTopBar
import com.formbuddy.android.ui.components.ios.ProPill
import com.formbuddy.android.ui.theme.IMessageBlue
import androidx.compose.ui.res.stringResource

/**
 * iOS-matching Business profile editor — same grouped-list look as
 * [ProfileScreen]. Sections: Company / Contact / Profile Info.
 */
@Composable
fun BusinessProfileScreen(
    navController: NavHostController,
    profileId: String,
    viewModel: BusinessProfileViewModel = hiltViewModel()
) {
    val profile by viewModel.profile.collectAsState()

    LaunchedEffect(profileId) { viewModel.loadProfile(profileId) }

    val p = profile ?: return
    val title = stringResource(R.string.profile_business)
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
                        FillinPressContainer(
                            onClick = {
                                viewModel.deleteProfile()
                                navController.popBackStack()
                            },
                            modifier = Modifier.padding(horizontal = FillinSpacing.padding4)
                        ) {
                            Icon(
                                Icons.Filled.Delete,
                                contentDescription = "Delete",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                )

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(bottom = 120.dp)
                ) {
                    FillinSection(title = "Company") {
                        BizFieldRow("Company name", p.companyName) {
                            viewModel.updateField { copy(companyName = it) }
                        }
                        FillinSeparator()
                        BizFieldRow("Tax ID / EIN", p.taxId.orEmpty()) {
                            viewModel.updateField { copy(taxId = it.ifBlank { null }) }
                        }
                        FillinSeparator()
                        BizFieldRow("Registration #", p.registrationNumber.orEmpty()) {
                            viewModel.updateField { copy(registrationNumber = it.ifBlank { null }) }
                        }
                        FillinSeparator()
                        BizFieldRow("Industry", p.industry.orEmpty()) {
                            viewModel.updateField { copy(industry = it.ifBlank { null }) }
                        }
                    }

                    FillinSection(title = "Contact") {
                        BizFieldRow("Business address", p.businessAddress.orEmpty()) {
                            viewModel.updateField { copy(businessAddress = it.ifBlank { null }) }
                        }
                        FillinSeparator()
                        BizFieldRow(
                            "Business phone",
                            p.businessPhone.orEmpty(),
                            keyboardType = KeyboardType.Phone
                        ) {
                            viewModel.updateField { copy(businessPhone = it.ifBlank { null }) }
                        }
                        FillinSeparator()
                        BizFieldRow(
                            "Business email",
                            p.businessEmail.orEmpty(),
                            keyboardType = KeyboardType.Email
                        ) {
                            viewModel.updateField { copy(businessEmail = it.ifBlank { null }) }
                        }
                        FillinSeparator()
                        BizFieldRow("Website", p.website.orEmpty(), keyboardType = KeyboardType.Uri) {
                            viewModel.updateField { copy(website = it.ifBlank { null }) }
                        }
                    }

                    FillinSection(title = "Profile Info") {
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

@Composable
private fun BizFieldRow(
    label: String,
    value: String,
    keyboardType: KeyboardType = KeyboardType.Text,
    onChange: (String) -> Unit
) {
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
    }
}
