package com.formbuddy.android.ui.screens.profiles

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavHostController
import com.formbuddy.android.data.model.Profile
import com.formbuddy.android.data.repository.ProfileRepository
import com.formbuddy.android.ui.navigation.Screen
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProfilesViewModel @Inject constructor(
    private val profileRepository: ProfileRepository
) : ViewModel() {

    val profiles: StateFlow<List<Profile>> = profileRepository.getAllProfiles()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun createPersonalProfile(navController: NavHostController) {
        viewModelScope.launch {
            val profile = profileRepository.createPersonalProfile()
            navController.navigate(Screen.Profile.createRoute(profile.id, false))
        }
    }

    fun addFamilyProfile() {
        viewModelScope.launch {
            profileRepository.createFamilyProfile()
        }
    }

    fun addBusinessProfile() {
        viewModelScope.launch {
            profileRepository.createBusinessProfile()
        }
    }
}
