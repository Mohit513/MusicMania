package com.example.musicmania.presentation.splash_screen

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.musicmania.MainActivity
import com.example.musicmania.R
import com.example.musicmania.databinding.ActivitySplashScreenBinding
import com.example.musicmania.presentation.dashboard.SongsActivity

class SplashScreenActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySplashScreenBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySplashScreenBinding.inflate(layoutInflater)
        enableEdgeToEdge()
        setContentView(binding.root)

        val intent = Intent(this,SongsActivity::class.java)
        startActivity(intent)
    }
}