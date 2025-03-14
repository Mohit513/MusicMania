package com.example.musicmania.presentation.dashboard

import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ActivityInfo
import android.media.AudioManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.PatternMatcher
import android.view.KeyEvent
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import com.example.musicmania.R
import com.example.musicmania.databinding.ActivitySongsBinding
import com.example.musicmania.databinding.ItemSongsListBinding
import com.example.musicmania.presentation.bottom_sheet.SongListBottomSheetFragment
import com.example.musicmania.presentation.bottom_sheet.model.SongListDataModel
import com.example.musicmania.presentation.service.MusicService
import com.example.musicmania.utils.SongUtils
import com.google.android.material.slider.Slider

class SongsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySongsBinding
    private lateinit var itemSongsListBinding: ItemSongsListBinding
    private lateinit var volumeText: TextView
    private lateinit var audioManager: AudioManager

    val currentSong = SongListDataModel()
    private var isPlaying = false
    private var songList: ArrayList<SongListDataModel> = arrayListOf()
    private var currentSongIndex = 0

    private val handler = Handler(Looper.getMainLooper())
    private lateinit var rotationAnimator: ObjectAnimator

    private val playbackReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                MusicService.BROADCAST_PLAYBACK_STATE -> {
                    isPlaying = intent.getBooleanExtra("isPlaying", false)
                    currentSongIndex = intent.getIntExtra("currentIndex", currentSongIndex)
                    updatePlayPauseButton()
                }
                MusicService.BROADCAST_PROGRESS -> {
                    val currentPosition = intent.getIntExtra("currentPosition", 0)
                    val duration = intent.getIntExtra("duration", 0)
                    updateProgress(currentPosition, duration)
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
        itemSongsListBinding = ItemSongsListBinding.inflate(layoutInflater)
        volumeText = binding.tvVolumePercentage
        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager

        setUpStatusBar()
        setUpListeners()
        setUpView()

        binding.layoutSongProgress.seekBar.addOnSliderTouchListener(object : Slider.OnSliderTouchListener {
            override fun onStartTrackingTouch(slider: Slider) {}

            override fun onStopTrackingTouch(slider: Slider) {
                // TODO: Implement seek functionality in MusicService
            }
        })
    }

    override fun onResume() {
        super.onResume()
        updateDeviceVolume()
        registerReceivers()
    }

    override fun onPause() {
        super.onPause()
        try {
            unregisterReceiver(volumeReceiver)
            unregisterReceiver(playbackReceiver)
        } catch (e: IllegalArgumentException) {
            // Receiver not registered
        }
    }

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    private fun registerReceivers() {
        val volumeFilter = IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY)
        val playbackFilter = IntentFilter().apply {
            addAction(MusicService.BROADCAST_PLAYBACK_STATE)
            addAction(MusicService.BROADCAST_PROGRESS)
            addDataScheme("package")
            addDataPath(packageName, PatternMatcher.PATTERN_LITERAL)
        }
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(volumeReceiver, volumeFilter, RECEIVER_NOT_EXPORTED)
            registerReceiver(playbackReceiver, playbackFilter, RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(volumeReceiver, volumeFilter)
            registerReceiver(playbackReceiver, playbackFilter)
        }
    }

    private fun setUpListeners() {
        binding.tvOpenSongList.setOnClickListener {
            val bottomSheetFragment = SongListBottomSheetFragment(songList)
            bottomSheetFragment.show(supportFragmentManager, "songList")
        }

        binding.apply {
            ivSongPlay.setOnClickListener {
                sendServiceCommand(MusicService.ACTION_PLAY_PAUSE)
            }

            ivPlayForward.setOnClickListener {
                sendServiceCommand(MusicService.ACTION_NEXT)
            }

            ivPlayback.setOnClickListener {
                sendServiceCommand(MusicService.ACTION_PREVIOUS)
            }
        }
    }

    private fun setUpView() {
        songList.addAll(SongUtils.getSongsFromDevice(contentResolver))
        if (songList.isNotEmpty()) {
            currentSong.apply {
                title = songList[currentSongIndex].title
                subTitle = songList[currentSongIndex].subTitle
                songThumbnail = songList[currentSongIndex].songThumbnail
                icon = songList[currentSongIndex].icon
                artist = songList[currentSongIndex].artist
            }
            binding.layoutSongName.tvTitle.text = currentSong.title
            binding.layoutSongName.tvSubTitle.text = currentSong.artist
            itemSongsListBinding.ivPlayAndPause.setImageResource(currentSong.icon)
            initializeService()
        } else {
            setDefaultSong()
        }
    }

    private fun setDefaultSong() {
        currentSong.apply {
            title = "No Song Available"
            subTitle = ""
            songThumbnail = R.drawable.ic_play
        }
        binding.layoutSongName.tvTitle.text = currentSong.title
    }

    private fun initializeService() {
        Intent(this, MusicService::class.java).apply {
            action = MusicService.ACTION_INIT_SERVICE
            putExtra("songList", songList)
            putExtra("currentIndex", currentSongIndex)
            startService(this)
        }
    }

    private fun sendServiceCommand(action: String) {
        Intent(this, MusicService::class.java).apply {
            this.action = action
            startService(this)
        }
    }

    private fun updatePlayPauseButton() {
        binding.ivSongPlay.setImageResource(
            if (isPlaying) R.drawable.ic_pause else R.drawable.ic_play
        )
    }

    private fun updateProgress(currentPosition: Int, duration: Int) {
        binding.layoutSongProgress.seekBar.apply {
            valueFrom = 0f
            valueTo = duration.toFloat()
            value = currentPosition.toFloat()
        }
        updateCurrentTime(currentPosition.toFloat())
        updateTotalTime(duration)
    }

    private fun pauseSong() {
        sendServiceCommand(MusicService.ACTION_PLAY_PAUSE)
    }

    private fun updateCurrentTime(position: Float) {
        val minutes = (position / 1000 / 60).toInt()
        val seconds = (position / 1000 % 60).toInt()
        binding.layoutSongProgress.tvCurrentTime.text = String.format("%02d:%02d", minutes, seconds)
    }

    private fun updateTotalTime(duration: Int) {
        val minutes = (duration / 1000 / 60)
        val seconds = (duration / 1000 % 60)
        binding.layoutSongProgress.tvTotalTime.text = String.format("%02d:%02d", minutes, seconds)
    }

    private fun updateDeviceVolume() {
        val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        val currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
        val volumePercent = (currentVolume.toFloat() / maxVolume.toFloat() * 100).toInt()
        volumeText.text = "$volumePercent%"
    }

    private fun setUpStatusBar() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { view, windowInsets ->
            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(insets.left, insets.top, insets.right, insets.bottom)
            WindowInsetsCompat.CONSUMED
        }
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        return when (keyCode) {
            KeyEvent.KEYCODE_VOLUME_UP -> {
                audioManager.adjustStreamVolume(
                    AudioManager.STREAM_MUSIC,
                    AudioManager.ADJUST_RAISE,
                    AudioManager.FLAG_SHOW_UI
                )
                updateDeviceVolume()
                true
            }
            KeyEvent.KEYCODE_VOLUME_DOWN -> {
                audioManager.adjustStreamVolume(
                    AudioManager.STREAM_MUSIC,
                    AudioManager.ADJUST_LOWER,
                    AudioManager.FLAG_SHOW_UI
                )
                updateDeviceVolume()
                true
            }
            else -> super.onKeyDown(keyCode, event)
        }
    }

    fun currentSongIndex(index: Int) {
        if (index < 0 || index >= songList.size) return
        
        currentSongIndex = index
        // Update current song UI immediately
        currentSong.apply {
            title = songList[currentSongIndex].title
            subTitle = songList[currentSongIndex].subTitle
            songThumbnail = songList[currentSongIndex].songThumbnail
            icon = songList[currentSongIndex].icon
            artist = songList[currentSongIndex].artist
        }
        binding.layoutSongName.tvTitle.text = currentSong.title
        binding.layoutSongName.tvSubTitle.text = currentSong.artist
        binding.ivSongThumbnail.setImageResource(currentSong.songThumbnail ?: R.drawable.ic_play)
        
        // Reset playback state UI
        isPlaying = true
        updatePlayPauseButton()

        // Initialize service with new song using INIT_SERVICE action
        Intent(this, MusicService::class.java).apply {
            action = MusicService.ACTION_INIT_SERVICE  // Using constant from memory
            putExtra("songList", songList)
            putExtra("currentIndex", currentSongIndex)
            startService(this)
        }

        // Reset seekbar and time displays until service sends update
        binding.layoutSongProgress.seekBar.apply {
            value = 0f
            valueFrom = 0f
            valueTo = 100f  // Temporary until actual duration is received
        }
        updateCurrentTime(0f)
        updateTotalTime(0)
    }
}
