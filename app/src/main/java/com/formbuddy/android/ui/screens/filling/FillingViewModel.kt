package com.formbuddy.android.ui.screens.filling

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.formbuddy.android.data.local.preferences.PreferencesManager
import com.formbuddy.android.data.model.ChatMessage
import com.formbuddy.android.data.model.ChatSession
import com.formbuddy.android.data.model.FormField
import com.formbuddy.android.data.model.FormMode
import com.formbuddy.android.data.model.FormTemplate
import com.formbuddy.android.data.model.Profile
import com.formbuddy.android.data.model.UserInputMethod
import com.formbuddy.android.data.repository.FormRepository
import com.formbuddy.android.data.repository.ProfileRepository
import com.formbuddy.android.domain.analysis.FormAnalyzer
import com.formbuddy.android.domain.filling.ConversationManager
import com.formbuddy.android.domain.filling.ResponseClassifier
import com.formbuddy.android.domain.speech.SpeechRecognitionService
import com.formbuddy.android.domain.tts.TextToSpeechService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class FillingUiState {
    data object Idle : FillingUiState()
    data object Processing : FillingUiState()
    data object Ready : FillingUiState()
    data class Error(val message: String) : FillingUiState()
}

@HiltViewModel
class FillingViewModel @Inject constructor(
    private val formRepository: FormRepository,
    private val profileRepository: ProfileRepository,
    private val formAnalyzer: FormAnalyzer,
    private val conversationManager: ConversationManager,
    private val responseClassifier: ResponseClassifier,
    private val ttsService: TextToSpeechService,
    private val speechService: SpeechRecognitionService,
    private val preferencesManager: PreferencesManager,
    private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow<FillingUiState>(FillingUiState.Idle)
    val uiState: StateFlow<FillingUiState> = _uiState

    private val _formTemplate = MutableStateFlow<FormTemplate?>(null)
    val formTemplate: StateFlow<FormTemplate?> = _formTemplate

    private val _isProcessing = MutableStateFlow(false)
    val isProcessing: StateFlow<Boolean> = _isProcessing

    private val _formMode = MutableStateFlow(FormMode.CHAT)
    val formMode: StateFlow<FormMode> = _formMode

    private val _chatMessages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val chatMessages: StateFlow<List<ChatMessage>> = _chatMessages

    private val _currentProfile = MutableStateFlow<Profile?>(null)
    val currentProfile: StateFlow<Profile?> = _currentProfile

    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording

    private val _agentProgress = MutableStateFlow(0f)
    val agentProgress: StateFlow<Float> = _agentProgress

    private var documentData: ByteArray? = null
    private var chatSession: ChatSession? = null
    private var existingFormId: String? = null

    fun initialize(source: String, uri: String?, formId: String?) {
        viewModelScope.launch {
            _formMode.value = preferencesManager.defaultFormMode.first()

            // Load profile
            val profiles = profileRepository.getAllProfiles().first()
            _currentProfile.value = profiles.firstOrNull()

            if (formId != null) {
                existingFormId = formId
                loadExistingForm(formId)
            } else if (uri != null) {
                loadNewDocument(uri)
            }
        }
    }

    private suspend fun loadExistingForm(formId: String) {
        val entity = formRepository.getFormById(formId) ?: return
        val template = formRepository.getFormTemplate(entity)
        documentData = formRepository.getDocumentData(formId)
        _formTemplate.value = template
        initChatSession(template)
        _uiState.value = FillingUiState.Ready
    }

    private suspend fun loadNewDocument(uriString: String) {
        _isProcessing.value = true
        _uiState.value = FillingUiState.Processing

        try {
            val uri = Uri.parse(uriString)
            val data = context.contentResolver.openInputStream(uri)?.readBytes()
                ?: throw Exception("Cannot read document")
            documentData = data

            val template = formAnalyzer.analyzeDocument(data)
            _formTemplate.value = template
            initChatSession(template)
            _uiState.value = FillingUiState.Ready
        } catch (e: Exception) {
            _uiState.value = FillingUiState.Error(e.message ?: "Analysis failed")
        }

        _isProcessing.value = false
    }

    private fun initChatSession(template: FormTemplate) {
        chatSession = ChatSession(formTemplate = template)
        conversationManager.startSession(template)
        askNextQuestion()
    }

    private fun askNextQuestion() {
        val session = chatSession ?: return
        val field = session.currentField

        if (field == null || session.isComplete) {
            _chatMessages.value = _chatMessages.value + ChatMessage(
                sender = ChatMessage.Sender.SYSTEM,
                content = "All fields have been filled! You can review and save your form.",
                associatedValue = ChatMessage.AssociatedValue.ConversationDone
            )
            return
        }

        val question = conversationManager.getQuestionForField(field)
        val message = ChatMessage(
            sender = ChatMessage.Sender.SYSTEM,
            content = question,
            associatedValue = ChatMessage.AssociatedValue.Field(field.id)
        )
        _chatMessages.value = _chatMessages.value + message

        // Check for profile suggestion
        val profile = _currentProfile.value
        if (profile != null) {
            val suggestion = profile.suggestedValue(field)
            if (suggestion != null) {
                _chatMessages.value = _chatMessages.value + ChatMessage(
                    sender = ChatMessage.Sender.SYSTEM,
                    content = "Suggestion from your profile: $suggestion",
                    associatedValue = ChatMessage.AssociatedValue.SuggestedValue(suggestion, field.id)
                )
            }
        }
    }

    fun sendChatMessage(text: String) {
        _chatMessages.value = _chatMessages.value + ChatMessage(
            sender = ChatMessage.Sender.USER,
            content = text
        )

        val session = chatSession ?: return
        val field = session.currentField ?: return

        val classification = responseClassifier.classify(text, field)
        if (classification == ResponseClassifier.Classification.SKIP) {
            session.currentFieldIndex++
            askNextQuestion()
            return
        }

        field.userValue = text
        field.userInputMethod = UserInputMethod.VOICE_FILLED
        _formTemplate.value = _formTemplate.value?.copy() // trigger recomposition
        session.currentFieldIndex++
        askNextQuestion()
    }

    fun acceptSuggestion(value: String, fieldId: String) {
        val template = _formTemplate.value ?: return
        val field = template.allFields.find { it.id == fieldId } ?: return
        field.userValue = value
        field.userInputMethod = UserInputMethod.ACCEPTED_SUGGESTION
        _formTemplate.value = template.copy()
        chatSession?.currentFieldIndex = (chatSession?.currentFieldIndex ?: 0) + 1

        _chatMessages.value = _chatMessages.value + ChatMessage(
            sender = ChatMessage.Sender.USER,
            content = value
        )
        askNextQuestion()
    }

    fun undoLastField() {
        val session = chatSession ?: return
        if (session.currentFieldIndex > 0) {
            session.currentFieldIndex--
            val field = session.currentField
            field?.userValue = null
            field?.userInputMethod = null
            _formTemplate.value = _formTemplate.value?.copy()
        }
    }

    fun skipToEnd() {
        val session = chatSession ?: return
        val template = _formTemplate.value ?: return
        session.currentFieldIndex = template.allFields.size
        _chatMessages.value = _chatMessages.value + ChatMessage(
            sender = ChatMessage.Sender.SYSTEM,
            content = "Skipped remaining fields. You can review and save your form.",
            associatedValue = ChatMessage.AssociatedValue.ConversationDone
        )
    }

    // Agent mode
    fun runAgent() {
        viewModelScope.launch {
            val template = _formTemplate.value ?: return@launch
            val profile = _currentProfile.value ?: return@launch
            val emptyFields = template.allFields.filter { it.isEmpty }
            val totalEmpty = emptyFields.size

            emptyFields.forEachIndexed { index, field ->
                val suggestion = profile.suggestedValue(field)
                if (suggestion != null) {
                    field.userValue = suggestion
                    field.userInputMethod = UserInputMethod.PROFILE_AUTOFILL
                }
                _agentProgress.value = (index + 1).toFloat() / totalEmpty
            }

            _formTemplate.value = template.copy()
            initChatSession(template) // reinit for remaining unfilled fields
        }
    }

    // Voice
    fun startRecording() {
        _isRecording.value = true
        speechService.startListening { result ->
            _isRecording.value = false
            if (result.isNotBlank()) {
                sendChatMessage(result)
            }
        }
    }

    fun stopRecording() {
        _isRecording.value = false
        speechService.stopListening()
    }

    fun speakText(text: String) {
        viewModelScope.launch {
            ttsService.speak(text)
        }
    }

    fun saveForm(onSaved: (String) -> Unit) {
        viewModelScope.launch {
            val template = _formTemplate.value ?: return@launch
            val data = documentData ?: return@launch
            val id = formRepository.saveForm(template, data, existingFormId)
            onSaved(id)
        }
    }

    fun setProfile(profile: Profile) {
        _currentProfile.value = profile
    }

    override fun onCleared() {
        super.onCleared()
        ttsService.shutdown()
        speechService.stopListening()
    }
}
