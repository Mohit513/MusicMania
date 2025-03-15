package com.example.musicmania.presentation.bottom_sheet.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.musicmania.R
import com.example.musicmania.databinding.ItemSongsListBinding
import com.example.musicmania.presentation.bottom_sheet.model.SongListDataModel

class SongListAdapter(
    private val songList: ArrayList<SongListDataModel>,
    private val itemClickListener: OnItemClickListener
) : RecyclerView.Adapter<SongListAdapter.ViewHolder>() {

    interface OnItemClickListener {
        fun onItemClick(position: Int)
    }

    inner class ViewHolder(private val binding: ItemSongsListBinding) :
        RecyclerView.ViewHolder(binding.root) {

        init {
            binding.root.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    itemClickListener.onItemClick(position)
                }
            }
        }

        fun bind(song: SongListDataModel) {
            binding.apply {
                layoutItemSongList.tvTitle.text = song.title ?: "Unknown"
                layoutItemSongList.tvSubTitle.text = song.artist ?: "Unknown Artist"
                ivPlayAndPause.setImageResource(
                    if (song.isPlaying) R.drawable.ic_pause else R.drawable.ic_play
                )
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemSongsListBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(songList[position])
    }

    override fun getItemCount(): Int = songList.size

    fun updatePlayingState(position: Int, isPlaying: Boolean) {
        songList.forEachIndexed { index, song ->
            song.isPlaying = index == position && isPlaying
        }
        notifyDataSetChanged()
    }
}
