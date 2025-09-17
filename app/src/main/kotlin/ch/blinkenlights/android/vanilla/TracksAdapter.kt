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

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import ch.blinkenlights.android.vanilla.databinding.ItemTrackBinding

class TracksAdapter(
    private val onTrackSelected: (AudioTrack) -> Unit
) : ListAdapter<AudioTrack, TracksAdapter.TrackViewHolder>(DiffCallback) {

    private var selectedTrackId: Long? = null

    fun updateSelection(trackId: Long?) {
        if (selectedTrackId != trackId) {
            selectedTrackId = trackId
            notifyDataSetChanged()
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TrackViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = ItemTrackBinding.inflate(inflater, parent, false)
        return TrackViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TrackViewHolder, position: Int) {
        val item = getItem(position)
        holder.bind(item, item.id == selectedTrackId)
    }

    inner class TrackViewHolder(
        private val binding: ItemTrackBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        init {
            binding.root.setOnClickListener {
                val track = bindingAdapterPosition.takeIf { it != RecyclerView.NO_POSITION }?.let(::getItem)
                    ?: return@setOnClickListener
                onTrackSelected(track)
            }
        }

        fun bind(track: AudioTrack, isSelected: Boolean) {
            val context = binding.root.context
            binding.trackTitle.text = track.title
            binding.trackSubtitle.text = context.getString(
                R.string.track_subtitle_format,
                track.artist,
                track.album,
                formatDuration(track.duration)
            )
            binding.root.isSelected = isSelected
            binding.root.alpha = if (isSelected) 0.8f else 1f
        }
    }

    private companion object {
        private val DiffCallback = object : DiffUtil.ItemCallback<AudioTrack>() {
            override fun areItemsTheSame(oldItem: AudioTrack, newItem: AudioTrack): Boolean =
                oldItem.id == newItem.id

            override fun areContentsTheSame(oldItem: AudioTrack, newItem: AudioTrack): Boolean =
                oldItem == newItem
        }
    }
}
