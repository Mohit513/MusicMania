package com.example.musicmania.presentation.bottom_sheet.adapter

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.musicmania.R
import com.example.musicmania.databinding.ItemSongsListBinding
import com.example.musicmania.presentation.bottom_sheet.model.SongListDataModel

class SongListAdapter(
    private val context: Context,
    private val songList: ArrayList<SongListDataModel>,
    private val onItemClick: (Int) -> Unit
) : RecyclerView.Adapter<SongListAdapter.ViewHolder>() {

    private var currentPlayingPosition = -1
    private var isCurrentlyPlaying = false

    inner class ViewHolder(val binding: ItemSongsListBinding) : RecyclerView.ViewHolder(binding.root) {
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            ItemSongsListBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        )
    }

    override fun getItemCount() = songList.size

    override fun onBindViewHolder(holder: ViewHolder, @SuppressLint("RecyclerView") position: Int) {
        val song = songList[position]

        holder.binding.apply {
            layoutItemSongList.tvTitle.text = song.title
            layoutItemSongList.tvSubTitle.text = song.artist

            ivPlayAndPause.setImageResource(
                when {
                    position == currentPlayingPosition && isCurrentlyPlaying -> R.drawable.ic_pause
                    else -> R.drawable.ic_play
                }
            )

            ivPlayAndPause.setBackgroundColor(
                when {
                    position == currentPlayingPosition && isCurrentlyPlaying -> ContextCompat.getColor(context, R.color.pomegranate)
                    else -> ContextCompat.getColor(context, R.color.woodsmoke)
                }
            )

            layoutItemSongList.tvTitle.setTextColor(
                when {
                    position == currentPlayingPosition && isCurrentlyPlaying -> ContextCompat.getColor(context, R.color.pomegranate)
                    else -> ContextCompat.getColor(context, R.color.dusty_gray)
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

    @SuppressLint("NotifyDataSetChanged")
    fun updatePlayingState(position: Int, isPlaying: Boolean) {
        currentPlayingPosition = position
        isCurrentlyPlaying = isPlaying
        notifyDataSetChanged()
    }
}

