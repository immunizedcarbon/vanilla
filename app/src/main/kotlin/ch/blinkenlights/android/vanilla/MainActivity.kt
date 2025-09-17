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

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import ch.blinkenlights.android.vanilla.databinding.ActivityMainBinding
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var player: ExoPlayer

    private val repository by lazy { MediaStoreAudioRepository(contentResolver) }

    private val viewModel: MainViewModel by viewModels {
        MainViewModel.provideFactory(repository)
    }

    private val playerListener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            viewModel.setIsPlaying(isPlaying)
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            if (playbackState == Player.STATE_ENDED) {
                viewModel.setIsPlaying(false)
            }
        }
    }

    private val permissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) {
                viewModel.refreshAudio()
            } else {
                Toast.makeText(this, R.string.permission_denied, Toast.LENGTH_LONG).show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)

        player = ExoPlayer.Builder(this)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(C.USAGE_MEDIA)
                    .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                    .build(),
                true
            )
            .build()
        player.addListener(playerListener)

        val adapter = TracksAdapter(::startPlayback)
        binding.trackList.adapter = adapter

        binding.refreshButton.setOnClickListener { requestContent() }
        binding.playPauseButton.setOnClickListener { togglePlayback() }
        binding.stopButton.setOnClickListener { stopPlayback() }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    adapter.submitList(state.tracks) {
                        adapter.updateSelection(state.nowPlaying?.id)
                    }
                    binding.loadingIndicator.isVisible = state.isLoading
                    binding.emptyView.isVisible = state.tracks.isEmpty() && !state.isLoading
                    binding.refreshButton.isEnabled = !state.isLoading

                    val hasSelection = state.nowPlaying != null
                    binding.nowPlayingGroup.isVisible = hasSelection
                    binding.playPauseButton.isEnabled = hasSelection
                    binding.stopButton.isEnabled = hasSelection

                    binding.nowPlayingTitle.text =
                        state.nowPlaying?.title ?: getString(R.string.no_track_selected)
                    binding.nowPlayingSubtitle.text = state.nowPlaying?.let {
                        getString(
                            R.string.track_subtitle_format,
                            it.artist,
                            it.album,
                            formatDuration(it.duration)
                        )
                    } ?: ""

                    binding.playPauseButton.text =
                        if (state.isPlaying) getString(R.string.pause) else getString(R.string.play)

                    state.errorMessage?.let { message ->
                        Toast.makeText(this@MainActivity, message, Toast.LENGTH_LONG).show()
                        viewModel.clearError()
                    }
                }
            }
        }

        if (hasAudioPermission()) {
            viewModel.refreshAudio()
        } else {
            permissionLauncher.launch(Manifest.permission.READ_MEDIA_AUDIO)
        }
    }

    override fun onStop() {
        super.onStop()
        if (!isChangingConfigurations) {
            player.pause()
            viewModel.setIsPlaying(false)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        player.removeListener(playerListener)
        player.release()
    }

    private fun startPlayback(track: AudioTrack) {
        runCatching {
            val mediaItem = MediaItem.Builder()
                .setUri(track.uri)
                .setMediaId(track.id.toString())
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setTitle(track.title)
                        .setArtist(track.artist)
                        .setAlbumTitle(track.album)
                        .build()
                )
                .build()

            player.setMediaItem(mediaItem)
            player.prepare()
            player.play()
            viewModel.setNowPlaying(track)
        }.onFailure {
            Toast.makeText(this, R.string.playback_error, Toast.LENGTH_LONG).show()
        }
    }

    private fun togglePlayback() {
        when {
            player.isPlaying -> player.pause()
            player.mediaItemCount > 0 -> player.play()
        }
    }

    private fun stopPlayback() {
        player.stop()
        player.clearMediaItems()
        viewModel.setNowPlaying(null)
        viewModel.setIsPlaying(false)
    }

    private fun requestContent() {
        if (hasAudioPermission()) {
            viewModel.refreshAudio()
        } else {
            permissionLauncher.launch(Manifest.permission.READ_MEDIA_AUDIO)
        }
    }

    private fun hasAudioPermission(): Boolean =
        ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.READ_MEDIA_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
}
