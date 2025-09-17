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

import android.content.ContentResolver
import android.provider.MediaStore
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MediaStoreAudioRepository(
    private val contentResolver: ContentResolver,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {
    suspend fun loadAudioTracks(): List<AudioTrack> = withContext(ioDispatcher) {
        val tracks = mutableListOf<AudioTrack>()
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.DURATION
        )
        val selection = "${MediaStore.Audio.Media.IS_MUSIC}!=0"
        val sortOrder = "${MediaStore.Audio.Media.TITLE} COLLATE NOCASE ASC"
        val queryUri = MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)

        contentResolver.query(queryUri, projection, selection, null, sortOrder)?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val titleColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val artistColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
            val albumColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
            val durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idColumn)
                val title = cursor.getString(titleColumn).orPlaceholder(DEFAULT_TITLE)
                val artist = cursor.getString(artistColumn).orPlaceholder(DEFAULT_ARTIST)
                val album = cursor.getString(albumColumn).orPlaceholder(DEFAULT_ALBUM)
                val duration = cursor.getLong(durationColumn)
                val uri = MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL, id)
                tracks += AudioTrack(id, title, artist, album, duration, uri)
            }
        }

        tracks
    }

    private fun String?.orPlaceholder(defaultValue: String): String =
        if (this.isNullOrBlank()) defaultValue else this

    private companion object {
        const val DEFAULT_TITLE = "Unknown title"
        const val DEFAULT_ARTIST = "Unknown artist"
        const val DEFAULT_ALBUM = "Unknown album"
    }
}
