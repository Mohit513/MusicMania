package com.example.musicmania.presentation.dashboard.adapter

import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.musicmania.R
import com.example.musicmania.databinding.ItemSongsListBinding
import com.example.musicmania.presentation.bottom_sheet.model.SongListDataModel
import com.example.musicmania.presentation.interfaces.SongSelectionListener

class SearchSongListAdapter(
    private val context: Context,
    private val songList: ArrayList<SongListDataModel>,
    private val listener: SongSelectionListener,
    private val originalIndexMap: MutableMap<SongListDataModel, Int>
) : RecyclerView.Adapter<SearchSongListAdapter.ViewHolder>() {

    private var currentPlayingOriginalIndex = -1
    private var isCurrentlyPlaying = false
    private val animationDuration = 200L

    inner class ViewHolder(val binding: ItemSongsListBinding) : RecyclerView.ViewHolder(binding.root) {
        private var colorAnimator: ValueAnimator? = null

        fun animateStateChange(isPlaying: Boolean) {
            colorAnimator?.cancel()
            colorAnimator = null

            val pomegranateColor = ContextCompat.getColor(context, R.color.pomegranate)
            val woodsmokeColor = ContextCompat.getColor(context, R.color.woodsmoke)
            val dustyGrayColor = ContextCompat.getColor(context, R.color.dusty_gray)

            binding.ivPlayAndPause.animate()
                .alpha(0f)
                .setDuration(animationDuration / 2)
                .withEndAction {
                    binding.ivPlayAndPause.setImageResource(
                        if (isPlaying) R.drawable.ic_pause else R.drawable.ic_play
                    )
                    binding.ivPlayAndPause.setBackgroundColor(
                        if (isPlaying) pomegranateColor else woodsmokeColor
                    )
                    binding.layoutItemSongList.tvTitle.setTextColor(
                        if (isPlaying) pomegranateColor else dustyGrayColor
                    )
                    binding.ivPlayAndPause.animate()
                        .alpha(1f)
                        .setDuration(animationDuration / 2)
                        .start()
                }
                .start()
        }

        fun cleanup() {
            colorAnimator?.cancel()
            colorAnimator = null
            binding.ivPlayAndPause.animate().cancel()
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            ItemSongsListBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        )
    }

    override fun getItemCount() = songList.size

    override fun onBindViewHolder(holder: ViewHolder, @SuppressLint("RecyclerView") position: Int) {
        val song = songList[position]
        val originalIndex = originalIndexMap[song] ?: position

        holder.binding.apply {
            layoutItemSongList.tvTitle.text = song.title
            layoutItemSongList.tvSubTitle.text = song.artist

            val isCurrentSong = originalIndex == currentPlayingOriginalIndex
            ivPlayAndPause.setImageResource(
                if (isCurrentSong) {
                    if (isCurrentlyPlaying) R.drawable.ic_pause else R.drawable.ic_play
                } else {
                    R.drawable.ic_play
                }
            )
            ivPlayAndPause.setBackgroundColor(
                ContextCompat.getColor(
                    context,
                    if (isCurrentSong) R.color.pomegranate else R.color.woodsmoke
                )
            )
            layoutItemSongList.tvTitle.setTextColor(
                ContextCompat.getColor(
                    context,
                    if (isCurrentSong) R.color.pomegranate else R.color.dusty_gray
                )
            )

            root.setOnClickListener {
                listener.onSongSelected(position, originalIndex)
            }
        }
    }

    override fun onViewDetachedFromWindow(holder: ViewHolder) {
        super.onViewDetachedFromWindow(holder)
        holder.cleanup()
    }

    @SuppressLint("NotifyDataSetChanged")
    fun updatePlayingState(originalIndex: Int, isPlaying: Boolean) {
        val oldPlayingOriginalIndex = currentPlayingOriginalIndex
        currentPlayingOriginalIndex = originalIndex
        isCurrentlyPlaying = isPlaying

        val itemsToUpdate = mutableSetOf<Int>()
        if (oldPlayingOriginalIndex != -1) {
            val oldPosition = songList.indexOfFirst { originalIndexMap[it] == oldPlayingOriginalIndex }
            if (oldPosition != -1) {
                itemsToUpdate.add(oldPosition)
            }
        }
        if (currentPlayingOriginalIndex != -1) {
            val newPosition = songList.indexOfFirst { originalIndexMap[it] == currentPlayingOriginalIndex }
            if (newPosition != -1) {
                itemsToUpdate.add(newPosition)
            }
        }

        itemsToUpdate.forEach { notifyItemChanged(it) }
    }
}