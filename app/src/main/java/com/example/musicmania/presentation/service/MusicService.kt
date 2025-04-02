package com.example.musicmania.presentation.service

import android.annotation.SuppressLint
import android.app.Activity
import android.app.Application
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Binder
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import android.widget.RemoteViews
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.example.musicmania.Constant
import com.example.musicmania.R
import com.example.musicmania.presentation.bottom_sheet.model.SongListDataModel
import com.example.musicmania.presentation.dashboard.SongsActivity

class MusicService : Service() {
    var mediaPlayer: MediaPlayer? = null
    private var currentSong: SongListDataModel? = null
    private var songList: ArrayList<SongListDataModel> = ArrayList()
    private var currentSongIndex = 0
    private val handler = Handler(Looper.getMainLooper())

//    private val binder = MusicBinder()

    private var notificationBuilder: NotificationCompat.Builder? = null
    private var isForegroundService = false
    private var remoteViews: RemoteViews? = null
    private var isAppInForeground = false

    private var currentVolume = 100 // Store volume as percentage
    private var audioManager: AudioManager? = null
    private var audioFocusRequest: AudioFocusRequest? = null
    private var hadAudioFocus = false

    private val afChangeListener = AudioManager.OnAudioFocusChangeListener { focusChange ->
        when (focusChange) {
            AudioManager.AUDIOFOCUS_LOSS -> {
                if (mediaPlayer?.isPlaying == true) {
                    mediaPlayer?.pause()
                    isPlaying = false
                    broadcastPlaybackState()
                    updateNotification()
                }
                hadAudioFocus = false
            }

            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                if (mediaPlayer?.isPlaying == true) {
                    mediaPlayer?.pause()
                    isPlaying = false
                    broadcastPlaybackState()
                    updateNotification()
                }
            }

            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                mediaPlayer?.setVolume(0.2f, 0.2f)
            }

            AudioManager.AUDIOFOCUS_GAIN -> {
                if (!isPlaying && hadAudioFocus) {
                    mediaPlayer?.start()
                    isPlaying = true
                    broadcastPlaybackState()
                    updateNotification()
                }
                mediaPlayer?.setVolume(1.0f, 1.0f)
            }

            AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK -> {
                mediaPlayer?.setVolume(1.0f, 1.0f)
            }

            AudioManager.AUDIOFOCUS_GAIN_TRANSIENT -> {
                if (!isPlaying && hadAudioFocus) {
                    mediaPlayer?.start()
                    isPlaying = true
                    broadcastPlaybackState()
                    updateNotification()
                }
                mediaPlayer?.setVolume(1.0f, 1.0f)
            }
        }
    }
    private var isPlaying = false


    override fun onBind(intent: Intent): IBinder {
        return MusicBinder()
    }

    inner class MusicBinder : Binder() {
        fun getService(): MusicService = this@MusicService
    }

    override fun onCreate() {
        super.onCreate()
        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            audioFocusRequest =
                AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN).setAudioAttributes(
                        AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA)
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build()
                    ).setAcceptsDelayedFocusGain(true)
                    .setOnAudioFocusChangeListener(afChangeListener).build()
        }

            createNotificationChannel()
            setupNotification()

        registerActivityLifecycleCallbacks()
    }

    private fun registerActivityLifecycleCallbacks() {
        val application = application as Application
        application.registerActivityLifecycleCallbacks(object :
            Application.ActivityLifecycleCallbacks {
            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}
            override fun onActivityStarted(activity: Activity) {}

            override fun onActivityResumed(activity: Activity) {
                if (activity is SongsActivity) {
                    isAppInForeground = true
                    requestAudioFocus()
                    updateNotification()
                }
            }

            override fun onActivityPaused(activity: Activity) {
                if (activity is SongsActivity) {
                    isAppInForeground = false
//                    abandonAudioFocus()
                    updateNotification()
                }
            }

            override fun onActivityStopped(activity: Activity) {}
            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
            override fun onActivityDestroyed(activity: Activity) {}
        })
    }

    private fun setupNotification() {
        if (!isForegroundService) {
            remoteViews = RemoteViews(packageName, R.layout.layout_custom_notification)
            notificationBuilder = createCustomNotification()
            startForeground(Constant.NOTIFICATION_ID, notificationBuilder?.build())
            isForegroundService = true
        }
    }

    fun pauseSong() {
        mediaPlayer?.pause()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            Constant.ACTION_PLAY_PAUSE -> togglePlayPause()
            Constant.ACTION_NEXT -> playNextSong()
            Constant.ACTION_PREVIOUS -> playPreviousSong()
            Constant.ACTION_SEEK -> {
                val seekPosition = intent.getIntExtra("seekPosition", 0)
                seekTo(seekPosition)
            }

            "ACTION_VOLUME_UP" -> adjustVolume(true)
            "ACTION_VOLUME_DOWN" -> adjustVolume(false)
            Constant.ACTION_INIT_SERVICE -> {
                songList = intent.getParcelableArrayListExtra("songList") ?: ArrayList()
                currentSongIndex = intent.getIntExtra("currentIndex", 0)
                val autoPlay = intent.getBooleanExtra("autoPlay", false)
                initializeService(autoPlay)
                broadcastPlaybackState()
            }

            "ACTION_STOP" -> {
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }

        return START_STICKY
    }
    fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                Constant.CHANNEL_ID, "Music Service Channel", NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Channel for Music Service"
                setShowBadge(true)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                setSound(null, null)
            }
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun updateNotificationFromActivity() {
        if (isForegroundService) {
            notificationBuilder = createCustomNotification()
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager?.notify(Constant.NOTIFICATION_ID, notificationBuilder?.build())
            createNotificationChannel()
            updateNotification()
        }
    }

    @SuppressLint("InflateParams", "RemoteViewLayout")
    private fun createCustomNotification(): NotificationCompat.Builder {
        val intent = Intent(this, SongsActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra("currentSongIndex", currentSongIndex)
            putExtra("isPlaying", mediaPlayer?.isPlaying ?: false)
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        Log.d("songIndex",currentSongIndex.toString())
//        val backgroundImage: Bitmap? = BitmapFactory.decodeResource(resources, R.drawable.app_logo)
        remoteViews?.apply {
            setTextViewText(R.id.notification_song_title, currentSong?.title ?: "Unknown")
            setTextViewText(R.id.notification_song_artist, currentSong?.artist ?: "Unknown Artist")
            setImageViewResource(R.id.ivSongImage, currentSong?.songThumbnail ?: R.drawable.ic_play)
            setImageViewResource(
                R.id.notification_play_pause,
                if (mediaPlayer?.isPlaying == true) R.drawable.ic_pause else R.drawable.ic_play
            )

            setOnClickPendingIntent(
                R.id.notification_play_pause, createActionPendingIntent(Constant.ACTION_PLAY_PAUSE)
            )
            setOnClickPendingIntent(
                R.id.notification_previous, createActionPendingIntent(Constant.ACTION_PREVIOUS)
            )
            setOnClickPendingIntent(
                R.id.notification_next, createActionPendingIntent(Constant.ACTION_NEXT)
            )

            mediaPlayer?.let {
                setProgressBar(R.id.notification_progress, it.duration, it.currentPosition, false)
                broadcastPlaybackState(it.isPlaying)
            }
        }


//        val deleteIntent = if (!isAppInForeground) {
//            Intent(this, MusicService::class.java).apply {
//                action = "ACTION_STOP"
//            }
//        } else null
//
//        val deletePendingIntent = if (deleteIntent != null) {
//            PendingIntent.getService(
//                this, 4, deleteIntent,
//                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
//            )
//        } else null

        return NotificationCompat.Builder(this, Constant.CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_play).setColor(getColor(R.color.white))
            .setStyle(NotificationCompat.DecoratedCustomViewStyle())
            .setCustomContentView(remoteViews).setContentIntent(pendingIntent)
            .setPriority(START_STICKY).setAllowSystemGeneratedContextualActions(true)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setCategory(NotificationCompat.CATEGORY_TRANSPORT).setShowWhen(false)
            .setAutoCancel(false).setOnlyAlertOnce(true).setOngoing(true)
            .setOngoing(isAppInForeground || mediaPlayer?.isPlaying == true)
    }

    private fun createActionPendingIntent(action: String): PendingIntent {
        val intent = Intent(this, MusicService::class.java).apply {
            this.action = action
        }
        return PendingIntent.getService(
            this, when (action) {
                Constant.ACTION_PLAY_PAUSE -> 0
                Constant.ACTION_NEXT -> 1
                Constant.ACTION_PREVIOUS -> 2
                else -> 3
            }, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun initializeService(autoPlay: Boolean = false) {
        if (songList.isNotEmpty() && currentSongIndex < songList.size) {
            currentSong = songList[currentSongIndex]
            mediaPlayer?.release()
            mediaPlayer = MediaPlayer().apply {
                setDataSource(currentSong?.subTitle)
                prepare()
                if (autoPlay) {
                    if (requestAudioFocus()) {
                        start()
                        this@MusicService.isPlaying = true
                    }
                }
                setOnCompletionListener {
                    playNextSong()
                }
                setOnPreparedListener {
                    broadcastProgress(0, duration)
                    broadcastPlaybackState()
                }
                setOnErrorListener { _, _, _ ->
                    broadcastPlaybackState()
                    true
                }
            }
            startProgressUpdates()
            updateNotification()
            broadcastPlaybackState()
        }
    }

    private fun playSong(song: SongListDataModel, autoPlay: Boolean = false) {
        currentSong = song
        mediaPlayer?.release()
        mediaPlayer = MediaPlayer().apply {
            setDataSource(song.subTitle)
            prepare()
            if (autoPlay || isPlaying) {
                if (requestAudioFocus()) {
                    start()

                    this@MusicService.isPlaying = true
                }
            }
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
                isPlaying = false
                abandonAudioFocus()
                startProgressUpdates()
                createNotificationChannel()
                setupNotification()
            } else {
                if (requestAudioFocus()) {
                    player.start()
                    isPlaying = true
                    startProgressUpdates()
                    setupNotification()
                }
            }
            broadcastPlaybackState()
            updateNotification()
        }
    }

    private fun playNextSong() {
        currentSongIndex = (currentSongIndex + 1) % songList.size
        playSong(songList[currentSongIndex], true)
        broadcastPlaybackState()
    }

    private fun playPreviousSong() {
        try {
            if (songList.isEmpty()) return
            currentSongIndex =
                if (currentSongIndex - 1 < 0) songList.size - 1 else currentSongIndex - 1
            playSong(songList[currentSongIndex], true)
            broadcastPlaybackState()
        } catch (e: Exception) {
            e.printStackTrace()
            if (songList.isNotEmpty()) {
                initializeService(true)
            }
        }
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
                        broadcastProgress(player.currentPosition, player.duration)
                        broadcastPlaybackState()
                        if (isForegroundService && remoteViews != null) {
                            remoteViews?.setProgressBar(
                                R.id.notification_progress,
                                player.duration,
                                player.currentPosition,
                                false
                            )
                            val notificationManager =
                                getSystemService(NotificationManager::class.java)
                            notificationManager?.notify(
                                Constant.NOTIFICATION_ID, notificationBuilder?.build()
                            )
                        }
                    }
                }
                handler.postDelayed(this, 1000)
            }
        })
    }

    private fun broadcastPlaybackState(rotation: Boolean = mediaPlayer?.isPlaying ?: false) {
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
            putExtra("currentPlayingPosition", currentSongIndex)
            putExtra("isCurrentlyPlaying", mediaPlayer?.isPlaying ?: false)
//            putExtra("rotation", mediaPlayer?.isPlaying ?: false)
            putExtra("rotation", rotation)
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

//    private fun prepareSong() {
//        mediaPlayer?.apply {
//            reset()
//            setDataSource(currentSong?.subTitle ?: return)
//            prepare()
//            // Don't start automatically
//            updateNotification()
//            broadcastSongChange()
//        }
//    }

    fun setVolume(volume: Int) {
        currentVolume = volume.coerceIn(0, 100)
        val volumeFloat = currentVolume / 100f
        mediaPlayer?.setVolume(volumeFloat, volumeFloat)
        broadcastVolumeChange()
    }

    fun adjustVolume(increase: Boolean) {
        val step = 1 // Adjust volume by 5%
        val newVolume = if (increase) {
            (currentVolume + step).coerceAtMost(100)
        } else {
            (currentVolume - step).coerceAtLeast(0)
        }
        setVolume(newVolume)
    }

    private fun broadcastVolumeChange() {
        val intent = Intent("VOLUME_CHANGED").apply {
            putExtra("volume", currentVolume)
        }
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent)
    }

    fun updateNotification() {
        if (isForegroundService && remoteViews != null) {
            remoteViews?.apply {
                setTextViewText(R.id.notification_song_title, currentSong?.title ?: "Unknown")
                setTextViewText(
                    R.id.notification_song_artist, currentSong?.artist ?: "Unknown Artist"
                )
                setImageViewResource(
                    R.id.ivSongImage, currentSong?.songThumbnail ?: R.drawable.ic_play
                )
                setImageViewResource(
                    R.id.notification_play_pause,
                    if (mediaPlayer?.isPlaying == true) R.drawable.ic_pause else R.drawable.ic_play
                )
                mediaPlayer?.let {
                    setProgressBar(R.id.notification_progress, it.duration, it.currentPosition, false)
                    broadcastPlaybackState(it.isPlaying)
                }
            }
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager?.notify(Constant.NOTIFICATION_ID, notificationBuilder?.build())
        }
    }

//    private fun broadcastSongChange() {
//        Intent().apply {
//            action = Constant.BROADCAST_PLAYBACK_STATE
//            `package` = packageName
//            flags = Intent.FLAG_RECEIVER_REGISTERED_ONLY
//            putExtra("currentIndex", currentSongIndex)
//            putExtra("title", currentSong?.title)
//            putExtra("artist", currentSong?.artist)
//            putExtra("thumbnail", currentSong?.songThumbnail)
//            putExtra("subTitle", currentSong?.artist)
//            putExtra("icon", currentSong?.icon)
//            sendBroadcast(this)
//            updateNotification()
//        }
//    }

    private fun requestAudioFocus(): Boolean {
        val result = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            audioFocusRequest?.let { request ->
                audioManager?.requestAudioFocus(request)
            } ?: AudioManager.AUDIOFOCUS_REQUEST_FAILED
        } else {
            @Suppress("DEPRECATION") audioManager?.requestAudioFocus(
                afChangeListener, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN
            )
        }
        hadAudioFocus = result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
        return hadAudioFocus
    }


    private fun abandonAudioFocus() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            audioFocusRequest?.let { request ->
                audioManager?.abandonAudioFocusRequest(request)
            }
        } else {
            @Suppress("DEPRECATION") audioManager?.abandonAudioFocus(afChangeListener)
        }
        hadAudioFocus = false
    }

    override fun stopService(name: Intent?): Boolean {
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
        return super.stopService(name)
    }

    override fun onDestroy() {
        super.onDestroy()
        abandonAudioFocus()
        handler.removeCallbacksAndMessages(null)
        mediaPlayer?.release()
        mediaPlayer = null
        stopSelf()
        stopService(Intent(this, MusicService::class.java))
    }
}
