package com.example.musicmania.presentation.dashboard

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.media.AudioManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.PatternMatcher
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import com.example.musicmania.R
import com.example.musicmania.databinding.ActivitySongsBinding
import com.example.musicmania.presentation.bottom_sheet.SongListBottomSheetFragment
import com.example.musicmania.presentation.bottom_sheet.model.SongListDataModel
import com.example.musicmania.presentation.service.MusicService
import com.example.musicmania.utils.SongUtils
import com.google.android.material.slider.Slider

// Define the ProgressUpdateCallback interface
interface ProgressUpdateCallback {
    fun onProgressUpdated(progress: Float)
}

class SongsActivity : AppCompatActivity(), SongListBottomSheetFragment.SongListListener {
    private lateinit var binding: ActivitySongsBinding
    private lateinit var volumeText: TextView
    private lateinit var audioManager: AudioManager
    private lateinit var handler: Handler
    private var currentSong = SongListDataModel()
    private var songList: ArrayList<SongListDataModel> = arrayListOf()
    private var currentSongIndex = 0
    private var isPlaying = false

    private var musicService: MusicService? = null
    private var isBound = false

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as MusicService.MusicBinder
            musicService = binder.getService()
            isBound = true
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            musicService = null
            isBound = false
        }
    }

    private val playbackReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                MusicService.BROADCAST_PLAYBACK_STATE -> {
                    isPlaying = intent.getBooleanExtra("isPlaying", false)
                    currentSongIndex = intent.getIntExtra("currentIndex", currentSongIndex)
                    val title = intent.getStringExtra("title")
                    val artist = intent.getStringExtra("artist")
                    val thumbnail = intent.getIntExtra("thumbnail", R.drawable.ic_play)
                    val subTitle = intent.getStringExtra("subTitle")
                    val icon = intent.getIntExtra("icon", R.drawable.ic_play)
                    val duration = intent.getIntExtra("duration", 0)
                    val currentPosition = intent.getIntExtra("currentPosition", 0)

                    updatePlaybackState(isPlaying)
                    updateSongInfo(title, artist, thumbnail, subTitle, icon)
                    updateProgress(currentPosition, duration) { _ ->
                        // Progress is handled in updateProgress
                    }
                }
                MusicService.BROADCAST_PROGRESS -> {
                    val currentPosition = intent.getIntExtra("currentPosition", 0)
                    val duration = intent.getIntExtra("duration", 0)
                    updateProgress(currentPosition, duration) { _ ->
                        // Progress is handled in updateProgress
                    }
                }
            }
        }
    }

    private val volumeReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == AudioManager.ACTION_AUDIO_BECOMING_NOISY) {
                pauseSong()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySongsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        volumeText = binding.tvVolumePercentage
        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager

        setUpStatusBar()
        setUpListeners()
        setUpView()

        // Handle notification click
        intent?.let { handleNotificationIntent(it) }
    }

    override fun onNewIntent(intent: Intent) {
        if (intent != null) {
            super.onNewIntent(intent)
        }
        intent?.let { handleNotificationIntent(it) }
    }

    private fun handleNotificationIntent(intent: Intent) {
        val index = intent.getIntExtra("currentSongIndex", -1)
        val isPlaying = intent.getBooleanExtra("isPlaying", false)
        if (index != -1) {
            currentSongIndex = index
            updatePlaybackState(isPlaying)
            musicService?.let {
                if (it.mediaPlayer?.isPlaying != isPlaying) {
                    sendServiceCommand(MusicService.ACTION_PLAY_PAUSE)
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        updateDeviceVolume()
        registerReceivers()

        // Bind to music service
        Intent(this, MusicService::class.java).also { intent ->
            bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
        }

        // Start progress updates
        handler = Handler(Looper.getMainLooper())
        startProgressUpdates()
    }

    override fun onPause() {
        super.onPause()
        try {
            unregisterReceiver(volumeReceiver)
            unregisterReceiver(playbackReceiver)
        } catch (e: IllegalArgumentException) {
            // Receiver not registered
            e.printStackTrace()
        }

        // Clean up resources
        handler.removeCallbacksAndMessages(null)
        if (isBound) {
            unbindService(serviceConnection)
            isBound = false
        }
    }

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    private fun registerReceivers() {
        try {
            // System broadcast for audio becoming noisy
            val volumeFilter = IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY)
            
            // App-specific broadcasts for music playback
            val playbackFilter = IntentFilter().apply {
                addAction(MusicService.BROADCAST_PLAYBACK_STATE)
                addAction(MusicService.BROADCAST_PROGRESS)
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                // System broadcast needs to be exported since it's from AudioManager
                registerReceiver(
                    volumeReceiver,
                    volumeFilter,
                    Context.RECEIVER_EXPORTED
                )
                
                // Our music service broadcasts should not be exported
                registerReceiver(
                    playbackReceiver,
                    playbackFilter,
                    Context.RECEIVER_NOT_EXPORTED
                )
            } else {
                registerReceiver(volumeReceiver, volumeFilter)
                registerReceiver(playbackReceiver, playbackFilter)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun setUpListeners() {
        binding.apply {
            tvOpenSongList.setOnClickListener {
                val bottomSheetFragment = SongListBottomSheetFragment(songList)
                bottomSheetFragment.show(supportFragmentManager, "songList")
            }

            ivSongPlay.setOnClickListener {
                sendServiceCommand(MusicService.ACTION_PLAY_PAUSE)
            }

            ivPlayForward.setOnClickListener {
                sendServiceCommand(MusicService.ACTION_NEXT)
            }

            ivPlayback.setOnClickListener {
                sendServiceCommand(MusicService.ACTION_PREVIOUS)
            }

            layoutSongProgress.seekBar.addOnSliderTouchListener(object : Slider.OnSliderTouchListener {
                override fun onStartTrackingTouch(slider: Slider) {
                    // Pause updates while seeking
                    handler.removeCallbacksAndMessages(null)
                }

                override fun onStopTrackingTouch(slider: Slider) {
                    val position = slider.value.toInt()
                    sendServiceCommand(MusicService.ACTION_SEEK, position)
                    // Resume updates after seeking
                    startProgressUpdates()
                }
            })

            layoutSongProgress.seekBar.addOnChangeListener { _, value, fromUser ->
                if (fromUser) {
                    sendServiceCommand(MusicService.ACTION_SEEK, value.toInt())
                }
            }
        }
    }

    private fun startProgressUpdates() {
        handler.postDelayed(object : Runnable {
            override fun run() {
                musicService?.mediaPlayer?.let { player ->
                    if (player.isPlaying) {
                        updateProgress(player.currentPosition, player.duration) { _ ->
                            // Progress is handled in updateProgress
                        }
                    }
                }
                handler.postDelayed(this, 1000)
            }
        }, 1000)
    }

    private fun broadcastProgress(currentPosition: Int, duration: Int) {
        Intent(MusicService.BROADCAST_PROGRESS).apply {
            setPackage(packageName)
            putExtra("currentPosition", currentPosition)
            putExtra("duration", duration)
            sendBroadcast(this)
        }
    }

    private fun setUpView() {
        songList.addAll(SongUtils.getSongsFromDevice(contentResolver))
        if (songList.isNotEmpty()) {
            currentSong = songList[currentSongIndex]
            updateSongInfo(
                currentSong.title,
                currentSong.artist,
                currentSong.songThumbnail,
                currentSong.subTitle,
                currentSong.icon
            )
            initializeService()
        } else {
            setDefaultSong()
        }
    }

    private fun updateSongInfo(
        title: String?,
        artist: String?,
        thumbnail: Int?,
        subTitle: String?,
        icon: Int?
    ) {
        binding.apply {
            layoutSongName.tvTitle.text = title ?: "Unknown"
            layoutSongName.tvSubTitle.text = artist ?: "Unknown Artist"
            ivSongThumbnail.setImageResource(thumbnail ?: R.drawable.ic_play)

            currentSong.apply {
                this.title = title
                this.artist = artist
                this.songThumbnail = thumbnail
                this.subTitle = subTitle
                this.icon = icon ?: R.drawable.ic_play
            }
        }
    }

    private fun updatePlaybackState(isPlaying: Boolean) {
        this.isPlaying = isPlaying
        binding.ivSongPlay.setImageResource(
            if (isPlaying) R.drawable.ic_pause else R.drawable.ic_play
        )
    }

    private fun updateProgress(currentPosition: Int, duration: Int, callback: (Any) -> Unit) {
        if (duration > 0) {
            binding.layoutSongProgress.seekBar.apply {
                if (!isPressed) {  // Only update if user is not currently seeking
                    valueFrom = 0f
                    valueTo = duration.toFloat()
                    value = currentPosition.toFloat()
                }
            }
            updateCurrentTime(currentPosition)
            updateTotalTime(duration)
        }
    }

    private fun sendServiceCommand(action: String, seekPosition: Int? = null) {
        Intent(this, MusicService::class.java).apply {
            this.action = action
            seekPosition?.let { putExtra("seekPosition", it) }
            startService(this)
        }
    }

    private fun initializeService() {
        Intent(this, MusicService::class.java).apply {
            action = MusicService.ACTION_INIT_SERVICE
            putExtra("songList", songList)
            putExtra("currentIndex", currentSongIndex)
            startService(this)
        }
    }

    private fun updateCurrentTime(position: Int) {
        val minutes = (position / 1000 / 60)
        val seconds = (position / 1000 % 60)
        binding.layoutSongProgress.tvCurrentTime.text = String.format("%02d:%02d", minutes, seconds)
    }

    private fun updateTotalTime(duration: Int) {
        val minutes = (duration / 1000 / 60)
        val seconds = (duration / 1000 % 60)
        binding.layoutSongProgress.tvTotalTime.text = String.format("%02d:%02d", minutes, seconds)
    }

    private fun setDefaultSong() {
        currentSong.apply {
            title = "No Song Available"
            subTitle = ""
            songThumbnail = R.drawable.ic_play
            icon = R.drawable.ic_play
            artist = ""
        }
        binding.apply {
            layoutSongName.tvTitle.text = currentSong.title
            layoutSongName.tvSubTitle.text = currentSong.artist
            ivSongThumbnail.setImageResource(currentSong.songThumbnail ?: R.drawable.ic_play)
            ivSongPlay.setImageResource(R.drawable.ic_play)
        }
    }

    private fun pauseSong() {
        sendServiceCommand(MusicService.ACTION_PLAY_PAUSE)
    }

    private fun updateDeviceVolume() {
        val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        val currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
        val volumePercentage = (currentVolume.toFloat() / maxVolume.toFloat()) * 100.0f
        volumeText.text = String.format("Volume: %.0f%%", volumePercentage)
    }


    private fun setUpStatusBar() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        ViewCompat.getWindowInsetsController(window.decorView)?.isAppearanceLightStatusBars = true
    }

    override fun onSelectSongItem(position: Int) {
        if (position < 0 || position >= songList.size) return
        
        currentSongIndex = position
        currentSong = songList[position]
        initializeService()
    }
}
