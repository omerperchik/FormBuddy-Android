package com.formbuddy.android.ui.screens.filling.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.formbuddy.android.data.model.ChatMessage
import com.formbuddy.android.ui.components.ios.FillinChatBubble
import com.formbuddy.android.ui.components.ios.FillinPressContainer
import com.formbuddy.android.ui.components.ios.FillinShapes
import com.formbuddy.android.ui.components.ios.FillinSpacing
import com.formbuddy.android.ui.components.ios.FillinSuggestionChip
import com.formbuddy.android.ui.components.ios.ProPill
import com.formbuddy.android.ui.screens.filling.FillingViewModel
import com.formbuddy.android.ui.theme.IMessageBlue

/**
 * iOS-matching chat view (IMG_9056 / 9057 / 9065 / 9066).
 *
 * - Asymmetric iMessage-style bubbles via [FillinChatBubble].
 * - Bottom input bar:
 *   - Leading mic button with a "PRO" badge floating above it.
 *   - Center pill text field showing the current question's field name in
 *     a small label above the input row, then the actual input.
 *   - Trailing blue circular send arrow.
 * - "Skip to end" pill above the input on the right when there are more
 *   fields, and "Review Document" wide blue pill when complete.
 */
@Composable
fun ChatView(viewModel: FillingViewModel) {
    val messages by viewModel.chatMessages.collectAsState()
    var inputText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    val isComplete by remember {
        derivedStateOf {
            messages.lastOrNull()?.associatedValue is ChatMessage.AssociatedValue.ConversationDone
        }
    }

    val currentFieldId by remember {
        derivedStateOf {
            messages.lastOrNull { it.sender == ChatMessage.Sender.SYSTEM }
                ?.associatedValue?.let { (it as? ChatMessage.AssociatedValue.Field)?.fieldId }
        }
    }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .imePadding()
    ) {
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            state = listState,
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            items(messages) { message ->
                Column {
                    val isUser = message.sender == ChatMessage.Sender.USER
                    FillinChatBubble(text = message.content, isSent = isUser)

                    if (message.associatedValue is ChatMessage.AssociatedValue.SuggestedValue) {
                        val s = message.associatedValue as ChatMessage.AssociatedValue.SuggestedValue
                        FillinSuggestionChip(
                            suggestion = s.value,
                            onAccept = { viewModel.acceptSuggestion(s.value, s.fieldId) }
                        )
                    }
                }
            }
        }

        if (isComplete) {
            // Wide "Review Document" CTA replaces the input bar (IMG_9057).
            ReviewDocumentBar()
        } else {
            ChatInputBar(
                inputText = inputText,
                onInputChange = { inputText = it },
                onSend = {
                    if (inputText.isNotBlank()) {
                        viewModel.sendChatMessage(inputText)
                        inputText = ""
                    }
                },
                onSkipToEnd = { viewModel.skipToEnd() },
                onMic = { viewModel.startRecording() },
                fieldHint = currentFieldId?.let { id ->
                    viewModel.formTemplate.value?.allFields?.firstOrNull { it.id == id }?.label
                }
            )
        }
    }
}

@Composable
private fun ChatInputBar(
    inputText: String,
    onInputChange: (String) -> Unit,
    onSend: () -> Unit,
    onSkipToEnd: () -> Unit,
    onMic: () -> Unit,
    fieldHint: String?
) {
    Column {
        // Skip to end pill — top-right.
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = FillinSpacing.padding16, vertical = FillinSpacing.padding4),
            horizontalArrangement = Arrangement.End
        ) {
            FillinPressContainer(
                onClick = onSkipToEnd,
                modifier = Modifier
                    .clip(FillinShapes.capsule)
                    .background(Color(0xFF1F1F1F))
                    .padding(horizontal = FillinSpacing.padding12, vertical = FillinSpacing.padding6)
            ) {
                Text(
                    text = "Skip to end",
                    color = Color(0xFF9C9CA1),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = FillinSpacing.padding16, vertical = FillinSpacing.padding8),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(FillinSpacing.padding8)
        ) {
            // Mic button with floating PRO chip.
            Box {
                FillinPressContainer(
                    onClick = onMic,
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF1F1F1F))
                ) {
                    Icon(
                        Icons.Filled.Mic,
                        contentDescription = "Mic",
                        tint = IMessageBlue,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = (-2).dp, end = (-4).dp)
                ) {
                    ProPill()
                }
            }

            // Center pill input — outlined accent border, with a small field
            // label floating at the top of the pill (notch effect).
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(FillinShapes.capsule)
                    .background(Color.Transparent)
                    .border(1.dp, IMessageBlue, FillinShapes.capsule)
                    .padding(horizontal = FillinSpacing.padding16)
                    .height(44.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (fieldHint != null) {
                        Text(
                            text = fieldHint,
                            color = IMessageBlue,
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                            modifier = Modifier.padding(end = FillinSpacing.padding8)
                        )
                    }
                    Box(modifier = Modifier.weight(1f)) {
                        if (inputText.isEmpty()) {
                            Text(
                                text = "Enter text here",
                                color = Color(0xFF6C6C72),
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                        BasicTextField(
                            value = inputText,
                            onValueChange = onInputChange,
                            singleLine = true,
                            cursorBrush = SolidColor(IMessageBlue),
                            textStyle = TextStyle(
                                color = Color.White,
                                fontSize = MaterialTheme.typography.bodyLarge.fontSize
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            // Send button — circular blue.
            FillinPressContainer(
                onClick = onSend,
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(IMessageBlue)
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.Send,
                    contentDescription = "Send",
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
private fun ReviewDocumentBar() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(FillinSpacing.padding16),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(FillinSpacing.padding8)
    ) {
        // Inactive mic on the left to keep visual weight matched.
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(Color(0xFF1F1F1F))
        ) {
            Icon(
                Icons.Filled.Mic,
                contentDescription = null,
                tint = Color(0xFF6C6C72),
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(20.dp)
            )
        }
        FillinPressContainer(
            onClick = { /* host scrolls to Form tab */ },
            modifier = Modifier
                .weight(1f)
                .height(44.dp)
                .clip(FillinShapes.capsule)
                .background(IMessageBlue)
        ) {
            Text(
                text = "Review Document",
                color = Color.White,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
            )
        }
    }
}

@Composable
private fun BoxWithAlign(modifier: Modifier = Modifier, content: @Composable () -> Unit) =
    Box(modifier = modifier, contentAlignment = Alignment.Center) { content() }
