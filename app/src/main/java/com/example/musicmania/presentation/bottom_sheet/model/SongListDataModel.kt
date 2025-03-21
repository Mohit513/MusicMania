package com.example.musicmania.presentation.bottom_sheet.model

import android.os.Parcel
import android.os.Parcelable
import com.example.musicmania.R


data class SongListDataModel(
    var title: String? = "",
    var subTitle: String? = "",
    var songThumbnail: Int? = null,
    var artist: String? = "",
    var length: String? = "",
    var icon: Int = R.drawable.ic_play,
    var isPlaying: Boolean = false
) : Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readString(),
        parcel.readString(),
        parcel.readValue(Int::class.java.classLoader) as? Int,
        parcel.readString(),
        parcel.readString(),
        parcel.readInt(),
        parcel.readByte() != 0.toByte()
    ) {
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(title)
        parcel.writeString(subTitle)
        parcel.writeValue(songThumbnail)
        parcel.writeString(artist)
        parcel.writeString(length)
        parcel.writeInt(icon)
        parcel.writeByte(if (isPlaying) 1 else 0)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<SongListDataModel> {
        override fun createFromParcel(parcel: Parcel): SongListDataModel {
            return SongListDataModel(parcel)
        }

        override fun newArray(size: Int): Array<SongListDataModel?> {
            return arrayOfNulls(size)
        }
    }
}
