package com.scenescribe.app.ui.feedback

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.scenescribe.app.data.api.ApiClient
import com.scenescribe.app.data.api.models.FeedbackDetailData
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed class FeedbackState {
    object Loading : FeedbackState()
    data class Success(val data: FeedbackDetailData) : FeedbackState()
    data class Error(val message: String) : FeedbackState()
}

class FeedbackViewModel(application: Application) : AndroidViewModel(application) {

    private val api = ApiClient.create(application)

    private val _state = MutableStateFlow<FeedbackState>(FeedbackState.Loading)
    val state: StateFlow<FeedbackState> = _state

    fun load(submissionId: String) {
        viewModelScope.launch {
            _state.value = FeedbackState.Loading
            try {
                val res = api.getFeedbackDetail(submissionId)
                val data = res.data ?: throw Exception("No data returned")
                _state.value = FeedbackState.Success(data)
            } catch (e: Exception) {
                _state.value = FeedbackState.Error(e.message ?: "Failed to load feedback")
            }
        }
    }
}
