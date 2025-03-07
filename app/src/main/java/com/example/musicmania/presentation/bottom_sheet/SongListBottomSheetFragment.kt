package com.example.musicmania.presentation.bottom_sheet

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.musicmania.R
import com.example.musicmania.databinding.FragmentSongListBottomSheetBinding
import com.example.musicmania.presentation.bottom_sheet.adapter.SongListAdapter
import com.example.musicmania.presentation.bottom_sheet.model.SongListDataModel
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class SongListBottomSheetFragment : BottomSheetDialogFragment() {

    private lateinit var binding: FragmentSongListBottomSheetBinding
    private val songItemList = ArrayList<SongListDataModel>()
    private lateinit var songListAdapter: SongListAdapter
    private var songSelectionListener : SongSelectionListener? = null

            interface SongSelectionListener{
                fun onSongSelected(songTitle : String,songThumbnails : Int)
            }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if(context is SongSelectionListener){
            songSelectionListener = context
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentSongListBottomSheetBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setUpRecyclerView()
        setUpDummyData()
    }

    private fun setUpRecyclerView() {
        songListAdapter = SongListAdapter(songItemList, itemClickListener = { songTitle ->
            val dummyThumbnail = R.drawable.app_logo
            songSelectionListener?.onSongSelected(songTitle,dummyThumbnail)
        })
        dismiss()
        binding.recyclerView.adapter = songListAdapter
    }

    private fun setUpDummyData() {
        songItemList.apply {
            add(SongListDataModel(title = "Song 1", subTitle = "Artist 1"))
            add(SongListDataModel(title = "Song 2", subTitle = "Artist 2"))
            add(SongListDataModel(title = "Song 3", subTitle = "Artist 3"))
            add(SongListDataModel(title = "Song 4", subTitle = "Artist 4"))
            add(SongListDataModel(title = "Song 5", subTitle = "Artist 5"))
        }
        songListAdapter.notifyDataSetChanged()
    }
}
