package com.example.musicmania.presentation.bottom_sheet.model

import com.example.musicmania.R
import java.io.Serializable

data class SongListDataModel(
    var title: String? = "ok",
    var subTitle: String? = "",
    var songThumbnail: Int? = null,
    val artist: String? = "",
    val length : String? = "",
    var icon : Int = R.drawable.ic_pause
) : Serializable
