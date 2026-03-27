package com.formbuddy.android.ui.screens.filling

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.formbuddy.android.R
import com.formbuddy.android.data.model.FormMode
import com.formbuddy.android.ui.components.FormCompletionCard
import com.formbuddy.android.ui.navigation.Screen
import com.formbuddy.android.ui.screens.filling.agent.AgentView
import com.formbuddy.android.ui.screens.filling.chat.ChatView
import com.formbuddy.android.ui.screens.filling.voice.VoiceView

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FillingScreen(
    navController: NavHostController,
    source: String,
    uri: String?,
    formId: String?,
    viewModel: FillingViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val formTemplate by viewModel.formTemplate.collectAsState()
    val isProcessing by viewModel.isProcessing.collectAsState()
    val formMode by viewModel.formMode.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.initialize(source, uri, formId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(formTemplate?.documentName ?: stringResource(R.string.filling_processing)) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.action_close))
                    }
                },
                actions = {
                    if (formTemplate != null) {
                        IconButton(onClick = {
                            viewModel.saveForm {
                                navController.navigate(Screen.Editor.createRoute(it)) {
                                    popUpTo(Screen.Docs.route)
                                }
                            }
                        }) {
                            Text(stringResource(R.string.action_save))
                        }
                    }
                }
            )
        }
    ) { padding ->
        if (isProcessing) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator()
                    Text(
                        text = stringResource(R.string.filling_analyzing),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(top = androidx.compose.ui.unit.dp(16))
                    )
                }
            }
        } else if (formTemplate != null) {
            val template = formTemplate!!

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                // Completion card
                FormCompletionCard(
                    completionPercentage = template.completionPercentage,
                    completedFields = template.completedFieldsCount,
                    totalFields = template.totalFieldsCount,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )

                // Mode tabs
                val modes = listOf(
                    stringResource(R.string.mode_chat),
                    stringResource(R.string.mode_voice),
                    stringResource(R.string.mode_agent)
                )
                var selectedTab by remember {
                    mutableIntStateOf(
                        when (formMode) {
                            FormMode.CHAT -> 0
                            FormMode.VOICE -> 1
                            FormMode.AGENT -> 2
                        }
                    )
                }

                TabRow(selectedTabIndex = selectedTab) {
                    modes.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            text = { Text(title) }
                        )
                    }
                }

                // Mode content
                when (selectedTab) {
                    0 -> ChatView(viewModel = viewModel)
                    1 -> VoiceView(viewModel = viewModel)
                    2 -> AgentView(viewModel = viewModel)
                }
            }
        }
    }
}
