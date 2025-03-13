package com.example.musicmania.presentation.bottom_sheet

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.musicmania.databinding.FragmentSongListBottomSheetBinding
import com.example.musicmania.presentation.bottom_sheet.adapter.SongListAdapter
import com.example.musicmania.presentation.bottom_sheet.model.SongListDataModel
import com.example.musicmania.presentation.dashboard.SongsActivity
import com.example.musicmania.utils.SongUtils
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class SongListBottomSheetFragment(private val songItemList: ArrayList<SongListDataModel>) : BottomSheetDialogFragment(), SongListAdapter.selectedSong {

    private lateinit var binding: FragmentSongListBottomSheetBinding
    private lateinit var songListAdapter: SongListAdapter
    private var parentActivity: SongsActivity? = null
    private var selectedPosition = -1

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentSongListBottomSheetBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        init()
        loadSongs()
    }

    private fun init() {
        parentActivity = activity as SongsActivity
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
        selectedPosition = position
        parentActivity?.currentSong?.apply {
            title = songItemList[position].title
            songThumbnail = songItemList[position].songThumbnail
            subTitle = songItemList[position].subTitle
        }
        parentActivity?.currentSongIndex(position)
        dismiss()
    }
}