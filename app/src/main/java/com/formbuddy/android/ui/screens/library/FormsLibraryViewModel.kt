package com.formbuddy.android.ui.screens.library

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.formbuddy.android.data.repository.FormsLibraryRepository
import com.formbuddy.android.data.repository.LibraryFormReference
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

@HiltViewModel
class FormsLibraryViewModel @Inject constructor(
    private val formsLibraryRepository: FormsLibraryRepository,
    private val context: Context
) : ViewModel() {

    private val _forms = MutableStateFlow<List<LibraryFormReference>>(emptyList())
    val forms: StateFlow<List<LibraryFormReference>> = _forms

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    init {
        search("")
    }

    fun search(query: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                _forms.value = formsLibraryRepository.searchForms(query, reset = true)
            } catch (_: Exception) {
                _forms.value = emptyList()
            }
            _isLoading.value = false
        }
    }

    fun downloadAndOpen(form: LibraryFormReference, onReady: (String) -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val data = formsLibraryRepository.downloadFormDocument(form.documentUrl)
                val file = File(context.cacheDir, "${form.id}.pdf")
                file.writeBytes(data)
                onReady(file.toURI().toString())
            } catch (_: Exception) {}
            _isLoading.value = false
        }
    }
}
