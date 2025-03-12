package com.example.musicmania.presentation.bottom_sheet

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.musicmania.R
import com.example.musicmania.databinding.FragmentSongListBottomSheetBinding
import com.example.musicmania.databinding.ItemSongsListBinding
import com.example.musicmania.presentation.bottom_sheet.adapter.SongListAdapter
import com.example.musicmania.presentation.bottom_sheet.model.SongListDataModel
import com.example.musicmania.presentation.dashboard.SongsActivity
import com.example.musicmania.utils.SongUtils
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class SongListBottomSheetFragment : BottomSheetDialogFragment(), SongListAdapter.selectedSong {

    private lateinit var binding: FragmentSongListBottomSheetBinding
    private val songItemList = ArrayList<SongListDataModel>()
    private lateinit var songListAdapter: SongListAdapter
    private var parentActivity: SongsActivity? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
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

    @SuppressLint("NotifyDataSetChanged")
    private fun loadSongs() {
        val contentResolver = context?.contentResolver
        if (contentResolver != null) {
            songItemList.clear()
            songItemList.addAll(SongUtils.getSongsFromDevice(contentResolver))
            setUpRecyclerView()
            songListAdapter.notifyDataSetChanged()
        }
    }

    override fun onSelectSongItem(position: Int) {
        parentActivity?.currentSong?.apply {
            title = songItemList[position].title
            songThumbnail = songItemList[position].songThumbnail
            subTitle = songItemList[position].subTitle
            icon = songItemList[position].icon
        }
        parentActivity?.currentSongIndex(position)

        dismiss()
    }


}
