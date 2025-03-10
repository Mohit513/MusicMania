package com.example.musicmania.presentation.dashboard

import android.annotation.SuppressLint
import android.content.pm.ActivityInfo
import android.media.MediaPlayer
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.example.musicmania.R
import com.example.musicmania.databinding.ActivitySongsBinding
import com.example.musicmania.presentation.bottom_sheet.SongListBottomSheetFragment
import com.example.musicmania.presentation.bottom_sheet.model.SongListDataModel
import com.example.musicmania.utils.SongUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SongsActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySongsBinding
    val currentSong = SongListDataModel()
    private lateinit var mediaPlayer: MediaPlayer
    private var isPlaying = false
    private var songList: ArrayList<SongListDataModel> = arrayListOf()
    private var currentSongIndex = 0

    private val handler = Handler(Looper.getMainLooper())

    // Runnable to update seekbar every second
    private val updateSeekBarRunnable = object : Runnable {
        override fun run() {
            if (mediaPlayer.isPlaying) {
                val currentPosition = mediaPlayer.currentPosition.toFloat()
                binding.layoutSongProgress.seekBar.value = currentPosition  // Update the seekbar
                handler.postDelayed(this, 1000)  // Update every second
            }
        }
    }

    // Runnable to update current time display every second
    private val updateCurrentTimeRunnable = object : Runnable {
        override fun run() {
            if (mediaPlayer.isPlaying) {
                val currentPosition = mediaPlayer.currentPosition.toFloat()
                updateCurrentTime(currentPosition)  // Update the current time display
                handler.postDelayed(this, 1000)  // Update every second
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySongsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setUpStatusBar()
        setUpListener()
        setUpView()
    }

    private fun setUpListener() {
        binding.tvOpenSongList.setOnClickListener {
            val bottomSheetFragment = SongListBottomSheetFragment()
            bottomSheetFragment.show(supportFragmentManager, "myList")
        }

        binding.apply {
            ivSongPlay.setOnClickListener {
                if (isPlaying) {
                    pauseSong()
                } else {
                    playSong()
                }
            }
            ivPlayForward.setOnClickListener {
                resetAndNewSong()
            }
            ivPlayback.setOnClickListener {
                resetAndLastSong()
            }
        }

        binding.layoutSongProgress.seekBar.addOnChangeListener { _, value, _ ->
            mediaPlayer.seekTo(value.toInt())
            updateCurrentTime(value)
        }
    }

    private fun setUpView() {
        songList = SongUtils.getSongsFromDevice(contentResolver) as ArrayList<SongListDataModel>
        if (songList.isNotEmpty()) {
            currentSong.apply {
                title = songList[currentSongIndex].title
                subTitle = songList[currentSongIndex].subTitle
                songThumbnail = songList[currentSongIndex].songThumbnail
            }
            binding.layoutSongName.tvTitle.text = currentSong.title
            binding.layoutSongName.tvSubTitle.text = currentSong.subTitle

            setUpMediaPlayer()
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
            mediaPlayer.release()  // Release any previously initialized media player
        }

        mediaPlayer = MediaPlayer().apply {
            if (!currentSong.subTitle.isNullOrEmpty()) {
                setDataSource(currentSong.subTitle ?: "")
                prepareAsync()  // Use asynchronous preparation to avoid blocking the UI thread
            }
        }

        mediaPlayer.setOnPreparedListener {
            // Setup seekbar range and initial time when the song is ready to play
            updateTotalTime(mediaPlayer.duration)
            binding.layoutSongProgress.seekBar.valueFrom = 0f
            binding.layoutSongProgress.seekBar.valueTo = mediaPlayer.duration.toFloat()

            // Start playback once ready
            playSong()
        }

        mediaPlayer.setOnCompletionListener {
            handler.removeCallbacks(updateSeekBarRunnable)
            handler.removeCallbacks(updateCurrentTimeRunnable)
        }
    }

    private fun playSong() {
        if (!::mediaPlayer.isInitialized) {
            setUpMediaPlayer()  // Initialize the player only if it's not already initialized
        }

        mediaPlayer.start()
        isPlaying = true
        binding.ivSongPlay.setImageResource(R.drawable.ic_pause)

        // Start both the update tasks
        handler.post(updateSeekBarRunnable)
        handler.post(updateCurrentTimeRunnable)
    }

    private fun pauseSong() {
        mediaPlayer.pause()
        isPlaying = false
        binding.ivSongPlay.setImageResource(R.drawable.ic_play)
        handler.removeCallbacks(updateSeekBarRunnable)
        handler.removeCallbacks(updateCurrentTimeRunnable)
    }

    private fun resetAndNewSong() {
        currentSongIndex = (currentSongIndex + 1) % songList.size
        updateCurrentSongUI()
    }

    private fun resetAndLastSong() {
        currentSongIndex = if (currentSongIndex - 1 < 0) songList.size - 1
        else currentSongIndex - 1
        updateCurrentSongUI()
    }

    private fun updateCurrentSongUI() {
        currentSong.apply {
            title = songList[currentSongIndex].title
            subTitle = songList[currentSongIndex].subTitle
            songThumbnail = songList[currentSongIndex].songThumbnail
        }

        binding.layoutSongName.tvTitle.text = currentSong.title
        binding.layoutSongName.tvSubTitle.text = currentSong.subTitle
        binding.ivSongThumbnail.setImageResource(currentSong.songThumbnail ?: R.drawable.ic_play)
        mediaPlayer.reset()
        setUpMediaPlayer()
        playSong()
    }

    fun setCurrentSongIndex(index: Int) {
        currentSongIndex = index
        updateCurrentSongUI()
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
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT)
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaPlayer.release()
        handler.removeCallbacks(updateSeekBarRunnable)
        handler.removeCallbacks(updateCurrentTimeRunnable)
    }
}
