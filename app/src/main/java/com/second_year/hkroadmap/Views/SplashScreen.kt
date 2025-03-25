package com.second_year.hkroadmap.Views

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import com.second_year.hkroadmap.R
import com.second_year.hkroadmap.Api.Interfaces.TokenManager

class SplashScreen : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.splash_screen)

        Handler(Looper.getMainLooper()).postDelayed({
            // First check if user is already logged in
            val token = TokenManager.getToken(this)

            if (token != null) {
                // User is already logged in, go directly to HomeActivity
                startActivity(Intent(this, HomeActivity::class.java))
            } else {
                // User is not logged in, show welcome screen
                startActivity(Intent(this, WelcomeActivity::class.java))
            }

            finish()
        }, 3000) // 3 seconds delay
    }
}