/*
 * Copyright (C) 2025 Vanilla Music contributors
 *
 * This file is part of Vanilla Music.
 *
 * Vanilla Music is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Vanilla Music is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Vanilla Music.  If not, see <https://www.gnu.org/licenses/>.
 */

package ch.blinkenlights.android.vanilla

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class MainUiState(
    val tracks: List<AudioTrack> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val nowPlaying: AudioTrack? = null,
    val isPlaying: Boolean = false
)

class MainViewModel(private val repository: MediaStoreAudioRepository) : ViewModel() {
    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    fun refreshAudio() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            runCatching { repository.loadAudioTracks() }
                .onSuccess { tracks ->
                    _uiState.update {
                        it.copy(
                            tracks = tracks,
                            isLoading = false,
                            errorMessage = null
                        )
                    }
                }
                .onFailure { throwable ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = throwable.localizedMessage ?: "Unknown error"
                        )
                    }
                }
        }
    }

    fun setNowPlaying(track: AudioTrack?) {
        _uiState.update { it.copy(nowPlaying = track) }
    }

    fun setIsPlaying(playing: Boolean) {
        _uiState.update { it.copy(isPlaying = playing) }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    companion object {
        fun provideFactory(repository: MediaStoreAudioRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
                        @Suppress("UNCHECKED_CAST")
                        return MainViewModel(repository) as T
                    }
                    throw IllegalArgumentException("Unknown ViewModel class: ${'$'}{modelClass.name}")
                }
            }
    }
}
