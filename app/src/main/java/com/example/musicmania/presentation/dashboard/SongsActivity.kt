package com.example.musicmania.presentation.dashboard

import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ActivityInfo
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Bundle
import android.os.Handler
import android.os.Looper
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
    private lateinit var itemSongsListBinding : ItemSongsListBinding
    private lateinit var mediaPlayer: MediaPlayer
    private lateinit var volumeText: TextView
    private lateinit var audioManager: AudioManager

    val currentSong = SongListDataModel()
    private var isPlaying = false
    private var songList: ArrayList<SongListDataModel> = arrayListOf()
    private var currentSongIndex = 0

    private val handler = Handler(Looper.getMainLooper())

    private val updateSeekBarRunnable = object : Runnable {
        override fun run() {
            if (mediaPlayer.isPlaying) {
                val currentPosition = mediaPlayer.currentPosition.toFloat()
                binding.layoutSongProgress.seekBar.value = currentPosition
                handler.postDelayed(this, 1000)

                if (currentPosition > mediaPlayer.duration.toFloat() - 1000) {
                    resetAndNewSong()
                }
            }
        }
    }

    private val updateCurrentTimeRunnable = object : Runnable {
        override fun run() {
            if (mediaPlayer.isPlaying) {
                val currentPosition = mediaPlayer.currentPosition.toFloat()
                updateCurrentTime(currentPosition)
                handler.postDelayed(this, 100)
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

        val filter = IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY)
        registerReceiver(volumeReceiver, filter)

        binding.layoutSongProgress.seekBar.addOnSliderTouchListener(object : Slider.OnSliderTouchListener {
            override fun onStartTrackingTouch(slider: Slider) {}

            override fun onStopTrackingTouch(slider: Slider) {
                mediaPlayer.seekTo(slider.value.toInt())
            }
        })

        binding.layoutSongProgress.seekBar.addOnChangeListener { _, value, _ ->
            updateCurrentTime(value)
            if (value == mediaPlayer.duration.toFloat()) {
                resetAndNewSong()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        updateDeviceVolume()
    }

    override fun onPause() {
        super.onPause()
        unregisterReceiver(volumeReceiver)
    }

    private fun setUpListeners() {
        binding.tvOpenSongList.setOnClickListener {
            val bottomSheetFragment = SongListBottomSheetFragment()
            bottomSheetFragment.show(supportFragmentManager, "myList")
        }

        binding.apply {
            ivSongPlay.setOnClickListener {
                if (isPlaying) pauseSong() else playSong()
            }

            ivPlayForward.setOnClickListener {
                resetAndNewSong()
            }

            ivPlayback.setOnClickListener {
                resetAndLastSong()
            }
        }
    }

    private fun setUpView() {
        songList = SongUtils.getSongsFromDevice(contentResolver) as ArrayList<SongListDataModel>
        if (songList.isNotEmpty()) {
            currentSong.apply {
                title = songList[currentSongIndex].title
                subTitle = songList[currentSongIndex].subTitle
                songThumbnail = songList[currentSongIndex].songThumbnail
                icon = songList[currentSongIndex].icon
            }
            binding.layoutSongName.tvTitle.text = currentSong.title
            binding.layoutSongName.tvSubTitle.text = currentSong.subTitle
            itemSongsListBinding.ivPlayForward.setImageResource(currentSong.icon)
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

    private fun setUpMediaPlayer() {
        if (::mediaPlayer.isInitialized) {
            mediaPlayer.release()
        }

        mediaPlayer = MediaPlayer().apply {
            setDataSource(currentSong.subTitle ?: "")
            prepareAsync()
        }

        mediaPlayer.setOnPreparedListener {
            updateTotalTime(mediaPlayer.duration)
            binding.layoutSongProgress.seekBar.valueFrom = 0f
            binding.layoutSongProgress.seekBar.valueTo = mediaPlayer.duration.toFloat()
            playSong()
        }

        mediaPlayer.setOnCompletionListener {
            handler.removeCallbacks(updateSeekBarRunnable)
            handler.removeCallbacks(updateCurrentTimeRunnable)
        }
    }

    private fun playSong() {
        if (!::mediaPlayer.isInitialized) {
            setUpMediaPlayer()
        }

        val intent = Intent(this, MusicService::class.java)
        intent.putExtra("song",currentSong)
        startService(intent)

        mediaPlayer.start()
        isPlaying = true
        binding.ivSongPlay.setImageResource(R.drawable.ic_pause)

        handler.post(updateSeekBarRunnable)
        handler.post(updateCurrentTimeRunnable)
        startContinuousThumbnailRotation()
    }


    private fun pauseSong() {
        mediaPlayer.pause()
        isPlaying = false
        binding.ivSongPlay.setImageResource(R.drawable.ic_play)

        handler.removeCallbacks(updateSeekBarRunnable)
        handler.removeCallbacks(updateCurrentTimeRunnable)
        stopContinuousThumbnailRotation()
    }

    private fun resetAndNewSong() {
        currentSongIndex = (currentSongIndex + 1) % songList.size
        updateCurrentSongUI()
    }

    private fun resetAndLastSong() {
        currentSongIndex = if (currentSongIndex - 1 < 0) songList.size - 1 else currentSongIndex - 1
        updateCurrentSongUI()
    }

    private fun updateCurrentSongUI() {
        currentSong.apply {
            title = songList[currentSongIndex].title
            subTitle = songList[currentSongIndex].subTitle
            songThumbnail = songList[currentSongIndex].songThumbnail
            icon = songList[currentSongIndex].icon
        }

        binding.layoutSongName.tvTitle.text = currentSong.title
        binding.layoutSongName.tvSubTitle.text = currentSong.subTitle
        binding.ivSongThumbnail.setImageResource(currentSong.songThumbnail ?: R.drawable.ic_play)
        updatePlayForwardIcon(currentSong.icon)

        mediaPlayer.reset()
        setUpMediaPlayer()
        playSong()

    }

    private fun updatePlayForwardIcon(iconResourceId: Int? = R.drawable.ic_pause) {
        if (iconResourceId != null) {
            binding.ivPlayForward.setImageResource(iconResourceId)
        } else {
            binding.ivPlayForward.setImageResource(R.drawable.ic_play)
        }
    }

    @SuppressLint("DefaultLocale")
    private fun updateCurrentTime(currentPosition: Float) {
        val seconds = (currentPosition / 1000).toInt()
        val minutes = seconds / 60
        val displaySeconds = seconds % 60
        binding.layoutSongProgress.tvCurrentTime.text =
            String.format("%02d:%02d", minutes, displaySeconds)
    }

    @SuppressLint("DefaultLocale")
    private fun updateTotalTime(duration: Int) {
        val totalSeconds = duration / 1000
        val totalMinutes = totalSeconds / 60
        val totalDisplaySeconds = totalSeconds % 60
        binding.layoutSongProgress.tvTotalTime.text =
            String.format("%02d:%02d", totalMinutes, totalDisplaySeconds)
    }

    fun currentSongIndex(index: Int) {
        currentSongIndex = index
        updateCurrentSongUI()
    }

    private fun startContinuousThumbnailRotation() {
        if (isPlaying) {
            val rotationAnimator = ObjectAnimator.ofFloat(binding.ivSongThumbnail, "rotation", 0f, 360f)
            rotationAnimator.duration = 30000
            rotationAnimator.repeatCount = ObjectAnimator.INFINITE
            rotationAnimator.repeatMode = ObjectAnimator.RESTART
            rotationAnimator.start()
        }
    }

    private fun stopContinuousThumbnailRotation() {
        if (isPlaying) {
            binding.ivSongThumbnail.animate().cancel()
        }
    }

    @SuppressLint("DefaultLocale")
    private fun updateDeviceVolume() {
        val deviceVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
        val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        val volumePercentage = (deviceVolume / maxVolume.toFloat()) * 100
        volumeText.text = String.format("Volume: %.0f%%", volumePercentage)
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_VOLUME_UP || keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
            updateDeviceVolume()
        }
        return super.onKeyDown(keyCode, event)
    }

    private val volumeReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            updateDeviceVolume()
        }
    }

    private fun setUpStatusBar() {
        window.apply {
            WindowCompat.setDecorFitsSystemWindows(this, false)
            ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content)) { view, insets ->
                val systemBarsInsets = insets.getInsets(WindowInsetsCompat.Type.systemBars())
                view.setPadding(
                    systemBarsInsets.left,
                    systemBarsInsets.top,
                    systemBarsInsets.right,
                    systemBarsInsets.bottom
                )
                insets
            }
        }
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
    }

    override fun onKeyShortcut(keyCode: Int, event: KeyEvent?): Boolean {
        return super.onKeyShortcut(keyCode, event)
    }
}
