package com.example.musicmania.presentation.dashboard

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.ServiceConnection
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.musicmania.Constant
import com.example.musicmania.base.BaseActivity
import com.example.musicmania.databinding.ActivitySongListBinding
import com.example.musicmania.presentation.bottom_sheet.model.SongListDataModel
import com.example.musicmania.presentation.dashboard.adapter.SearchSongListAdapter
import com.example.musicmania.presentation.interfaces.SongSelectionListener
import com.example.musicmania.presentation.service.MusicService

class SongListActivity : BaseActivity(), SongSelectionListener {

    private var songList: ArrayList<SongListDataModel> = arrayListOf()
    private var commonSearchList: ArrayList<SongListDataModel> = arrayListOf()
    private val originalIndexMap = mutableMapOf<SongListDataModel, Int>()
    private var currentSongIndex: Int = 0
    private var isPlaying: Boolean = true
    private lateinit var binding: ActivitySongListBinding
    private lateinit var songListAdapter: SearchSongListAdapter
    private var musicService: MusicService? = null
    private var isBound = false

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as MusicService.MusicBinder
            musicService = binder.getService()
            isBound = true
            updateAdapterPlayingState()
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            musicService = null
            isBound = false
        }
    }

    private val playbackStateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == Constant.BROADCAST_PLAYBACK_STATE) {
                val isPlaying = intent.getBooleanExtra(Constant.IS_PLAYING, false)
                val currentIndex = intent.getIntExtra(Constant.CURRENT_INDEX, 0)

                this@SongListActivity.currentSongIndex = currentIndex
                this@SongListActivity.isPlaying = isPlaying
                updateAdapterPlayingState()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySongListBinding.inflate(layoutInflater)
        enableEdgeToEdge()
        setContentView(binding.root)

        songList = intent.getParcelableArrayListExtra(Constant.SONG_LIST) ?: arrayListOf()
        commonSearchList.addAll(songList)
        currentSongIndex = intent.getIntExtra(Constant.CURRENT_SONG_INDEX, 0)
        isPlaying = intent.getBooleanExtra(Constant.IS_PLAYING, true)

        setUpRecyclerView()
        bindMusicService()
        setupSearchListener()
    }
    private fun setUpRecyclerView() {
        songList.forEachIndexed { index, song ->
            originalIndexMap[song] = index
        }
        songListAdapter = SearchSongListAdapter(
            context = applicationContext,
            songList = commonSearchList,
            listener = this,
            originalIndexMap = originalIndexMap
        )

        binding.recyclerView.apply {
//            layoutManager = LinearLayoutManager(this@SongListActivity)
            adapter = songListAdapter
        }
        updateAdapterPlayingState()
    }

    private fun bindMusicService() {
        val intent = Intent(this, MusicService::class.java)
        bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
    }

    private fun unbindMusicService() {
        if (isBound) {
            unbindService(serviceConnection)
            isBound = false
        }
    }


    override fun onSongSelected(position: Int, originalIndex: Int) {
        val clickedSong = commonSearchList[position]
        val songIndexInOriginalList = songList.indexOf(clickedSong)
        val previousPlayingPosition = currentSongIndex
        currentSongIndex = songIndexInOriginalList
        if (isBound && musicService != null) {
            val intent = Intent(this, MusicService::class.java).apply {
                action = Constant.ACTION_INIT_SERVICE
                putParcelableArrayListExtra(Constant.SONG_LIST, songList)
                putExtra(Constant.CURRENT_INDEX, currentSongIndex)
                putExtra(Constant.AUTO_PLAY, true)
            }
            startService(intent)
            isPlaying = true
//            if (previousPlayingPosition != -1){
//                songListAdapter.notifyItemChanged(originalIndexMap.values.indexOf(previousPlayingPosition))
//            }
            songListAdapter.notifyItemChanged(originalIndexMap.values.indexOf(currentSongIndex))
            musicService?.updateNotificationFromActivity()
            musicService?.updateNotification()
        }
    }

    private fun updateAdapterPlayingState() {
        if (isBound && musicService != null) {
            musicService?.mediaPlayer?.let {
                isPlaying = it.isPlaying
            }
            songListAdapter.updatePlayingState(currentSongIndex, isPlaying)
        } else {
            songListAdapter.updatePlayingState(currentSongIndex, isPlaying)
        }
    }
    private fun setupSearchListener() {
        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                val searchText = s.toString()
                filterSearchList(searchText)
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun filterSearchList(searchText: String) {
        commonSearchList.clear()

        if (searchText.isNotEmpty()) {
            binding.ivClose.visibility = View.VISIBLE
            binding.ivClose.setOnClickListener {
                clearSearch()
            }
            val filteredList = songList.filter { song ->
                song.title?.lowercase()?.contains(searchText.lowercase()) == true ||
                        song.artist?.lowercase()?.contains(searchText.lowercase()) == true
            }
            commonSearchList.addAll(filteredList)
        } else {
            binding.ivClose.visibility = View.GONE
            commonSearchList.addAll(songList)
        }
        songListAdapter.notifyDataSetChanged()
    }

    private fun clearSearch() {
        binding.etSearch.text?.clear()
        filterSearchList("")
    }

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    override fun onStart() {
        super.onStart()
        val filter = IntentFilter(Constant.BROADCAST_PLAYBACK_STATE)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(playbackStateReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(playbackStateReceiver, filter)
        }
    }

    override fun onStop() {
        super.onStop()
        unregisterReceiver(playbackStateReceiver)
    }

    override fun onDestroy() {
        super.onDestroy()
        unbindMusicService()
        stopService(intent)
//        onStop()
//        stopService(Intent(this,MusicService::class.java))
    }
}