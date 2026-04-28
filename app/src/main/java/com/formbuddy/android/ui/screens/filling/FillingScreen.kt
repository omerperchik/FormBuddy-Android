package com.formbuddy.android.ui.screens.filling

import androidx.compose.foundation.ScrollState
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
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import com.formbuddy.android.R
import com.formbuddy.android.data.model.FormMode
import com.formbuddy.android.ui.components.FirstSaveCelebrationOverlay
import com.formbuddy.android.ui.components.FormCompletionCard
import com.formbuddy.android.ui.navigation.Screen
import com.formbuddy.android.ui.screens.filling.agent.AgentView
import com.formbuddy.android.ui.screens.filling.chat.ChatView
import com.formbuddy.android.ui.screens.filling.document.DocumentAnalysisView
import com.formbuddy.android.ui.screens.filling.form.FormFillingView
import com.formbuddy.android.ui.screens.filling.voice.VoiceView
import kotlinx.coroutines.launch

/**
 * Mirrors iOS `DocumentFillingView` — exposes the document, structured form, and the
 * conversational filling modes (chat, voice, agent) as scrollable tabs. The first
 * tabs (Document/Form) match the iOS tab structure exactly; the rest map to iOS
 * Chat-sheet capabilities surfaced as their own tabs on Android for discoverability.
 */
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

    var selectedFieldId by remember { mutableStateOf<String?>(null) }
    val showCelebration by viewModel.showCelebration.collectAsState()
    val activity = LocalContext.current.findActivity()
    val scope = rememberCoroutineScope()

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
                        modifier = Modifier.padding(top = 16.dp)
                    )
                }
            }
            return@Scaffold
        }

        val template = formTemplate ?: return@Scaffold

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            FormCompletionCard(
                completionPercentage = template.completionPercentage,
                completedFields = template.completedFieldsCount,
                totalFields = template.totalFieldsCount,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )

            val tabs = remember {
                listOf(
                    R.string.mode_document,
                    R.string.mode_form,
                    R.string.mode_chat,
                    R.string.mode_voice,
                    R.string.mode_agent
                )
            }
            // Default tab follows the user's preferred FormMode setting, mapped onto the
            // 5-tab Android layout: Chat=2, Voice=3, Agent=4. Document/Form aren't first
            // class FormMode values yet.
            var selectedTab by remember(formMode) {
                mutableIntStateOf(
                    when (formMode) {
                        FormMode.CHAT -> 2
                        FormMode.VOICE -> 3
                        FormMode.AGENT -> 4
                    }
                )
            }

            ScrollableTabRow(
                selectedTabIndex = selectedTab,
                edgePadding = 16.dp
            ) {
                tabs.forEachIndexed { index, titleRes ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(stringResource(titleRes)) }
                    )
                }
            }

            Box(modifier = Modifier.fillMaxSize()) {
                when (selectedTab) {
                    0 -> DocumentAnalysisView(
                        template = template,
                        documentBytes = viewModel.documentBytes(),
                        selectedFieldId = selectedFieldId,
                        onFieldSelected = { selectedFieldId = it },
                        editorMode = viewModel.editorMode.collectAsState().value,
                        onAddFieldAt = { pageIndex, x, y ->
                            viewModel.addUserGeneratedField(pageIndex, x, y)
                        }
                    )
                    1 -> FormFillingView(
                        template = template,
                        viewModel = viewModel,
                        selectedFieldId = selectedFieldId,
                        onFieldSelected = { selectedFieldId = it },
                        editorMode = viewModel.editorMode.collectAsState().value
                    )
                    2 -> ChatView(viewModel = viewModel)
                    3 -> VoiceView(viewModel = viewModel)
                    4 -> AgentView(viewModel = viewModel)
                }

                FirstSaveCelebrationOverlay(
                    isVisible = showCelebration,
                    onDismiss = { viewModel.dismissCelebration() },
                    onRequestReview = {
                        activity?.let { act -> scope.launch { viewModel.maybeAskForReview(act) } }
                    }
                )
            }
        }
    }
}

/** Walks ContextWrappers to find the host Activity. */
private fun Context.findActivity(): Activity? {
    var ctx: Context? = this
    while (ctx is ContextWrapper) {
        if (ctx is Activity) return ctx
        ctx = ctx.baseContext
    }
    return null
}
