package com.example.musicmania.presentation.bottom_sheet.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.musicmania.R
import com.example.musicmania.databinding.ItemSongsListBinding
import com.example.musicmania.presentation.bottom_sheet.model.SongListDataModel

class SongListAdapter(
    private val songList: ArrayList<SongListDataModel>,
    private val onItemClick: (Int) -> Unit
) : RecyclerView.Adapter<SongListAdapter.ViewHolder>() {

    private var currentPlayingPosition = -1
    private var isCurrentlyPlaying = false

    inner class ViewHolder(private val binding: ItemSongsListBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(song: SongListDataModel, position: Int) {
            binding.apply {
                layoutItemSongList.tvTitle.text = song.title
                layoutItemSongList.tvSubTitle.text = song.subTitle
                
                // Update play/pause icon based on current state
                ivPlayAndPause.setImageResource(
                    when {
                        position == currentPlayingPosition && isCurrentlyPlaying -> R.drawable.ic_pause
                        else -> R.drawable.ic_play
                    }
                )

                root.setOnClickListener {
                    onItemClick(position)
                    currentPlayingPosition = position
                    isCurrentlyPlaying = true
                    notifyDataSetChanged()
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            ItemSongsListBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )
    }

    override fun getItemCount() = songList.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(songList[position], position)
    }

    fun updatePlayingState(position: Int, isPlaying: Boolean) {
        currentPlayingPosition = position
        isCurrentlyPlaying = isPlaying
        notifyDataSetChanged()
    }
}
