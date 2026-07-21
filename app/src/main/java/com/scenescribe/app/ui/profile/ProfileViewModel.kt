package com.scenescribe.app.ui.profile

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.scenescribe.app.data.api.ApiClient
import com.scenescribe.app.data.api.models.HistoryItemDto
import com.scenescribe.app.data.api.models.HistoryMeta
import com.scenescribe.app.data.api.models.ProfileData
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed class ProfileState {
    object Loading : ProfileState()
    data class Success(val data: ProfileData) : ProfileState()
    data class Error(val message: String) : ProfileState()
}

data class HistoryUiState(
    val items: List<HistoryItemDto> = emptyList(),
    val meta: HistoryMeta? = null,
    val currentPage: Int = 1,
    val isLoading: Boolean = false
)

class ProfileViewModel(application: Application) : AndroidViewModel(application) {

    private val api = ApiClient.create(application)

    private val _profileState = MutableStateFlow<ProfileState>(ProfileState.Loading)
    val profileState: StateFlow<ProfileState> = _profileState

    private val _historyState = MutableStateFlow(HistoryUiState(isLoading = true))
    val historyState: StateFlow<HistoryUiState> = _historyState

    init {
        loadProfile()
        loadHistory(page = 1)
    }

    private fun loadProfile() {
        viewModelScope.launch {
            _profileState.value = ProfileState.Loading
            try {
                val res = api.getProfile()
                val data = res.data ?: throw Exception("No profile data")
                _profileState.value = ProfileState.Success(data)
            } catch (e: Exception) {
                _profileState.value = ProfileState.Error(e.message ?: "Failed to load profile")
            }
        }
    }

    fun loadHistory(page: Int) {
        viewModelScope.launch {
            _historyState.value = _historyState.value.copy(isLoading = true)
            try {
                val res = api.getHistory(page)
                _historyState.value = HistoryUiState(
                    items = res.data ?: emptyList(),
                    meta = res.meta,
                    currentPage = page,
                    isLoading = false
                )
            } catch (_: Exception) {
                _historyState.value = _historyState.value.copy(isLoading = false)
            }
        }
    }

    fun nextPage() {
        val state = _historyState.value
        val totalPages = state.meta?.pages ?: 1
        if (state.currentPage < totalPages) {
            loadHistory(state.currentPage + 1)
        }
    }

    fun prevPage() {
        val state = _historyState.value
        if (state.currentPage > 1) {
            loadHistory(state.currentPage - 1)
        }
    }
}
