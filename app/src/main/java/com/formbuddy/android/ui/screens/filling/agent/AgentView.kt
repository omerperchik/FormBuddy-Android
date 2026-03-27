package com.formbuddy.android.ui.screens.filling.agent

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.formbuddy.android.R
import com.formbuddy.android.ui.screens.filling.FillingViewModel

@Composable
fun AgentView(viewModel: FillingViewModel) {
    val formTemplate by viewModel.formTemplate.collectAsState()
    val agentProgress by viewModel.agentProgress.collectAsState()
    val currentProfile by viewModel.currentProfile.collectAsState()
    var hasStarted by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        if (!hasStarted) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = stringResource(R.string.agent_title),
                    style = MaterialTheme.typography.headlineMedium,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = stringResource(R.string.agent_description),
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (currentProfile != null) {
                    Text(
                        text = stringResource(R.string.agent_using_profile, currentProfile!!.displayName),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = {
                        hasStarted = true
                        viewModel.runAgent()
                    },
                    enabled = currentProfile != null
                ) {
                    Text(stringResource(R.string.agent_start))
                }

                if (currentProfile == null) {
                    Text(
                        text = stringResource(R.string.agent_no_profile),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        } else {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = if (agentProgress >= 1f)
                        stringResource(R.string.agent_complete)
                    else
                        stringResource(R.string.agent_filling),
                    style = MaterialTheme.typography.titleLarge,
                    textAlign = TextAlign.Center
                )

                LinearProgressIndicator(
                    progress = { agentProgress },
                    modifier = Modifier.fillMaxWidth()
                )

                Text(
                    text = "${(agentProgress * 100).toInt()}%",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.primary
                )

                if (agentProgress >= 1f) {
                    val template = formTemplate
                    if (template != null) {
                        val remaining = template.allFields.count { it.isEmpty }
                        if (remaining > 0) {
                            Text(
                                text = stringResource(R.string.agent_remaining, remaining),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}
