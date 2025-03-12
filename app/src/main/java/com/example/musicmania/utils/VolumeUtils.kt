package com.example.musicmania.utils

import android.content.Context
import android.media.AudioManager
import android.widget.TextView
import com.example.musicmania.R

object VolumeUtils {

    fun updateDeviceVolume(audioManager: AudioManager, volumeText: TextView) {
        val deviceVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
        val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        val volumePercentage = (deviceVolume / maxVolume.toFloat()) * 100
        volumeText.text = String.format("Volume: %.0f%%", volumePercentage)
    }

    fun getColorFromVolume(volumePercentage: Float, context: Context): Int {
        return when {
            volumePercentage < 25 -> context.resources.getColor(R.color.pomegranate, context.theme)
            volumePercentage in 25.0..75.0 -> context.resources.getColor(R.color.pomegranate, context.theme)
            else -> context.resources.getColor(R.color.pomegranate, context.theme) // High volume
        }
    }
}
