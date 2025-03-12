package com.example.musicmania.presentation.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.MediaPlayer
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.musicmania.R
import com.example.musicmania.presentation.bottom_sheet.model.SongListDataModel

class MusicService : Service() {

    private lateinit var mediaPlayer: MediaPlayer
    private var currentSong: SongListDataModel? = null
    private val channelId = "music_channel"

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent?.let {
            currentSong = it.getSerializableExtra("song") as SongListDataModel
            playSong()
        }
        return START_NOT_STICKY
    }

    private fun playSong() {
        currentSong?.let {
            mediaPlayer = MediaPlayer().apply {
                setDataSource(it.subTitle)  // Use the path or URI of the song
                prepareAsync()
                setOnPreparedListener { mp ->
                    mp.start()
                    showNotification()
                }
            }
        }
    }

    private fun showNotification() {
        val notification = buildNotification()
        startForeground(1, notification)
    }

    private fun buildNotification(): Notification {
        val playPauseAction = NotificationCompat.Action(
            R.drawable.ic_play, "Pause", getPlayPausePendingIntent()
        )

        return NotificationCompat.Builder(this, channelId)
            .setContentTitle(currentSong?.title)
            .setContentText(currentSong?.subTitle)
            .setSmallIcon(R.drawable.app_logo)
            .addAction(playPauseAction)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun getPlayPausePendingIntent(): PendingIntent {
        val actionIntent = Intent(this, MusicService::class.java).apply {
            action = "PLAY_PAUSE"
        }
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            PendingIntent.getService(this, 0, actionIntent, PendingIntent.FLAG_IMMUTABLE)
        } else {
            PendingIntent.getService(this, 0, actionIntent, PendingIntent.FLAG_IMMUTABLE)
        }
    }


    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Music Channel"
            val descriptionText = "Notification channel for music playback"
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(channelId, name, importance).apply {
                description = descriptionText
            }
            val notificationManager: NotificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    override fun onBind(intent: Intent?) = null

    override fun onDestroy() {
        mediaPlayer.release()
        super.onDestroy()
    }
}
