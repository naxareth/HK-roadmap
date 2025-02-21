package com.second_year.hkroadmap.Views

import android.content.Intent
import android.os.Bundle
import android.util.Log
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
    private var currentEmail: String = ""
    private val TAG = "ForgotPassword"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityForgotPasswordBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Get email from intent if it exists
        currentEmail = intent.getStringExtra("email") ?: ""

        if (currentEmail.isEmpty()) {
            // Show email input UI if no email provided (coming from login page)
            setupEmailInputUI()
        } else {
            // Show password change UI if email exists (coming from OTP verification)
            setupPasswordChangeUI()
        }
    }

    private fun setupEmailInputUI() {
        binding.apply {
            tvTitle.text = "Forgot Password"
            tilEmail.visibility = View.VISIBLE
            tilNewPassword.visibility = View.GONE
            tilConfirmPassword.visibility = View.GONE

            buttonSendResetLink.text = "Send OTP"
            buttonSendResetLink.setOnClickListener { handleEmailSubmission() }

            textViewBackToLogin.setOnClickListener { navigateToLogin() }
        }
    }

    private fun setupPasswordChangeUI() {
        binding.apply {
            tvTitle.text = "Reset Password"
            tilEmail.visibility = View.GONE
            tilNewPassword.visibility = View.VISIBLE
            tilConfirmPassword.visibility = View.VISIBLE

            buttonSendResetLink.text = "Reset Password"
            buttonSendResetLink.setOnClickListener { handlePasswordChange() }

            textViewBackToLogin.setOnClickListener { navigateToLogin() }
        }
    }

    private fun handleEmailSubmission() {
        val email = binding.etEmail.text.toString().trim()

        if (!validateEmail(email)) return

        showProgress()

        lifecycleScope.launch {
            try {
                val response = RetrofitInstance.createApiService()
                    .studentRequestOtp(EmailRequest(email))

                hideProgress()

                if (response.message.contains("sent", ignoreCase = true)) {
                    showToast("OTP sent successfully")
                    navigateToOtpVerification(email)
                } else {
                    showToast(response.message)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to send OTP", e)
                hideProgress()
                showToast("Failed to send OTP: ${e.message}")
            }
        }
    }

    private fun handlePasswordChange() {
        val newPassword = binding.etNewPassword.text.toString()
        val confirmPassword = binding.etConfirmPassword.text.toString()

        if (!validatePasswords(newPassword, confirmPassword)) return

        showProgress()

        lifecycleScope.launch {
            try {
                val request = PasswordChangeRequest(currentEmail, newPassword)
                val response = RetrofitInstance.createApiService()
                    .changePassword(request)

                hideProgress()

                if (response.message.contains("success", ignoreCase = true)) {
                    showToast("Password changed successfully")
                    navigateToLogin()
                } else {
                    showToast(response.message)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Password change failed", e)
                hideProgress()
                showToast("Failed to change password: ${e.message}")
            }
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

    private fun validatePasswords(password: String, confirmPassword: String): Boolean {
        if (password.isEmpty() || confirmPassword.isEmpty()) {
            showToast("Please fill in all fields")
            return false
        }

        if (password.length < 6) {
            showToast("Password must be at least 6 characters")
            return false
        }

        if (password != confirmPassword) {
            showToast("Passwords do not match")
            return false
        }

        return true
    }

    private fun navigateToOtpVerification(email: String) {
        Intent(this, OtpVerificationActivity::class.java).also { intent ->
            intent.putExtra("email", email)
            startActivity(intent)
        }
    }

    private fun navigateToLogin() {
        Intent(this, LoginActivity::class.java).also { intent ->
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }
    }

    private fun showProgress() {
        binding.progressBar.visibility = View.VISIBLE
        binding.buttonSendResetLink.isEnabled = false
    }

    private fun hideProgress() {
        binding.progressBar.visibility = View.GONE
        binding.buttonSendResetLink.isEnabled = true
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}