package com.example.musicmania.presentation.bottom_sheet.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.musicmania.databinding.ItemSongsListBinding
import com.example.musicmania.presentation.bottom_sheet.model.SongListDataModel

class SongListAdapter(
    var songItemList: ArrayList<SongListDataModel>,
    private val itemClickListener: selectedSong
) : RecyclerView.Adapter<SongListAdapter.SongViewHolder>() {

    class SongViewHolder(val binding: ItemSongsListBinding) :
        RecyclerView.ViewHolder(binding.root) {
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SongViewHolder {
        val binding =
            ItemSongsListBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return SongViewHolder(binding)
    }

    override fun getItemCount(): Int {
        return songItemList.size
    }

    override fun onBindViewHolder(holder: SongViewHolder, position: Int) {
        val item = songItemList[position]
        holder.binding.layoutItemSongList.apply {
            tvTitle.text = item.title
            tvSubTitle.text = item.subTitle

            root.setOnClickListener {
                itemClickListener.onSelectSongItem(position)
            }
        }

    }

    interface selectedSong {
        fun onSelectSongItem(position: Int)
    }
}
