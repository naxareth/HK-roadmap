package com.second_year.hkroadmap.Views

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
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

    companion object {
        private const val MIN_PASSWORD_LENGTH = 8
        private const val MAX_PASSWORD_LENGTH = 20
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityForgotPasswordBinding.inflate(layoutInflater)
        setContentView(binding.root)

        currentEmail = intent.getStringExtra("email") ?: ""
        setupUI()
    }

    private fun setupUI() {
        if (currentEmail.isEmpty()) {
            setupEmailInputUI()
        } else {
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

            etEmail.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                override fun afterTextChanged(s: Editable?) {
                    tilEmail.error = null
                }
            })
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

            setupPasswordTextWatchers()
        }
    }

    private fun setupPasswordTextWatchers() {
        binding.apply {
            etNewPassword.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                override fun afterTextChanged(s: Editable?) {
                    tilNewPassword.error = null
                }
            })

            etConfirmPassword.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                override fun afterTextChanged(s: Editable?) {
                    tilConfirmPassword.error = null
                }
            })
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
                    binding.tilEmail.error = response.message
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to send OTP", e)
                hideProgress()
                handleError(e)
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
                    binding.tilNewPassword.error = response.message
                }
            } catch (e: Exception) {
                Log.e(TAG, "Password change failed", e)
                hideProgress()
                handleError(e)
            }
        }
    }

    private fun validateEmail(email: String): Boolean {
        when {
            email.isEmpty() -> {
                binding.tilEmail.error = "Email is required"
                return false
            }
            !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches() -> {
                binding.tilEmail.error = "Invalid email format"
                return false
            }
            !email.contains("@") -> {
                binding.tilEmail.error = "Email must contain @"
                return false
            }
        }
        return true
    }

    private fun validatePasswords(password: String, confirmPassword: String): Boolean {
        when {
            password.isEmpty() -> {
                binding.tilNewPassword.error = "Password is required"
                return false
            }
            password.length < MIN_PASSWORD_LENGTH -> {
                binding.tilNewPassword.error = "Password must be at least $MIN_PASSWORD_LENGTH characters"
                return false
            }
            password.length > MAX_PASSWORD_LENGTH -> {
                binding.tilNewPassword.error = "Password must be less than $MAX_PASSWORD_LENGTH characters"
                return false
            }
            !isValidPassword(password) -> {
                binding.tilNewPassword.error = "Password must contain uppercase, lowercase, number, and special character"
                return false
            }
            confirmPassword.isEmpty() -> {
                binding.tilConfirmPassword.error = "Please confirm your password"
                return false
            }
            password != confirmPassword -> {
                binding.tilConfirmPassword.error = "Passwords do not match"
                return false
            }
        }
        return true
    }

    private fun isValidPassword(password: String): Boolean {
        val pattern = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=!])(?=\\S+$).{$MIN_PASSWORD_LENGTH,$MAX_PASSWORD_LENGTH}$".toRegex()
        return pattern.matches(password)
    }

    private fun handleError(error: Exception) {
        val errorMessage = when (error) {
            is java.net.UnknownHostException -> "No internet connection"
            is java.net.SocketTimeoutException -> "Connection timed out"
            else -> "Error: ${error.message}"
        }
        showToast(errorMessage)
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