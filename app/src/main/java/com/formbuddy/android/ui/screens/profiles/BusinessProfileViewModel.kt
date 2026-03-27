package com.formbuddy.android.ui.screens.profiles

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.formbuddy.android.data.local.db.entity.BusinessProfileEntity
import com.formbuddy.android.data.repository.ProfileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class BusinessProfileViewModel @Inject constructor(
    private val profileRepository: ProfileRepository
) : ViewModel() {

    private val _profile = MutableStateFlow<BusinessProfileEntity?>(null)
    val profile: StateFlow<BusinessProfileEntity?> = _profile

    fun loadProfile(id: String) {
        viewModelScope.launch {
            _profile.value = profileRepository.getBusinessProfileById(id)
        }
    }

    fun updateField(transform: BusinessProfileEntity.() -> BusinessProfileEntity) {
        val current = _profile.value ?: return
        val updated = current.transform()
        _profile.value = updated
        viewModelScope.launch {
            profileRepository.saveBusinessProfile(updated)
        }
    }

    fun deleteProfile() {
        val current = _profile.value ?: return
        viewModelScope.launch {
            profileRepository.deleteBusinessProfile(current)
        }
    }
}
