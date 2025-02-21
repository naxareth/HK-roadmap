package com.second_year.hkroadmap.Views

import android.os.Bundle
import android.content.Intent
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.second_year.hkroadmap.Api.Models.EmailRequest
import com.second_year.hkroadmap.Api.Interfaces.RetrofitInstance
import com.second_year.hkroadmap.databinding.EmailInputBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import android.widget.Toast
import android.util.Log

class EmailInputActivity : AppCompatActivity() {
    private lateinit var binding: EmailInputBinding
    private val TAG = "EmailInput"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = EmailInputBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupViews()
        Log.d(TAG, "Activity created")
    }

    private fun setupViews() {
        binding.apply {
            btnSendOtp.setOnClickListener {
                Log.d(TAG, "Send OTP button clicked")
                handleEmailSubmission()
            }
        }
    }

    private fun handleEmailSubmission() {
        val email = binding.etEmail.text.toString().trim()
        Log.d(TAG, "Handling email submission for: $email")

        if (!validateEmail(email)) return

        showProgress()

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val response = RetrofitInstance.createApiService()
                    .studentRequestOtp(EmailRequest(email))

                withContext(Dispatchers.Main) {
                    hideProgress()
                    Log.d(TAG, "API Response: ${response.message}")

                    if (response.message.contains("sent", ignoreCase = true)) {
                        Log.d(TAG, "OTP sent successfully")
                        showToast("OTP sent successfully")
                        navigateToOtpVerification(email)
                    } else {
                        Log.w(TAG, "Failed to send OTP: ${response.message}")
                        showToast(response.message)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "API Error: ${e.message}")
                withContext(Dispatchers.Main) {
                    hideProgress()
                    showToast("Failed to send OTP: ${e.message}")
                }
            }
        }
    }

    private fun navigateToOtpVerification(email: String) {
        try {
            Log.d(TAG, "Attempting to navigate to OTP verification")
            Intent(this, OtpVerificationActivity::class.java).also { intent ->
                intent.putExtra("email", email)
                startActivity(intent)
            }
            Log.d(TAG, "Navigation intent sent with email: $email")
        } catch (e: Exception) {
            Log.e(TAG, "Navigation Error: ${e.message}")
            e.printStackTrace()
            showToast("Error launching OTP verification")
        }
    }

    private fun validateEmail(email: String): Boolean {
        if (email.isEmpty()) {
            showToast("Please enter your email")
            return false
        }
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            showToast("Please enter a valid email")
            return false
        }
        return true
    }

    private fun showProgress() {
        binding.progressBar.visibility = View.VISIBLE
        binding.btnSendOtp.isEnabled = false
    }

    private fun hideProgress() {
        binding.progressBar.visibility = View.GONE
        binding.btnSendOtp.isEnabled = true
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}