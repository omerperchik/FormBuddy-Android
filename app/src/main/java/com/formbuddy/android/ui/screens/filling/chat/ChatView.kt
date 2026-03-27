package com.formbuddy.android.ui.screens.filling.chat

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.unit.dp
import com.formbuddy.android.R
import com.formbuddy.android.data.model.ChatMessage
import com.formbuddy.android.ui.components.ChatBubble
import com.formbuddy.android.ui.screens.filling.FillingViewModel

@Composable
fun ChatView(viewModel: FillingViewModel) {
    val messages by viewModel.chatMessages.collectAsState()
    var inputText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .imePadding()
    ) {
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            state = listState,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(messages) { message ->
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    ChatBubble(
                        text = message.content,
                        isUser = message.sender == ChatMessage.Sender.USER
                    )

                    // Show suggestion chip
                    if (message.associatedValue is ChatMessage.AssociatedValue.SuggestedValue) {
                        val suggestion = message.associatedValue as ChatMessage.AssociatedValue.SuggestedValue
                        AssistChip(
                            onClick = { viewModel.acceptSuggestion(suggestion.value, suggestion.fieldId) },
                            label = { Text(stringResource(R.string.chat_use_suggestion, suggestion.value)) }
                        )
                    }
                }
            }
        }

        // Action buttons
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            TextButton(onClick = { viewModel.undoLastField() }) {
                Icon(Icons.Filled.Undo, contentDescription = null, modifier = Modifier.padding(end = 4.dp))
                Text(stringResource(R.string.action_undo))
            }
            TextButton(onClick = { viewModel.skipToEnd() }) {
                Icon(Icons.Filled.SkipNext, contentDescription = null, modifier = Modifier.padding(end = 4.dp))
                Text(stringResource(R.string.action_skip_to_end))
            }
        }

        // Input bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = inputText,
                onValueChange = { inputText = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text(stringResource(R.string.chat_input_placeholder)) },
                shape = MaterialTheme.shapes.large
            )
            IconButton(
                onClick = {
                    if (inputText.isNotBlank()) {
                        viewModel.sendChatMessage(inputText)
                        inputText = ""
                    }
                }
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.Send,
                    contentDescription = stringResource(R.string.action_send),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}
