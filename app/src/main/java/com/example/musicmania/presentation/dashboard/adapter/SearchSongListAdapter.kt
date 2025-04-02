package com.example.musicmania.presentation.dashboard.adapter

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.musicmania.R
import com.example.musicmania.databinding.ItemSongsListBinding
import com.example.musicmania.presentation.bottom_sheet.model.SongListDataModel


class SearchSongListAdapter(
    private val context: Context,
    private val songList: ArrayList<SongListDataModel>,
    private val onItemClick: (Int) -> Unit,
    private val originalIndexMap: MutableMap<SongListDataModel, Int>
) : RecyclerView.Adapter<SearchSongListAdapter.ViewHolder>() {

    private var currentPlayingPosition = -1
    private var isCurrentlyPlaying = false

    inner class ViewHolder(val binding: ItemSongsListBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            ItemSongsListBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        )
    }

    override fun getItemCount() = songList.size

    @SuppressLint("NotifyDataSetChanged")
    override fun onBindViewHolder(holder: ViewHolder, @SuppressLint("RecyclerView") position: Int) {
        val song = songList[position]
        val originalIndex = originalIndexMap[song] ?: position

        holder.binding.apply {
            layoutItemSongList.tvTitle.text = song.title
            layoutItemSongList.tvSubTitle.text = song.artist

            ivPlayAndPause.setImageResource(
                when {
                    originalIndex == currentPlayingPosition && isCurrentlyPlaying -> R.drawable.ic_pause
                    else -> R.drawable.ic_play
                }
            )

            ivPlayAndPause.setBackgroundColor(
                when {
                    originalIndex == currentPlayingPosition && isCurrentlyPlaying -> ContextCompat.getColor(context, R.color.pomegranate)
                    else -> ContextCompat.getColor(context, R.color.woodsmoke)
                }
            )

            layoutItemSongList.tvTitle.setTextColor(
                when {
                    originalIndex == currentPlayingPosition && isCurrentlyPlaying -> ContextCompat.getColor(context, R.color.pomegranate)
                    else -> ContextCompat.getColor(context, R.color.dusty_gray)
                }
            )

            root.setOnClickListener {
                val previousPlayingPosition = currentPlayingPosition;
                currentPlayingPosition = originalIndex
                isCurrentlyPlaying = true
                onItemClick(position)
                if (previousPlayingPosition != -1){
                    notifyItemChanged(originalIndexMap.values.indexOf(previousPlayingPosition))
                }
                notifyItemChanged(position)
            }
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    fun updatePlayingState(originalIndex: Int, isPlaying: Boolean) {
        val previousPlayingPosition = currentPlayingPosition
        currentPlayingPosition = originalIndex
        isCurrentlyPlaying = isPlaying
        if (previousPlayingPosition != -1){
            notifyItemChanged(originalIndexMap.values.indexOf(previousPlayingPosition))
        }
        notifyItemChanged(originalIndexMap.values.indexOf(originalIndex))
    }
}