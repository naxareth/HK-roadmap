package com.second_year.hkroadmap.Views

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.second_year.hkroadmap.Api.Models.EmailRequest
import com.second_year.hkroadmap.Api.Models.PasswordChangeRequest
import com.second_year.hkroadmap.Api.Interfaces.RetrofitInstance
import com.second_year.hkroadmap.databinding.ActivityForgotPasswordBinding
import kotlinx.coroutines.launch

class ForgotPasswordActivity : AppCompatActivity() {
    private lateinit var binding: ActivityForgotPasswordBinding
    private var progressBar: View? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityForgotPasswordBinding.inflate(layoutInflater)
        setContentView(binding.root)

        progressBar = binding.progressBar
        setupButtons()
    }

    private fun setupButtons() {
        binding.buttonSendResetLink.setOnClickListener {
            if (validateInputs()) {
                performPasswordReset()
            }
        }

        binding.textViewBackToLogin.setOnClickListener {
            finish()
        }
    }

    private fun validateInputs(): Boolean {
        val email = binding.etEmail.text.toString().trim()
        val newPassword = binding.etNewPassword.text.toString().trim()
        val confirmPassword = binding.etConfirmPassword.text.toString().trim()

        when {
            email.isEmpty() -> {
                binding.etEmail.error = "Email is required"
                return false
            }
            !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches() -> {
                binding.etEmail.error = "Invalid email format"
                return false
            }
            newPassword.isEmpty() -> {
                binding.etNewPassword.error = "New password is required"
                return false
            }
            newPassword.length < 6 -> {
                binding.etNewPassword.error = "Password must be at least 6 characters"
                return false
            }
            newPassword != confirmPassword -> {
                binding.etConfirmPassword.error = "Passwords do not match"
                return false
            }
        }
        return true
    }

    private fun performPasswordReset() {
        showLoading(true)
        val email = binding.etEmail.text.toString().trim()
        val newPassword = binding.etNewPassword.text.toString().trim()

        lifecycleScope.launch {
            try {
                // First request OTP
                val otpResponse = RetrofitInstance.createApiService()
                    .studentRequestOtp(EmailRequest(email))

                if (otpResponse.message.contains("success", ignoreCase = true)) {
                    // If OTP sent successfully, proceed with password change
                    val passwordChangeResponse = RetrofitInstance.createApiService()
                        .studentChangePassword(PasswordChangeRequest(
                            email = email,
                            new_password = newPassword
                        ))

                    showMessage(passwordChangeResponse.message)
                    if (passwordChangeResponse.message.contains("success", ignoreCase = true)) {
                        finish()
                    }
                } else {
                    showError(otpResponse.message)
                }
            } catch (e: Exception) {
                handleError(e)
            } finally {
                showLoading(false)
            }
        }
    }

    private fun handleError(error: Exception) {
        val errorMessage = when (error) {
            is java.net.UnknownHostException -> "No internet connection"
            is java.net.SocketTimeoutException -> "Connection timed out"
            else -> "Network error: ${error.message}"
        }
        showError(errorMessage)
    }

    private fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private fun showMessage(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private fun showLoading(show: Boolean) {
        progressBar?.visibility = if (show) View.VISIBLE else View.GONE
        binding.buttonSendResetLink.isEnabled = !show
    }
}