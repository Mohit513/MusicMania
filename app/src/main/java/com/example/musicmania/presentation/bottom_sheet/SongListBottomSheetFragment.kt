package com.example.musicmania.presentation.bottom_sheet

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.musicmania.databinding.FragmentSongListBottomSheetBinding
import com.example.musicmania.presentation.bottom_sheet.adapter.SongListAdapter
import com.example.musicmania.presentation.bottom_sheet.model.SongListDataModel
import com.example.musicmania.presentation.interfaces.SongSelectionListener
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class SongListBottomSheetFragment(
    private val songList: ArrayList<SongListDataModel>,
    private val currentSongIndex: Int,
    private val isPlaying: Boolean
) : BottomSheetDialogFragment(), SongSelectionListener {

    private lateinit var binding: FragmentSongListBottomSheetBinding

    private var songListListener: SongListListener? = null
    private lateinit var songListAdapter: SongListAdapter

    interface SongListListener {
        fun onSelectSongItem(position: Int)
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is SongListListener) {
            songListListener = context
        } else {
            throw RuntimeException("$context must implement SongListListener")
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentSongListBottomSheetBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
    }

    private fun setupRecyclerView() {
        songListAdapter = SongListAdapter(
            context = requireContext(),
            songList = songList,
            listener = this
        )
        
        binding.recyclerViewBottomList.apply {
            adapter = songListAdapter
        }

        songListAdapter.updatePlayingState(currentSongIndex, isPlaying)
    }

    override fun onSongSelected(position: Int, originalIndex: Int) {
        songListListener?.onSelectSongItem(position)
        dismiss()
    }


}