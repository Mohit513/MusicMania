package com.example.musicmania.presentation.service


import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.media.MediaPlayer
import android.os.Binder
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import com.example.musicmania.Constant
import com.example.musicmania.R
import com.example.musicmania.presentation.dashboard.SongsActivity
import com.example.musicmania.presentation.bottom_sheet.model.SongListDataModel

class MusicService : Service() {
    var mediaPlayer: MediaPlayer? = null
    private var currentSong: SongListDataModel? = null
    private var songList: ArrayList<SongListDataModel> = ArrayList()
    private var currentSongIndex = 0
    private val handler = Handler(Looper.getMainLooper())

    private val binder = MusicBinder()

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    fun pauseSong() {
        mediaPlayer?.pause()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            Constant.ACTION_PLAY_PAUSE -> togglePlayPause()
            Constant.ACTION_NEXT -> playNextSong()
            Constant.ACTION_PREVIOUS -> playPreviousSong()
            Constant.ACTION_SEEK -> {
                val seekPosition = intent.getIntExtra("seekPosition", 0)
                seekTo(seekPosition)
            }
            Constant.ACTION_INIT_SERVICE -> {
                @Suppress("UNCHECKED_CAST")
                songList = intent.getParcelableArrayListExtra("songList") ?: ArrayList()
                currentSongIndex = intent.getIntExtra("currentIndex", 0)
                initializeService()
            }
        }
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent): IBinder {
        return MusicBinder()
    }

    inner class MusicBinder : Binder() {
        fun getService(): MusicService = this@MusicService
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                Constant.CHANNEL_ID,
                "Music Service Channel",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Channel for Music Service"
                setShowBadge(false)
                lockscreenVisibility = NotificationCompat.VISIBILITY_PUBLIC
            }
            channel.lockscreenVisibility = NotificationCompat.VISIBILITY_PUBLIC
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)


        }
    }

    @SuppressLint("InflateParams", "RemoteViewLayout")
    private fun createCustomNotification(): NotificationCompat.Builder{
        val intent = Intent(this,SongsActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra("currentSongIndex",currentSongIndex)
            putExtra("isPlaying",mediaPlayer?.isPlaying ?: false)
        }
        val pendingIntent = PendingIntent.getActivity(
            this,0,intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
//        val customView = LayoutInflater.from(this).inflate(R.layout.layout_custom_notification,null)
        val remoteViews = RemoteViews(packageName, R.layout.layout_custom_notification)

        remoteViews.setTextViewText(R.id.notification_song_title, currentSong?.title ?: "Unknown")
        remoteViews.setTextViewText(R.id.notification_song_artist, currentSong?.artist ?: "Unknown Artist")
        remoteViews.setImageViewResource(R.id.ivSongImage, currentSong?.songThumbnail ?: R.drawable.ic_play)
        remoteViews.setImageViewResource(R.id.notification_play_pause, if (mediaPlayer?.isPlaying == true) R.drawable.ic_pause else R.drawable.ic_play)

        remoteViews.setOnClickPendingIntent(R.id.notification_play_pause, createActionPendingIntent(Constant.ACTION_PLAY_PAUSE))
        remoteViews.setOnClickPendingIntent(R.id.notification_previous, createActionPendingIntent(Constant.ACTION_PREVIOUS))
        remoteViews.setOnClickPendingIntent(R.id.notification_next, createActionPendingIntent(Constant.ACTION_NEXT))
        mediaPlayer?.let {
            remoteViews.setProgressBar(R.id.notification_progress, it.duration, it.currentPosition, false)
        }


        return NotificationCompat.Builder(this, Constant.CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_play)
            .setColor(getColor(R.color.white))
            .setStyle(NotificationCompat.DecoratedCustomViewStyle())
            .setCustomContentView(remoteViews)
            .setOngoing(true)
            .setAutoCancel(false)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setCategory(NotificationCompat.CATEGORY_TRANSPORT)
            .setShowWhen(false)

    }


    private fun createActionPendingIntent(action: String): PendingIntent {
        val intent = Intent(this, MusicService::class.java).apply {
            this.action = action
        }
        return PendingIntent.getService(
            this, when(action) {
                Constant.ACTION_PLAY_PAUSE -> 0
                Constant.ACTION_NEXT -> 1
                Constant.ACTION_PREVIOUS -> 2
                else -> 3
            }, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun playSong(song: SongListDataModel) {
        currentSong = song
        mediaPlayer?.release()
        mediaPlayer = MediaPlayer().apply {
            setDataSource(song.subTitle)
            prepare()
            start()
            setOnCompletionListener {
                playNextSong()
            }
            setOnPreparedListener {
                broadcastProgress(0, duration)
                broadcastPlaybackState()
                startProgressUpdates()
            }
            setOnErrorListener { _, _, _ ->
                broadcastPlaybackState()
                true
            }
        }
        updateNotification()
        broadcastPlaybackState()
    }

    private fun togglePlayPause() {
        mediaPlayer?.let { player ->
            if (player.isPlaying) {
                player.pause()
            } else {
                player.start()
            }
            broadcastPlaybackState()
            updateNotification()
        }
    }
    private fun playNextSong() {
        currentSongIndex = (currentSongIndex + 1) % songList.size
        playSong(songList[currentSongIndex])
        broadcastPlaybackState()

    }

    private fun playPreviousSong() {
        currentSongIndex = if (currentSongIndex - 1 < 0) songList.size - 1 else currentSongIndex - 1
        playSong(songList[currentSongIndex])
        broadcastPlaybackState()
    }

    private fun seekTo(position: Int) {
        mediaPlayer?.let { player ->
            if (position >= 0 && position <= player.duration) {
                player.seekTo(position)
                broadcastProgress(player.currentPosition, player.duration)
                broadcastPlaybackState()
                updateNotification()
            }
        }
    }

    private fun startProgressUpdates() {
        handler.removeCallbacksAndMessages(null)
        handler.post(object : Runnable {
            override fun run() {
                mediaPlayer?.let { player ->
                    if (player.isPlaying) {
                        updateNotification()
                        broadcastPlaybackState()
                        broadcastProgress(player.currentPosition, player.duration)
                    }
                }
                handler.postDelayed(this, 1000)
            }
        })
    }

    private fun broadcastPlaybackState() {
        Intent().apply {
            action = Constant.BROADCAST_PLAYBACK_STATE
            `package` = packageName
            flags = Intent.FLAG_RECEIVER_REGISTERED_ONLY
            putExtra("isPlaying", mediaPlayer?.isPlaying ?: false)
            putExtra("currentIndex", currentSongIndex)
            putExtra("title", currentSong?.title)
            putExtra("artist", currentSong?.artist)
            putExtra("thumbnail", currentSong?.songThumbnail)
            putExtra("subTitle", currentSong?.artist)
            putExtra("icon", currentSong?.icon)
            putExtra("duration", mediaPlayer?.duration ?: 0)
            putExtra("currentPosition", mediaPlayer?.currentPosition ?: 0)
            sendBroadcast(this)
        }
    }

    private fun broadcastProgress(currentPosition: Int, duration: Int) {
        Intent().apply {
            action = Constant.BROADCAST_PROGRESS
            `package` = packageName
            flags = Intent.FLAG_RECEIVER_REGISTERED_ONLY
            putExtra("currentPosition", currentPosition)
            putExtra("duration", duration)
            sendBroadcast(this)
        }
    }

    private fun initializeService() {
        if (songList.isNotEmpty() && currentSongIndex < songList.size) {
            playSong(songList[currentSongIndex])
        }
    }

    private fun updateNotification() {
        startForeground(Constant.NOTIFICATION_ID, createCustomNotification().build())
    }

    override fun stopService(name: Intent?): Boolean {
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
        return super.stopService(name)
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacksAndMessages(null)
        mediaPlayer?.release()
        mediaPlayer = null
        stopSelf()
        stopService(Intent(this,MusicService::class.java))
    }
}
