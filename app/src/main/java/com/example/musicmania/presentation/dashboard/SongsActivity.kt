package com.example.musicmania.presentation.dashboard

import android.animation.ObjectAnimator
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
import android.view.KeyEvent
import android.view.View
import android.view.WindowInsetsController
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.core.app.ServiceCompat.StopForegroundFlags
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import com.example.musicmania.Constant
import com.example.musicmania.R
import com.example.musicmania.base.BaseActivity
import com.example.musicmania.databinding.ActivitySongsBinding
import com.example.musicmania.presentation.bottom_sheet.SongListBottomSheetFragment
import com.example.musicmania.presentation.bottom_sheet.model.SongListDataModel
import com.example.musicmania.presentation.service.MusicService
import com.example.musicmania.utils.PermissionUtils
import com.example.musicmania.utils.SongUtils
import com.google.android.material.slider.Slider

class SongsActivity : BaseActivity(), SongListBottomSheetFragment.SongListListener {
    private lateinit var binding: ActivitySongsBinding
    private lateinit var volumeText: TextView
    private lateinit var audioManager: AudioManager
    private lateinit var handler: Handler
    private var currentSong = SongListDataModel()
    private var songList: ArrayList<SongListDataModel> = arrayListOf()
    private var currentSongIndex = 0
    private var isPlaying = false

    private var rotationAnimation: ObjectAnimator? = null

    private var musicService: MusicService? = null
    private var isBound = false

    var isLockScreenActive = false

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
                Constant.BROADCAST_PLAYBACK_STATE -> {
                    val isPlaying = intent.getBooleanExtra("isPlaying", false)
                    val currentIndex = intent.getIntExtra("currentIndex", -1)
                    val duration = intent.getIntExtra("duration", 0)
                    val currentPosition = intent.getIntExtra("currentPosition", 0)

                    if (currentIndex != -1 && currentIndex < songList.size) {
                        // Reset isPlaying for all songs
                        songList.forEach { it.isPlaying = false }

                        currentSongIndex = currentIndex
                        currentSong = songList[currentIndex]
                        // Set isPlaying only for current song
                        currentSong.isPlaying = isPlaying
                        updateSongInfo(currentSong)
                    }

                    updatePlaybackState(isPlaying)
                    updateProgress(currentPosition, duration) {}
                }

                Constant.BROADCAST_PROGRESS -> {
                    val currentPosition = intent.getIntExtra("currentPosition", 0)
                    val duration = intent.getIntExtra("duration", 0)
                    updateProgress(currentPosition, duration) {}
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
        enableEdgeToEdge()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.insetsController?.apply {
                hide(android.view.WindowInsets.Type.statusBars())
                systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        } else {
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_FULLSCREEN
                    or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN)
        }

        checkAndRequestPermissions()

        setContentView(binding.root)
        volumeText = binding.tvVolumePercentage
        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager

        setUpStatusBar()
        setUpListeners()
        setUpView()
        // showLockScreenIfNeeded() //todo

        intent?.let { handleNotificationIntent(it) }

    }

    private fun refreshUI() {
        // After updating the song list, you can ensure UI elements are updated
        if (songList.isNotEmpty()) {
            // Reset isPlaying for all songs
            songList.forEach { it.isPlaying = false }

            currentSong = songList[currentSongIndex]
            // Set isPlaying for current song
            currentSong.isPlaying = isPlaying
            updateSongInfo(currentSong)
        }

        binding.apply {
            ivSongThumbnail.setImageResource(currentSong.songThumbnail ?: R.drawable.app_logo)
            layoutSongName.tvTitle.text = currentSong.title
            layoutSongName.tvSubTitle.text = currentSong.artist
        }
    }

    private fun checkAndRequestPermissions() {
        if (!PermissionUtils.checkAllPermissions(this)) {
            PermissionUtils.requestAllPermissions(this)
        } else {
            setUpView()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        when (requestCode) {
            PermissionUtils.ALL_PERMISSIONS_REQUEST_CODE -> {
                if (grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                    initializeService()
                    setUpView()
//                    refreshUI()
                } else {
                    Toast.makeText(this, "App requires all permission", Toast.LENGTH_SHORT).show()
                    setDefaultSong()
                    setUpView()
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        intent?.let { handleNotificationIntent(it) }
    }

    private fun updatePlaybackState(isPlaying: Boolean) {
        this.isPlaying = isPlaying
        binding.apply {

            ivSongPlay.setImageResource(
                if (isPlaying) R.drawable.ic_pause else R.drawable.ic_play
            )
            if (isPlaying) {
                startThumbnailRotation()
            } else {
                stopThumbnailRotation()
            }
        }
    }

    private fun startThumbnailRotation() {
        rotationAnimation?.cancel()
        rotationAnimation = ObjectAnimator.ofFloat(
            binding.ivSongThumbnail,
            View.ROTATION,
            binding.ivSongThumbnail.rotation,
            binding.ivSongThumbnail.rotation + 360f
        ).apply {
            duration = 8000
            interpolator = android.view.animation.LinearInterpolator()
            repeatCount = ObjectAnimator.INFINITE
            start()
        }
    }

    private fun stopThumbnailRotation() {
        rotationAnimation?.cancel()
        rotationAnimation = null
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

    override fun onKeyUp(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_VOLUME_UP || keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
            updateDeviceVolume()
            return true
        }
        return super.onKeyUp(keyCode, event)
    }

    private fun handleNotificationIntent(intent: Intent) {
        val index = intent.getIntExtra("currentSongIndex", -1)
        val isPlaying = intent.getBooleanExtra("isPlaying", false)
        if (index != -1) {
            currentSongIndex = index
            updatePlaybackState(isPlaying)
            musicService?.let {
                if (it.mediaPlayer?.isPlaying != isPlaying) {
                    sendServiceCommand(Constant.ACTION_PLAY_PAUSE)
                }
            }
        }
    }


    override fun onResume() {
        super.onResume()
        updateDeviceVolume()
        registerReceivers()
        // showLockScreenIfNeeded() //todo

        Intent(this, Constant::class.java).also { intent ->
            bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
        }

        handler = Handler(Looper.getMainLooper())
        startProgressUpdates()

        if (isPlaying) {
            startThumbnailRotation()
        }
    }

    override fun onPause() {
        super.onPause()
        try {
            unregisterReceiver(volumeReceiver)
            unregisterReceiver(playbackReceiver)
        } catch (e: IllegalArgumentException) {
            e.printStackTrace()
        }
        stopThumbnailRotation()
        // hideLockScreenIfNeeded() //todo
        handler.removeCallbacksAndMessages(null)
        if (isBound) {
            unbindService(serviceConnection)
            isBound = false
        }
    }

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    private fun registerReceivers() {
        try {
            val volumeFilter = IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY)

            val playbackFilter = IntentFilter().apply {
                addAction(Constant.BROADCAST_PLAYBACK_STATE)
                addAction(Constant.BROADCAST_PROGRESS)
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                registerReceiver(
                    volumeReceiver,
                    volumeFilter,
                    Context.RECEIVER_EXPORTED
                )

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
            ivMenu.setOnClickListener {
                val intent = Intent(applicationContext, SongListActivity::class.java)
                intent.putParcelableArrayListExtra("songList", songList)
                intent.putExtra("currentSongIndex", currentSongIndex)
                intent.putExtra("isPlaying", isPlaying)
                startActivity(intent)
            }
            tvOpenSongList.setOnClickListener {
                showSongListBottomSheet()
            }


            ivSongPlay.setOnClickListener {
                if (songList.isNotEmpty()) {
                    sendServiceCommand(Constant.ACTION_PLAY_PAUSE)
                } else {
                    Toast.makeText(this@SongsActivity, "No songs available.", Toast.LENGTH_SHORT).show()
                }
            }

            ivPlayForward.setOnClickListener {
                if (songList.isNotEmpty()) {
                    sendServiceCommand(Constant.ACTION_PREVIOUS)
                } else {
                    Toast.makeText(this@SongsActivity, "No songs available.", Toast.LENGTH_SHORT).show()
                }
            }

            ivPlayback.setOnClickListener {
                if (songList.isNotEmpty()) {
                    sendServiceCommand(Constant.ACTION_NEXT)
                } else {
                    Toast.makeText(this@SongsActivity, "No songs available.", Toast.LENGTH_SHORT).show()
                }
            }

            layoutSongProgress.seekBar.addOnSliderTouchListener(object :
                Slider.OnSliderTouchListener {
                override fun onStartTrackingTouch(slider: Slider) {
                    handler.removeCallbacksAndMessages(null)
                }

                override fun onStopTrackingTouch(slider: Slider) {
                    val position = slider.value.toInt()
                    sendServiceCommand(Constant.ACTION_SEEK, position)

                    startProgressUpdates()
                }
            })

            layoutSongProgress.seekBar.addOnChangeListener { _, value, fromUser ->
                if (fromUser) {
                    sendServiceCommand(Constant.ACTION_SEEK, value.toInt())
                }
            }
        }
    }

    private fun showSongListBottomSheet() {
        SongListBottomSheetFragment(
            songList,
            currentSongIndex,
            isPlaying
        ).show(supportFragmentManager, SongListBottomSheetFragment.TAG)
    }

    private fun startProgressUpdates() {
        handler.postDelayed(object : Runnable {
            override fun run() {
                musicService?.mediaPlayer?.let { player ->
                    if (player.isPlaying) {
                        updateProgress(player.currentPosition, player.duration) { _ ->

                        }
                    }
                }
                handler.postDelayed(this, 1000)
            }
        }, 1000)
    }

    private fun broadcastProgress(currentPosition: Int, duration: Int) {
        Intent(Constant.BROADCAST_PROGRESS).apply {
            setPackage(packageName)
            putExtra("currentPosition", currentPosition)
            putExtra("duration", duration)
            sendBroadcast(this)
        }
    }

    private fun setUpView() {
        songList.clear()
        songList.addAll(SongUtils.getSongsFromDevice(contentResolver))
        if (songList.isNotEmpty()) {
            currentSong = songList[currentSongIndex]
            updateSongInfo(currentSong)
            initializeService()
        } else {
            setDefaultSong()
        }
    }

    private fun updateSongInfo(song: SongListDataModel?) {
        song?.let {
            binding.apply {
                layoutSongName.tvTitle.text = it.title
                layoutSongName.tvSubTitle.text = it.artist
                layoutSongName.tvSubTitle.textAlignment = View.TEXT_ALIGNMENT_CENTER
                layoutSongName.tvTitle.textAlignment = View.TEXT_ALIGNMENT_CENTER
                ivSongThumbnail.setImageResource(it.songThumbnail ?: R.drawable.app_logo)
                ivSongPlay.setImageResource(if (isPlaying) R.drawable.ic_pause else R.drawable.ic_play)
            }
        }
    }

    private fun updateProgress(currentPosition: Int, duration: Int, callback: (Any) -> Unit) {
        if (duration > 0) {
            binding.layoutSongProgress.seekBar.apply {
                if (!isPressed) {
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
            action = Constant.ACTION_INIT_SERVICE
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
            subTitle = "No song here"
            songThumbnail = R.drawable.app_logo
            icon = R.drawable.app_logo
            artist = ""
        }
        binding.apply {
            layoutSongName.tvTitle.text = currentSong.title
            layoutSongName.tvSubTitle.text = currentSong.artist
            layoutSongName.tvTitle.textAlignment
            layoutSongName.tvTitle.textAlignment = View.TEXT_ALIGNMENT_CENTER
            ivSongThumbnail.setImageResource(currentSong.songThumbnail ?: R.drawable.ic_play)
            ivSongPlay.setImageResource(R.drawable.ic_play)
        }
    }

    private fun pauseSong() {
        sendServiceCommand(Constant.ACTION_PLAY_PAUSE)
    }

    private fun updateDeviceVolume() {
        val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        val currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
        val volume = (currentVolume.toFloat() / maxVolume.toFloat()) * 100
        binding.tvVolumePercentage.text = "${volume.toInt()}%"
    }

    private fun setUpStatusBar() {

        WindowCompat.setDecorFitsSystemWindows(window, false)
        ViewCompat.getWindowInsetsController(window.decorView)?.isAppearanceLightStatusBars = true
        window.decorView.top = getColor(R.color.pomegranate)
    }

    override fun onSelectSongItem(position: Int) {
        if (position < 0 || position >= songList.size) return

        currentSongIndex = position
        currentSong = songList[position]
        updatePlaybackState(true)
        initializeService()
    }

    inner class VolumeUpdateReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == "android.media.VOLUME_CHANGED_ACTION") {
                updateDeviceVolume()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        stopService(Intent(this, MusicService::class.java))
        musicService?.onDestroy()
        StopForegroundFlags()
    }
}