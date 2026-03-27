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
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BusinessProfileScreen(
    navController: NavHostController,
    profileId: String,
    viewModel: BusinessProfileViewModel = hiltViewModel()
) {
    val profile by viewModel.profile.collectAsState()

    LaunchedEffect(profileId) {
        viewModel.loadProfile(profileId)
    }

    val p = profile ?: return

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.profile_business)) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.action_back))
                    }
                },
                actions = {
                    IconButton(onClick = {
                        viewModel.deleteProfile()
                        navController.popBackStack()
                    }) {
                        Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.action_delete))
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
            Text(stringResource(R.string.profile_company_section), style = MaterialTheme.typography.titleSmall)
            FloatingLabelTextField(
                value = p.companyName,
                onValueChange = { viewModel.updateField { copy(companyName = it) } },
                label = stringResource(R.string.field_company_name)
            )
            FloatingLabelTextField(
                value = p.taxId ?: "",
                onValueChange = { viewModel.updateField { copy(taxId = it.ifBlank { null }) } },
                label = stringResource(R.string.field_tax_id)
            )
            FloatingLabelTextField(
                value = p.registrationNumber ?: "",
                onValueChange = { viewModel.updateField { copy(registrationNumber = it.ifBlank { null }) } },
                label = stringResource(R.string.field_registration_number)
            )
            FloatingLabelTextField(
                value = p.industry ?: "",
                onValueChange = { viewModel.updateField { copy(industry = it.ifBlank { null }) } },
                label = stringResource(R.string.field_industry)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(stringResource(R.string.profile_contact_section), style = MaterialTheme.typography.titleSmall)
            FloatingLabelTextField(
                value = p.businessAddress ?: "",
                onValueChange = { viewModel.updateField { copy(businessAddress = it.ifBlank { null }) } },
                label = stringResource(R.string.field_business_address)
            )
            FloatingLabelTextField(
                value = p.businessPhone ?: "",
                onValueChange = { viewModel.updateField { copy(businessPhone = it.ifBlank { null }) } },
                label = stringResource(R.string.field_business_phone),
                keyboardType = KeyboardType.Phone
            )
            FloatingLabelTextField(
                value = p.businessEmail ?: "",
                onValueChange = { viewModel.updateField { copy(businessEmail = it.ifBlank { null }) } },
                label = stringResource(R.string.field_business_email),
                keyboardType = KeyboardType.Email
            )
            FloatingLabelTextField(
                value = p.website ?: "",
                onValueChange = { viewModel.updateField { copy(website = it.ifBlank { null }) } },
                label = stringResource(R.string.field_website),
                keyboardType = KeyboardType.Uri,
                imeAction = ImeAction.Done
            )

            Spacer(modifier = Modifier.height(8.dp))

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
                    Text(stringResource(R.string.profile_biometric_lock), style = MaterialTheme.typography.titleSmall)
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
