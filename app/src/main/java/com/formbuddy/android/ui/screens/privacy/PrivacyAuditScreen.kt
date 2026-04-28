package com.formbuddy.android.ui.screens.privacy

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Phonelink
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavHostController
import com.formbuddy.android.data.privacy.PrivacyAuditLog
import com.formbuddy.android.ui.components.ios.FillinSection
import com.formbuddy.android.ui.components.ios.FillinSeparator
import com.formbuddy.android.ui.components.ios.FillinSpacing
import com.formbuddy.android.ui.components.ios.FillinTextButton
import com.formbuddy.android.ui.components.ios.FillinTopBar
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PrivacyAuditViewModel @Inject constructor(
    val auditLog: PrivacyAuditLog
) : ViewModel() {
    val entries: Flow<List<PrivacyAuditLog.Entry>> = auditLog.entries

    private val _exported = MutableStateFlow<String?>(null)
    val exported: StateFlow<String?> = _exported

    fun export() {
        viewModelScope.launch { _exported.value = auditLog.exportText() }
    }

    fun clear() {
        viewModelScope.launch { auditLog.clear() }
    }
}

@Composable
fun PrivacyAuditScreen(
    navController: NavHostController,
    viewModel: PrivacyAuditViewModel = hiltViewModel()
) {
    val entries by viewModel.entries.collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()

    Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        FillinTopBar(
            title = "Privacy audit log",
            onBack = { navController.popBackStack() },
            trailing = {
                FillinTextButton(text = "Clear", onClick = { viewModel.clear() })
            }
        )

        if (entries.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Filled.Shield,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = FillinSpacing.padding16)
                    )
                    Text("Nothing has crossed your device boundary yet.")
                }
            }
            return@Column
        }

        FillinSection(
            title = "Recent activity",
            footer = "We log every action that could leave your device. Field values are " +
                "hashed (not stored) so this list never leaks the data we promised to keep on-device."
        ) {
            entries.reversed().forEachIndexed { index, entry ->
                AuditEntryRow(entry)
                if (index < entries.size - 1) FillinSeparator()
            }
        }
    }
}

@Composable
private fun AuditEntryRow(entry: PrivacyAuditLog.Entry) {
    val (icon, label) = when (entry.category) {
        PrivacyAuditLog.Category.OnDevice -> Icons.Filled.Phonelink to "On-device"
        PrivacyAuditLog.Category.Cloud -> Icons.Filled.Cloud to "Cloud"
        PrivacyAuditLog.Category.Telemetry -> Icons.Filled.Cloud to "Telemetry"
        PrivacyAuditLog.Category.Storage -> Icons.Filled.Save to "Storage"
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(FillinSpacing.padding16),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(FillinSpacing.padding12)
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(label, style = MaterialTheme.typography.labelMedium)
                Spacer(Modifier.padding(horizontal = FillinSpacing.padding4))
                Text(
                    entry.destination,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(entry.description, style = MaterialTheme.typography.bodyMedium)
            Text(
                java.text.DateFormat.getDateTimeInstance().format(java.util.Date(entry.timestampMs)),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

