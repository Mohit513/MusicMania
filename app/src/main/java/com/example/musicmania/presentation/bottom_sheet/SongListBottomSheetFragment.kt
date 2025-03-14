package com.example.musicmania.presentation.bottom_sheet

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Context.RECEIVER_NOT_EXPORTED
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.musicmania.databinding.FragmentSongListBottomSheetBinding
import com.example.musicmania.presentation.bottom_sheet.adapter.SongListAdapter
import com.example.musicmania.presentation.bottom_sheet.model.SongListDataModel
import com.example.musicmania.presentation.dashboard.SongsActivity
import com.example.musicmania.presentation.service.MusicService
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class SongListBottomSheetFragment(private val songItemList: ArrayList<SongListDataModel>) : BottomSheetDialogFragment(), SongListAdapter.selectedSong {

    private lateinit var binding: FragmentSongListBottomSheetBinding
    private lateinit var songListAdapter: SongListAdapter
    private var parentActivity: SongsActivity? = null
    private var selectedPosition = -1

    private val playbackReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == MusicService.BROADCAST_PLAYBACK_STATE) {
                val isPlaying = intent.getBooleanExtra("isPlaying", false)
                val currentIndex = intent.getIntExtra("currentIndex", -1)
                if (currentIndex != -1) {
                    updatePlayPauseIcon(currentIndex, isPlaying)
                }
            }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentSongListBottomSheetBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        init()
        loadSongs()
    }

    override fun onResume() {
        super.onResume()
        registerReceiver()
    }

    override fun onPause() {
        super.onPause()
        try {
            context?.unregisterReceiver(playbackReceiver)
        } catch (e: IllegalArgumentException) {
            // Receiver not registered
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        try {
            context?.unregisterReceiver(playbackReceiver)
        } catch (e: IllegalArgumentException) {
            // Receiver not registered
        }
    }

    private fun init() {
        parentActivity = activity as SongsActivity
    }

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    private fun registerReceiver() {
        val filter = IntentFilter(MusicService.BROADCAST_PLAYBACK_STATE)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context?.registerReceiver(playbackReceiver, filter, RECEIVER_NOT_EXPORTED)
        } else {
            context?.registerReceiver(playbackReceiver, filter)
        }
    }

    private fun setUpRecyclerView() {
        songListAdapter = SongListAdapter(songItemList, this)
        binding.recyclerView.adapter = songListAdapter
    }

    private fun loadSongs() {
        setUpRecyclerView()
    }

    fun updatePlayPauseIcon(index: Int, isPlaying: Boolean) {
        if (index != -1 && index < songItemList.size) {
            songItemList.forEachIndexed { position, item ->
                item.isPlaying = position == index && isPlaying
            }
            songListAdapter.notifyDataSetChanged()
        }
    }

    override fun onSelectSongItem(position: Int) {
        if (position < 0 || position >= songItemList.size) return
        
        selectedPosition = position
        parentActivity?.currentSong?.apply {
            title = songItemList[position].title
            songThumbnail = songItemList[position].songThumbnail
            subTitle = songItemList[position].subTitle
            icon = songItemList[position].icon
            artist = songItemList[position].artist
        }
        parentActivity?.currentSongIndex(position)
        dismiss()
    }
}