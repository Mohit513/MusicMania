package com.example.musicmania.utils

import android.content.ContentResolver
import android.provider.MediaStore
import com.example.musicmania.R
import com.example.musicmania.presentation.bottom_sheet.model.SongListDataModel

object SongUtils {
    fun getSongsFromDevice(contentResolver: ContentResolver): ArrayList<SongListDataModel> {
        val songList = ArrayList<SongListDataModel>()
        val uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        val projection = arrayOf(
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.DATA,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.ALBUM_ID
        )
        val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0"
        val sortOrder = "${MediaStore.Audio.Media.TITLE} ASC"

        contentResolver.query(uri, projection, selection, null, sortOrder)?.use { cursor ->
            val titleColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val artistColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
            val pathColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)
            val durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
            val albumId = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)


            while (cursor.moveToNext()) {
                val title = cursor.getString(titleColumn)
                val artist = cursor.getString(artistColumn)
                val path = cursor.getString(pathColumn)
                val duration = cursor.getLong(durationColumn)
                val album = cursor.getInt(albumId)
                
                val minutes = duration / 1000 / 60
                val seconds = duration / 1000 % 60
                val length = String.format("%02d:%02d", minutes, seconds)

                songList.add(
                    SongListDataModel(
                        title = title ?: "Unknown",
                        artist = artist ?: "Unknown Artist",
                        subTitle = path,
                        songThumbnail =  R.drawable.app_logo,
                        icon = R.drawable.ic_play,
                        length = length,
                        isPlaying = false
                    )
                )
            }
        }

        return songList
    }
}
