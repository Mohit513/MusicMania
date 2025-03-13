package com.example.musicmania.utils

import android.content.ContentResolver
import android.database.Cursor
import android.provider.MediaStore
import android.util.Log
import com.example.musicmania.presentation.bottom_sheet.model.SongListDataModel

object SongUtils {

    fun getSongsFromDevice(contentResolver: ContentResolver): List<SongListDataModel> {
        val songItemList = mutableListOf<SongListDataModel>()
        val audioUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        val projection = arrayOf(
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.DATA,
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.ARTIST,
        )

        val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0"
        val sortOrder = "${MediaStore.Audio.Media.TITLE} ASC"

        val audioCursor: Cursor? = contentResolver.query(
            audioUri,
            projection,
            selection,
            null,
            sortOrder
        )

        audioCursor?.use {
            while (it.moveToNext()) {
                val title = it.getString(it.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE))
                val path = it.getString(it.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA))
                val artist = it.getString(it.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST))
                val audioList = SongListDataModel(
                    title = title,
                    subTitle = path,
                    artist = artist,
                    songThumbnail = com.example.musicmania.R.drawable.app_logo
                )
                songItemList.add(audioList)
                Log.d("this file", audioList.toString())
            }
        }

        return songItemList
    }
}
