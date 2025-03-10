package com.example.musicmania.presentation.bottom_sheet.model

data class SongListDataModel(
    var title: String? = "ok",
    var subTitle: String? = "",
    var songThumbnail: Int? = null,
    val artist: String? = "",
    val length : String? = ""
)
