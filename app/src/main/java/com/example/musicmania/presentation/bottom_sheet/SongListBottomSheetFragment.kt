package com.example.musicmania.presentation.bottom_sheet

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import android.os.PatternMatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.musicmania.databinding.FragmentSongListBottomSheetBinding
import com.example.musicmania.presentation.bottom_sheet.adapter.SongListAdapter
import com.example.musicmania.presentation.bottom_sheet.model.SongListDataModel
import com.example.musicmania.presentation.service.MusicService
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class SongListBottomSheetFragment(private val songList: ArrayList<SongListDataModel>) :
    BottomSheetDialogFragment(), SongListAdapter.OnItemClickListener {

    private var _binding: FragmentSongListBottomSheetBinding? = null
    private val binding get() = _binding!!
    private lateinit var songListAdapter: SongListAdapter
    private var songListListener: SongListListener? = null

    interface SongListListener {
        fun onSelectSongItem(position: Int)
    }

    private val playbackReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == MusicService.BROADCAST_PLAYBACK_STATE) {
                val isPlaying = intent.getBooleanExtra("isPlaying", false)
                val currentIndex = intent.getIntExtra("currentIndex", -1)
                if (currentIndex != -1) {
                    updatePlayingState(currentIndex, isPlaying)
                }
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSongListBottomSheetBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        songListListener = activity as? SongListListener
        registerReceiver()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        try {
            context?.unregisterReceiver(playbackReceiver)
        } catch (e: IllegalArgumentException) {
            // Receiver not registered
        }
    }

    private fun setupRecyclerView() {
        songListAdapter = SongListAdapter(songList, this)
        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = songListAdapter
        }
    }

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    private fun registerReceiver() {
        val filter = IntentFilter().apply {
            addAction(MusicService.BROADCAST_PLAYBACK_STATE)
            addDataScheme("package")
            addDataPath(requireContext().packageName, PatternMatcher.PATTERN_LITERAL)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context?.registerReceiver(playbackReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            context?.registerReceiver(playbackReceiver, filter)
        }
    }

    private fun updatePlayingState(index: Int, isPlaying: Boolean) {
        songListAdapter.updatePlayingState(index, isPlaying)
    }

    override fun onItemClick(position: Int) {
        songListListener?.onSelectSongItem(position)
        dismiss()
    }
}