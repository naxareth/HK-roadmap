package com.second_year.hkroadmap.Views

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import androidx.appcompat.app.AppCompatActivity
import com.second_year.hkroadmap.R
import com.second_year.hkroadmap.Api.Interfaces.TokenManager

class SplashScreen : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.splash_screen)

        Handler().postDelayed({
            // Check if user is logged in
            val token = TokenManager.getToken(this)
            val intent = if (token != null) {
                Intent(this, HomeActivity::class.java)
            } else {
                Intent(this, LoginActivity::class.java)
            }
            startActivity(intent)
            finish()
        }, 3000) // 3 seconds delay
    }
}