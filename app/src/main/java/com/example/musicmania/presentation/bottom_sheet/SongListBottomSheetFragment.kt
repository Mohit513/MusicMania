package com.example.musicmania.presentation.bottom_sheet

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.musicmania.databinding.FragmentSongListBottomSheetBinding
import com.example.musicmania.presentation.bottom_sheet.adapter.SongListAdapter
import com.example.musicmania.presentation.bottom_sheet.model.SongListDataModel
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class SongListBottomSheetFragment(
    private val songList: ArrayList<SongListDataModel>,
    private val currentSongIndex: Int,
    private val isPlaying: Boolean
) : BottomSheetDialogFragment() {

    private var _binding: FragmentSongListBottomSheetBinding? = null
    private val binding get() = _binding!!
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
        _binding = FragmentSongListBottomSheetBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
    }

    private fun setupRecyclerView() {
        songListAdapter = SongListAdapter(songList) { position ->
            songListListener?.onSelectSongItem(position)
            dismiss()
        }
        
        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = songListAdapter
        }

        // Update the current playing state
        songListAdapter.updatePlayingState(currentSongIndex, isPlaying)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val TAG = "SongListBottomSheetFragment"
    }
}