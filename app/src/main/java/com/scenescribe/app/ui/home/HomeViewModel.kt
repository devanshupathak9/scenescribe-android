package com.scenescribe.app.ui.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.scenescribe.app.data.api.ApiClient
import com.scenescribe.app.data.api.models.SceneItem
import com.scenescribe.app.data.api.models.SubmitRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed class HomeState {
    object Loading : HomeState()
    data class Success(val scenes: List<SceneItem>) : HomeState()
    data class Error(val message: String) : HomeState()
}

data class SceneInputState(
    val text: String = "",
    val inputType: String = "keyboard",
    val isRecording: Boolean = false,
    val isSubmitting: Boolean = false,
    val submitError: String = ""
)

data class HomeUiState(
    val screenState: HomeState = HomeState.Loading,
    val sceneInputs: Map<String, SceneInputState> = emptyMap()
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
                val scenes = res.data?.scenes ?: throw Exception("No scenes returned")
                _uiState.value = _uiState.value.copy(screenState = HomeState.Success(scenes))
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    screenState = HomeState.Error(e.message ?: "Failed to load today's scenes")
                )
            }
        }
    }

    fun updateDescription(videoId: String, text: String) {
        mutateInput(videoId) { it.copy(text = text, inputType = "keyboard", submitError = "") }
    }

    fun appendSpeechResult(videoId: String, text: String) {
        mutateInput(videoId) { it.copy(text = text, inputType = "microphone") }
    }

    fun setRecording(videoId: String, recording: Boolean) {
        mutateInput(videoId) { it.copy(isRecording = recording) }
    }

    fun stopAllRecording() {
        val updated = _uiState.value.sceneInputs.mapValues { (_, v) -> v.copy(isRecording = false) }
        _uiState.value = _uiState.value.copy(sceneInputs = updated)
    }

    fun submit(videoId: String) {
        val scenes = (_uiState.value.screenState as? HomeState.Success)?.scenes ?: return
        val input = _uiState.value.sceneInputs[videoId] ?: SceneInputState()
        val text = input.text.trim()

        if (text.length < 10) {
            mutateInput(videoId) { it.copy(submitError = "Please write at least 10 characters.") }
            return
        }

        viewModelScope.launch {
            mutateInput(videoId) { it.copy(isSubmitting = true, submitError = "") }
            try {
                val res = api.submit(
                    SubmitRequest(videoId = videoId, responseText = text, inputType = input.inputType)
                )
                val submission = res.data ?: throw Exception("No submission data")
                val updatedScenes = scenes.map { scene ->
                    if (scene.video.videoId == videoId)
                        scene.copy(status = "submitted", submission = submission.copy(responseText = text))
                    else scene
                }
                _uiState.value = _uiState.value.copy(
                    screenState = HomeState.Success(updatedScenes)
                )
                mutateInput(videoId) { it.copy(isSubmitting = false) }
            } catch (e: Exception) {
                mutateInput(videoId) {
                    it.copy(isSubmitting = false, submitError = e.message ?: "Submission failed")
                }
            }
        }
    }

    private fun mutateInput(videoId: String, transform: (SceneInputState) -> SceneInputState) {
        val map = _uiState.value.sceneInputs.toMutableMap()
        map[videoId] = transform(map[videoId] ?: SceneInputState())
        _uiState.value = _uiState.value.copy(sceneInputs = map)
    }
}
