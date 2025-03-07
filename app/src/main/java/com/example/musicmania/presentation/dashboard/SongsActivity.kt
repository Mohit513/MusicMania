package com.example.musicmania.presentation.dashboard

import android.content.pm.ActivityInfo
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import com.example.musicmania.databinding.ActivitySongsBinding
import com.example.musicmania.presentation.bottom_sheet.SongListBottomSheetFragment

class SongsActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySongsBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivitySongsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setUpStatusBar()
        setUpListener()
    }

    private fun setUpListener(){
         binding.tvOpenSongList.setOnClickListener{
               val bottomSheetFragment = SongListBottomSheetFragment()
               bottomSheetFragment.show(supportFragmentManager,"myList")
         }
    }
    private fun setUpStatusBar() {
        window.apply {
            WindowCompat.setDecorFitsSystemWindows(this, false)
            ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content)) { view, insets ->
                val systemBarsInsets = insets.getInsets(WindowInsetsCompat.Type.systemBars())
                view.setPadding(
                    systemBarsInsets.left,
                    systemBarsInsets.top,
                    systemBarsInsets.right,
                    systemBarsInsets.bottom
                )
                insets
            }
        }
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT)
    }
}