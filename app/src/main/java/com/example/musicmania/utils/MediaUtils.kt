package com.example.musicmania.utils

import android.media.MediaPlayer

object MediaUtils {

    fun setUpMediaPlayer(
        mediaPlayer: MediaPlayer?,
        currentSongSubtitle: String?,
        onPreparedListener: MediaPlayer.OnPreparedListener
    ) {
        mediaPlayer?.apply {
            reset()
            setDataSource(currentSongSubtitle ?: "")
            prepareAsync()
            setOnPreparedListener(onPreparedListener)
        }
    }

    fun playSong(mediaPlayer: MediaPlayer, handler: android.os.Handler, updateSeekBarRunnable: Runnable, updateCurrentTimeRunnable: Runnable) {
        mediaPlayer.start()
        handler.post(updateSeekBarRunnable)
        handler.post(updateCurrentTimeRunnable)
    }

    fun pauseSong(mediaPlayer: MediaPlayer, handler: android.os.Handler, updateSeekBarRunnable: Runnable, updateCurrentTimeRunnable: Runnable) {
        mediaPlayer.pause()
        handler.removeCallbacks(updateSeekBarRunnable)
        handler.removeCallbacks(updateCurrentTimeRunnable)
    }
}
