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
import android.database.ContentObserver
import android.media.AudioManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.provider.Settings
import android.view.KeyEvent
import android.view.View
import android.view.WindowInsetsController
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
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

open class SongsActivity : BaseActivity(), SongListBottomSheetFragment.SongListListener {

    private lateinit var binding: ActivitySongsBinding
    private lateinit var audioManager: AudioManager
    private lateinit var handler: Handler
    private var currentSong = SongListDataModel()
    private var songList: ArrayList<SongListDataModel> = arrayListOf()
    private var currentSongIndex = 0
    private var isPlaying = false

    private var rotationAnimation: ObjectAnimator? = null

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
                Constant.BROADCAST_PLAYBACK_STATE -> {
                    val isPlaying = intent.getBooleanExtra("isPlaying", false)
                    val currentIndex = intent.getIntExtra("currentIndex", -1)
                    val duration = intent.getIntExtra("duration", 0)
                    val currentPosition = intent.getIntExtra("currentPosition", 0)
                    val rotation = intent.getBooleanExtra("rotation", false)

                    if (currentIndex != -1 && currentIndex < songList.size) {
                        songList.forEachIndexed { index, song ->
                            song.isPlaying = index == currentIndex && isPlaying
                        }
                        currentSongIndex = currentIndex
                        currentSong = songList[currentIndex]
                        updateSongInfo(currentSong)
                    }

                    updatePlaybackState(isPlaying)

                    if (rotation) {
                        startThumbnailRotation()
                    } else {
                        stopThumbnailRotation()
                    }

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

    private val audioVolumeObserver = object : ContentObserver(Handler(Looper.getMainLooper())) {
        override fun onChange(selfChange: Boolean, uri: Uri?) {
            super.onChange(selfChange, uri)
            if (uri == Settings.System.CONTENT_URI) {
                updateDeviceVolume()
            }
        }
    }
    private val volumeHandler = Handler(Looper.getMainLooper())
    private val volumeRunnable = object : Runnable {
        override fun run() {
            updateDeviceVolume()
            volumeHandler.postDelayed(this, 200)
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

        setContentView(binding.root)
        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager

        if (intent.flags and Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT != 0) {
            finish()
            return
        }

        setUpStatusBar()
        setUpListeners()
        checkAndRequestPermissions()

        intent?.let { handleNotificationIntent(it) }
    }

    private fun handleNotificationIntent(intent: Intent) {
        val index = intent.getIntExtra("currentSongIndex", -1)
        val isPlaying = intent.getBooleanExtra("isPlaying", false)
        val fromNotification = intent.getBooleanExtra("fromNotification", false)

        if (index != -1) {
            currentSongIndex = index
            if (fromNotification) {
                Intent(this, MusicService::class.java).apply {
                    action = Constant.BROADCAST_PLAYBACK_STATE
                    startService(this)
                }
            } else {
                updatePlaybackState(isPlaying)
            }
        }
    }

    override fun onBackPressed() {
        musicService?.apply {
            createNotificationChannel()
            startForeground(Constant.NOTIFICATION_ID, createCustomNotification().build())
        }
        moveTaskToBack(true)
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
        intent.let { handleNotificationIntent(it) }
    }

    private fun updatePlaybackState(isPlaying: Boolean) {
        this.isPlaying = isPlaying
        binding.apply {

            ivSongPlay.setImageResource(
                if (isPlaying) R.drawable.ic_pause else R.drawable.ic_play
            )
            ivSongPlay.tag = if (isPlaying) "playing" else "paused"
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

    private fun setUpListeners() {
        binding.apply {
            ivMenu.setOnClickListener {
                val intent = Intent(applicationContext, SongListActivity::class.java)
                intent.putParcelableArrayListExtra("songList", songList)
                intent.putExtra("currentSongIndex", currentSongIndex)
                intent.putExtra("isPlaying", isPlaying)
                startActivity(intent)
            }
            layoutOpenSongList.setOnClickListener {
                showSongListBottomSheet()
            }

            ivSongPlay.setOnClickListener {
                if (songList.isNotEmpty()) {
                    sendServiceCommand(Constant.ACTION_PLAY_PAUSE)
                } else {
                    Toast.makeText(this@SongsActivity, "No songs available.", Toast.LENGTH_SHORT)
                        .show()
                }
            }

            ivPlayForward.setOnClickListener {
                if (songList.isNotEmpty()) {
                    sendServiceCommand(Constant.ACTION_PREVIOUS)
                } else {
                    Toast.makeText(this@SongsActivity, "No songs available.", Toast.LENGTH_SHORT)
                        .show()
                }
            }

            ivPlayback.setOnClickListener {
                if (songList.isNotEmpty()) {
                    sendServiceCommand(Constant.ACTION_NEXT)
                } else {
                    Toast.makeText(this@SongsActivity, "No songs available.", Toast.LENGTH_SHORT)
                        .show()
                }
            }

            layoutSongProgress.slider.addOnSliderTouchListener(object :
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

            layoutSongProgress.slider.addOnChangeListener { _, value, fromUser ->
                if (fromUser) {
                    sendServiceCommand(Constant.ACTION_SEEK, value.toInt())
                }
            }
        }
    }

    private fun showSongListBottomSheet() {
        SongListBottomSheetFragment(
            songList = songList,
            currentSongIndex = currentSongIndex,
            isPlaying = isPlaying
        ).show(supportFragmentManager, Constant.TAG)
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

    private fun setUpView() {
        songList.clear()
        songList.addAll(SongUtils.getSongsFromDevice(contentResolver))
        if (songList.isNotEmpty()) {
            currentSong = songList[currentSongIndex]
            updateSongInfo(currentSong)
            initializeService(false)
            binding.ivSongPlay.setImageResource(R.drawable.ic_play)
            binding.ivSongPlay.tag = "paused"
        } else {
            setDefaultSong()
        }
    }

    private fun updateSongInfo(song: SongListDataModel?) {
        song?.let {
            binding.apply {
                layoutSongName.apply {
                    tvTitle.text = it.title
                    tvSubTitle.text = it.artist
                    tvSubTitle.textAlignment = View.TEXT_ALIGNMENT_CENTER
                    tvTitle.textAlignment = View.TEXT_ALIGNMENT_CENTER
                }
                ivSongThumbnail.setImageResource(it.songThumbnail ?: R.drawable.app_logo)
                ivSongPlay.setImageResource(if (isPlaying) R.drawable.ic_pause else R.drawable.ic_play)
            }
        }
    }

    private fun updateProgress(currentPosition: Int, duration: Int, callback: (Any) -> Unit) {
        if (duration > 0) {
            binding.layoutSongProgress.slider.apply {
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

    private fun initializeService(autoPlay: Boolean = false) {
        Intent(this, MusicService::class.java).apply {
            action = Constant.ACTION_INIT_SERVICE
            putExtra("songList", songList)
            putExtra("currentIndex", currentSongIndex)
            putExtra("autoPlay", autoPlay)
            startService(this)
        }
    }

    @SuppressLint("DefaultLocale")
    private fun updateCurrentTime(position: Int) {
        val minutes = (position / 1000 / 60)
        val seconds = (position / 1000 % 60)
        binding.layoutSongProgress.tvCurrentTime.text = String.format("%02d:%02d", minutes, seconds)
    }

    @SuppressLint("DefaultLocale")
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

    @SuppressLint("SetTextI18n")
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

        // Reset all songs' playing state
        songList.forEach { it.isPlaying = false }

        currentSongIndex = position
        currentSong = songList[position]
        currentSong.isPlaying = true

        Intent(this, MusicService::class.java).apply {
            action = Constant.ACTION_INIT_SERVICE
            putExtra("songList", songList)
            putExtra("currentIndex", position)
            putExtra("autoPlay", true)
            startService(this)
        }

        if (!isBound) {
            Intent(this, MusicService::class.java).also { intent ->
                bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
            }
        }
    }

    private fun registerVolumeObserver() {
        contentResolver.registerContentObserver(
            Settings.System.CONTENT_URI,
            true,
            audioVolumeObserver
        )
    }

    private fun unregisterVolumeObserver() {
        contentResolver.unregisterContentObserver(audioVolumeObserver)
    }

    inner class VolumeUpdateReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == "android.media.VOLUME_CHANGED_ACTION") {
                updateDeviceVolume()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        updateDeviceVolume()
        registerReceivers()
        registerVolumeObserver()
        startVolumeUpdates()

        Intent(this, MusicService::class.java).also { intent ->
            bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
        }

        handler = Handler(Looper.getMainLooper())
        startProgressUpdates()

        Intent(this, MusicService::class.java).apply {
            action = Constant.BROADCAST_PLAYBACK_STATE
            startService(this)
        }
    }

    override fun onPause() {
        super.onPause()
        try {
            unregisterReceiver(volumeReceiver)
            unregisterReceiver(playbackReceiver)
            unregisterVolumeObserver()
            stopVolumeUpdates()
        } catch (e: IllegalArgumentException) {
            e.printStackTrace()
        }
        stopThumbnailRotation()
        handler.removeCallbacksAndMessages(null)

        if (isPlaying || musicService?.mediaPlayer?.isPlaying == true) {
            musicService?.apply {
                createNotificationChannel()
                startForeground(Constant.NOTIFICATION_ID, createCustomNotification().build())
            }
        }

        if (isBound) {
            unbindService(serviceConnection)
            isBound = false
        }
    }

    private fun startVolumeUpdates() {
        volumeHandler.post(volumeRunnable)
    }

    private fun stopVolumeUpdates() {
        volumeHandler.removeCallbacks(volumeRunnable)
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

    override fun onDestroy() {
        try {
            if (isFinishing) {
                if (!isPlaying && musicService?.mediaPlayer?.isPlaying != true) {
                    stopService(Intent(this, MusicService::class.java))
                } else {
                    musicService?.apply {
                        createNotificationChannel()
                        startForeground(
                            Constant.NOTIFICATION_ID,
                            createCustomNotification().build()
                        )
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        musicService?.mediaPlayer?.release()
        stopService(intent)
        super.onDestroy()
    }
}