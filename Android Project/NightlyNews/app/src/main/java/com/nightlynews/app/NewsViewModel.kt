package com.nightlynews.app

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

sealed class UiState {
    object Loading : UiState()
    data class Success(val items: List<NewsItem>, val updatedAt: String) : UiState()
    data class Error(val message: String) : UiState()
}

class NewsViewModel : ViewModel() {

    private val _uiState = MutableLiveData<UiState>(UiState.Loading)
    val uiState: LiveData<UiState> = _uiState

    fun loadCategory(category: String) {
        _uiState.value = UiState.Loading
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val items = NewsRepository.fetchCategory(category)
                _uiState.postValue(
                    if (items.isEmpty()) UiState.Error("No news found. Pull down to refresh.")
                    else UiState.Success(items, currentTime())
                )
            } catch (e: Exception) {
                _uiState.postValue(UiState.Error("Could not load news.\nCheck your internet connection."))
            }
        }
    }

    fun loadState(state: String) {
        _uiState.value = UiState.Loading
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val items = NewsRepository.fetchState(state)
                _uiState.postValue(
                    if (items.isEmpty()) UiState.Error("No news found for $state.\nPull down to refresh.")
                    else UiState.Success(items, currentTime())
                )
            } catch (e: Exception) {
                _uiState.postValue(UiState.Error("Could not load news.\nCheck your internet connection."))
            }
        }
    }

    private fun currentTime(): String {
        val fmt = java.text.SimpleDateFormat("h:mm a", java.util.Locale.getDefault())
        return "Updated ${fmt.format(java.util.Date())}"
    }
}
