package com.formbuddy.android.ui.screens.profiles

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.formbuddy.android.data.local.db.entity.ProfileEntity
import com.formbuddy.android.data.repository.ProfileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val profileRepository: ProfileRepository
) : ViewModel() {

    private val _profile = MutableStateFlow<ProfileEntity?>(null)
    val profile: StateFlow<ProfileEntity?> = _profile

    fun loadProfile(id: String) {
        viewModelScope.launch {
            _profile.value = profileRepository.getProfileById(id)
        }
    }

    fun updateField(transform: ProfileEntity.() -> ProfileEntity) {
        val current = _profile.value ?: return
        val updated = current.transform()
        _profile.value = updated
        viewModelScope.launch {
            profileRepository.saveProfile(updated)
        }
    }

    fun deleteProfile() {
        val current = _profile.value ?: return
        viewModelScope.launch {
            profileRepository.deleteProfile(current)
        }
    }
}
