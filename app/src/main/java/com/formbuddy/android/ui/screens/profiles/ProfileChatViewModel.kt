package com.formbuddy.android.ui.screens.profiles

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.formbuddy.android.data.local.db.entity.ProfileEntity
import com.formbuddy.android.data.model.ChatMessage
import com.formbuddy.android.data.repository.ProfileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProfileChatViewModel @Inject constructor(
    private val profileRepository: ProfileRepository
) : ViewModel() {

    private val profileFields = listOf(
        "firstName" to "What's your first name?",
        "lastName" to "What's your last name?",
        "email" to "What's your email address?",
        "phone" to "What's your phone number?",
        "birthDate" to "What's your date of birth?",
        "homeAddress" to "What's your home address?",
        "city" to "What city do you live in?",
        "state" to "What state/province?",
        "postalCode" to "What's your postal/ZIP code?",
        "country" to "What country?"
    )

    private var currentFieldIndex = 0
    private var profile: ProfileEntity? = null

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages

    private val _isComplete = MutableStateFlow(false)
    val isComplete: StateFlow<Boolean> = _isComplete

    init {
        viewModelScope.launch {
            profile = profileRepository.getPersonalProfile().first()
            if (profile == null) {
                profile = profileRepository.createPersonalProfile()
            }
            askNextQuestion()
        }
    }

    private fun askNextQuestion() {
        if (currentFieldIndex >= profileFields.size) {
            _messages.value = _messages.value + ChatMessage(
                sender = ChatMessage.Sender.SYSTEM,
                content = "Great! Your profile is all set up. You can always edit it later from the Profiles tab."
            )
            _isComplete.value = true
            return
        }

        val (_, question) = profileFields[currentFieldIndex]
        _messages.value = _messages.value + ChatMessage(
            sender = ChatMessage.Sender.SYSTEM,
            content = question
        )
    }

    fun sendMessage(text: String) {
        _messages.value = _messages.value + ChatMessage(
            sender = ChatMessage.Sender.USER,
            content = text
        )

        val p = profile ?: return
        val (field, _) = profileFields[currentFieldIndex]

        val updated = when (field) {
            "firstName" -> p.copy(firstName = text)
            "lastName" -> p.copy(lastName = text)
            "email" -> p.copy(email = text)
            "phone" -> p.copy(phone = text)
            "birthDate" -> p.copy(birthDate = text)
            "homeAddress" -> p.copy(homeAddress = text)
            "city" -> p.copy(city = text)
            "state" -> p.copy(state = text)
            "postalCode" -> p.copy(postalCode = text)
            "country" -> p.copy(country = text)
            else -> p
        }

        profile = updated
        viewModelScope.launch {
            profileRepository.saveProfile(updated)
        }

        currentFieldIndex++
        askNextQuestion()
    }
}
