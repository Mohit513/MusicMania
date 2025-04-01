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
import com.example.musicmania.presentation.bottom_sheet.adapter.SongListAdapter
import com.example.musicmania.presentation.bottom_sheet.model.SongListDataModel
import com.example.musicmania.presentation.service.MusicService

class SongListActivity : BaseActivity() {

    private var songList: ArrayList<SongListDataModel> = arrayListOf()
    private var commonSearchList: ArrayList<SongListDataModel> = arrayListOf()
    private var currentSongIndex: Int = 0
    private var isPlaying: Boolean = true
    private lateinit var binding: ActivitySongListBinding
    private lateinit var songListAdapter: SongListAdapter
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
                val isPlaying = intent.getBooleanExtra("isPlaying", false)
                val currentIndex = intent.getIntExtra("currentIndex", 0)

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

        songList = intent.getParcelableArrayListExtra("songList") ?: arrayListOf()
        commonSearchList.addAll(songList)
        currentSongIndex = intent.getIntExtra("currentSongIndex", 0)
        isPlaying = intent.getBooleanExtra("isPlaying", true)

        setUpRecyclerView()
        bindMusicService()
        setupSearchListener()
    }

    private fun setUpRecyclerView() {
        songListAdapter = SongListAdapter(applicationContext, commonSearchList) { position ->
            onSongItemClicked(position)
        }
        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(this@SongListActivity)
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

    private fun onSongItemClicked(position: Int) {
        val clickedSong = commonSearchList[position]
        val songIndexInOriginalList = songList.indexOf(clickedSong)
        currentSongIndex = songIndexInOriginalList
        if (isBound && musicService != null) {
            val intent = Intent(this, MusicService::class.java).apply {
                action = Constant.ACTION_INIT_SERVICE
                putParcelableArrayListExtra("songList", songList)
                putExtra("currentIndex", currentSongIndex)
                putExtra("autoPlay", true)
            }
            startService(intent)
            isPlaying = true
            updateAdapterPlayingState()
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

//    private fun showNoDataLayout(isEmpty: Boolean) {
//        binding.noDataLayout.visibility = if (isEmpty) View.VISIBLE else View.GONE
//    }

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
    }
}
