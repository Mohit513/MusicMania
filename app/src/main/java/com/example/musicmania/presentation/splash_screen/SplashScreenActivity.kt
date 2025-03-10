package com.example.musicmania.presentation.splash_screen

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.musicmania.MainActivity
import com.example.musicmania.R
import com.example.musicmania.databinding.ActivitySplashScreenBinding
import com.example.musicmania.presentation.dashboard.SongsActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Delay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@SuppressLint("CustomSplashScreen")
class SplashScreenActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySplashScreenBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySplashScreenBinding.inflate(layoutInflater)
        enableEdgeToEdge()
        setContentView(binding.root)

        CoroutineScope(Dispatchers.Main).launch{
            delay(timeMillis = 3000)
            val intent = Intent(this@SplashScreenActivity,SongsActivity::class.java)
            startActivity(intent)
            finish()
        }

    }
}