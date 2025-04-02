package com.example.musicmania.base

import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.os.Build
import android.os.Build.VERSION
import android.os.Bundle
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import com.example.musicmania.R
import com.example.musicmania.presentation.service.MusicService

open class BaseActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            enableEdgeToEdge(
                statusBarStyle = SystemBarStyle.light(
                    ContextCompat.getColor(
                        this,
                        R.color.color_primary
                    ), ContextCompat.getColor(this, R.color.color_primary)
                ),
                navigationBarStyle = SystemBarStyle.light(
                    ContextCompat.getColor(
                        this,
                        R.color.color_primary
                    ), ContextCompat.getColor(this, R.color.color_primary)
                )
            )
        }

        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LOCKED
        if (resources.configuration.orientation != Configuration.ORIENTATION_PORTRAIT) {
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }

        setUpStatusBar()
    }

    private fun setUpStatusBar() {
        window.apply {
            // for adding space for android 15
            WindowCompat.setDecorFitsSystemWindows(this, false)

            // Apply insets dynamically
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

