package com.second_year.hkroadmap.Views

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.second_year.hkroadmap.R
import com.second_year.hkroadmap.Api.Interfaces.RetrofitInstance
import com.second_year.hkroadmap.Api.Interfaces.TokenManager
import com.second_year.hkroadmap.databinding.ActivityHomeBinding
import kotlinx.coroutines.launch

class HomeActivity : AppCompatActivity() {
    private lateinit var binding: ActivityHomeBinding
    private val TAG = "HomeActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        setupLogoutButton()
    }

    private fun setupLogoutButton() {
        binding.btnLogout.setOnClickListener {
            handleLogout()
        }
    }

    private fun handleLogout() {
        val token = TokenManager.getToken(this)
        if (token == null) {
            Log.w(TAG, "No token found, proceeding with local logout")
            performLocalLogout()
            return
        }

        lifecycleScope.launch {
            try {
                val authToken = "Bearer $token"
                val response = RetrofitInstance.createApiService().studentLogout(authToken)
                Log.d(TAG, "Logout response: ${response.message}")

                if (response.message.contains("success", ignoreCase = true)) {
                    performLocalLogout()
                } else {
                    showToast("Logout failed: ${response.message}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Logout failed", e)
                showToast("Logout failed: ${e.message}")
                // Perform local logout anyway in case of network error
                performLocalLogout()
            }
        }
    }

    private fun performLocalLogout() {
        TokenManager.clearToken(this)
        navigateToLogin()
    }

    private fun navigateToLogin() {
        Intent(this, LoginActivity::class.java).also { intent ->
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}