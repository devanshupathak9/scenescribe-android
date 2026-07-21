package com.scenescribe.app.ui.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.scenescribe.app.data.api.ApiClient
import com.scenescribe.app.data.api.models.SubmitRequest
import com.scenescribe.app.data.api.models.TodayData
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed class HomeState {
    object Loading : HomeState()
    data class Success(val data: TodayData) : HomeState()
    data class Error(val message: String) : HomeState()
}

data class HomeUiState(
    val screenState: HomeState = HomeState.Loading,
    val descriptionText: String = "",
    val inputType: String = "keyboard",
    val isRecording: Boolean = false,
    val isSubmitting: Boolean = false,
    val submitError: String = ""
)

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val api = ApiClient.create(application)

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState

    init {
        loadToday()
    }

    fun loadToday() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(screenState = HomeState.Loading)
            try {
                val res = api.getToday()
                val data = res.data ?: throw Exception("No data returned")
                _uiState.value = _uiState.value.copy(screenState = HomeState.Success(data))
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    screenState = HomeState.Error(e.message ?: "Failed to load today's scene")
                )
            }
        }
    }

    fun updateDescription(text: String) {
        _uiState.value = _uiState.value.copy(
            descriptionText = text,
            inputType = "keyboard",
            submitError = ""
        )
    }

    fun appendSpeechResult(text: String) {
        _uiState.value = _uiState.value.copy(
            descriptionText = text,
            inputType = "microphone"
        )
    }

    fun setRecording(recording: Boolean) {
        _uiState.value = _uiState.value.copy(isRecording = recording)
    }

    fun submit() {
        val state = _uiState.value
        val currentData = (state.screenState as? HomeState.Success)?.data ?: return
        val text = state.descriptionText.trim()

        if (text.length < 10) {
            _uiState.value = state.copy(submitError = "Please write at least 10 characters describing the scene.")
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSubmitting = true, submitError = "")
            try {
                val res = api.submit(
                    SubmitRequest(
                        videoId      = currentData.video.videoId,
                        responseText = text,
                        inputType    = state.inputType
                    )
                )
                val submission = res.data ?: throw Exception("No submission data")
                val updatedData = currentData.copy(
                    status = "submitted",
                    submission = submission.copy(responseText = text)
                )
                _uiState.value = _uiState.value.copy(
                    screenState = HomeState.Success(updatedData),
                    isSubmitting = false
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    submitError = e.message ?: "Submission failed",
                    isSubmitting = false
                )
            }
        }
    }
}
