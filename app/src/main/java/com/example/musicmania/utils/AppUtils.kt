package com.example.musicmania.utils

import android.content.Context
import android.widget.Toast
import java.util.concurrent.TimeUnit

object AppUtils {
    fun showToast(context: Context, message: String, duration: Int = Toast.LENGTH_SHORT) {
        Toast.makeText(context, message, duration).show()
    }

    fun formatTime(milliseconds: Int): String {
        val minutes = TimeUnit.MILLISECONDS.toMinutes(milliseconds.toLong())
        val seconds = TimeUnit.MILLISECONDS.toSeconds(milliseconds.toLong()) % 60
        return String.format("%02d:%02d", minutes, seconds)
    }

    fun formatDuration(duration: Int): String {
        val minutes = (duration / 1000) / 60
        val seconds = (duration / 1000) % 60
        return String.format("%02d:%02d", minutes, seconds)
    }

    fun isMediaPlayable(path: String?): Boolean {
        return !path.isNullOrEmpty()
    }

    fun getProgressPercentage(currentDuration: Long, totalDuration: Long): Int {
        return if (totalDuration > 0) {
            ((currentDuration.toFloat() / totalDuration) * 100).toInt()
        } else 0
    }

    fun progressToTimer(progress: Int, totalDuration: Int): Int {
        return (totalDuration * progress) / 100
    }

    fun getVolumePercentage(volume: Float): String {
        return "${(volume * 100).toInt()}%"
    }
}
