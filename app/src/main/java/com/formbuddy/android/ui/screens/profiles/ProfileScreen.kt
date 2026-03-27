package com.formbuddy.android.ui.screens.profiles

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.formbuddy.android.R
import com.formbuddy.android.ui.components.FloatingLabelTextField
import com.formbuddy.android.ui.components.ProBadge
import com.formbuddy.android.ui.components.SignatureCanvas
import com.formbuddy.android.ui.components.pathToSignatureStrokes
import com.formbuddy.android.ui.navigation.Screen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    navController: NavHostController,
    profileId: String,
    isFamily: Boolean,
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val profile by viewModel.profile.collectAsState()

    LaunchedEffect(profileId) {
        viewModel.loadProfile(profileId)
    }

    val p = profile ?: return

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(if (isFamily) stringResource(R.string.profile_family) else stringResource(R.string.profile_personal))
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.action_back))
                    }
                },
                actions = {
                    if (isFamily) {
                        IconButton(onClick = {
                            viewModel.deleteProfile()
                            navController.popBackStack()
                        }) {
                            Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.action_delete))
                        }
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Name fields
            Text(stringResource(R.string.profile_name_section), style = MaterialTheme.typography.titleSmall)
            FloatingLabelTextField(
                value = p.firstName,
                onValueChange = { viewModel.updateField { copy(firstName = it) } },
                label = stringResource(R.string.field_first_name)
            )
            FloatingLabelTextField(
                value = p.middleName ?: "",
                onValueChange = { viewModel.updateField { copy(middleName = it.ifBlank { null }) } },
                label = stringResource(R.string.field_middle_name)
            )
            FloatingLabelTextField(
                value = p.lastName,
                onValueChange = { viewModel.updateField { copy(lastName = it) } },
                label = stringResource(R.string.field_last_name)
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Contact
            Text(stringResource(R.string.profile_contact_section), style = MaterialTheme.typography.titleSmall)
            FloatingLabelTextField(
                value = p.email ?: "",
                onValueChange = { viewModel.updateField { copy(email = it.ifBlank { null }) } },
                label = stringResource(R.string.field_email),
                keyboardType = KeyboardType.Email
            )
            FloatingLabelTextField(
                value = p.phone ?: "",
                onValueChange = { viewModel.updateField { copy(phone = it.ifBlank { null }) } },
                label = stringResource(R.string.field_phone),
                keyboardType = KeyboardType.Phone
            )
            FloatingLabelTextField(
                value = p.birthDate ?: "",
                onValueChange = { viewModel.updateField { copy(birthDate = it.ifBlank { null }) } },
                label = stringResource(R.string.field_birth_date)
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Address
            Text(stringResource(R.string.profile_address_section), style = MaterialTheme.typography.titleSmall)
            FloatingLabelTextField(
                value = p.homeAddress ?: "",
                onValueChange = { viewModel.updateField { copy(homeAddress = it.ifBlank { null }) } },
                label = stringResource(R.string.field_home_address)
            )
            FloatingLabelTextField(
                value = p.workAddress ?: "",
                onValueChange = { viewModel.updateField { copy(workAddress = it.ifBlank { null }) } },
                label = stringResource(R.string.field_work_address)
            )
            FloatingLabelTextField(
                value = p.city ?: "",
                onValueChange = { viewModel.updateField { copy(city = it.ifBlank { null }) } },
                label = stringResource(R.string.field_city)
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                FloatingLabelTextField(
                    value = p.state ?: "",
                    onValueChange = { viewModel.updateField { copy(state = it.ifBlank { null }) } },
                    label = stringResource(R.string.field_state),
                    modifier = Modifier.weight(1f)
                )
                FloatingLabelTextField(
                    value = p.postalCode ?: "",
                    onValueChange = { viewModel.updateField { copy(postalCode = it.ifBlank { null }) } },
                    label = stringResource(R.string.field_postal_code),
                    modifier = Modifier.weight(1f)
                )
            }
            FloatingLabelTextField(
                value = p.country ?: "",
                onValueChange = { viewModel.updateField { copy(country = it.ifBlank { null }) } },
                label = stringResource(R.string.field_country),
                imeAction = ImeAction.Done
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Signature
            Text(stringResource(R.string.profile_signature_section), style = MaterialTheme.typography.titleSmall)
            SignatureCanvas(
                strokes = pathToSignatureStrokes(p.signaturePath ?: ""),
                enabled = false,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedButton(
                onClick = { navController.navigate(Screen.Signature.createRoute(profileId)) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Filled.Edit, contentDescription = null, modifier = Modifier.padding(end = 8.dp))
                Text(stringResource(R.string.profile_edit_signature))
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Privacy
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        if (p.isPrivate) Icons.Filled.Lock else Icons.Filled.LockOpen,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Column {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(stringResource(R.string.profile_biometric_lock), style = MaterialTheme.typography.titleSmall)
                            ProBadge()
                        }
                        Text(
                            stringResource(R.string.profile_biometric_description),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Switch(
                    checked = p.isPrivate,
                    onCheckedChange = { viewModel.updateField { copy(isPrivate = it) } }
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
