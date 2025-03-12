package com.second_year.hkroadmap.Views

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.second_year.hkroadmap.Api.Models.StudentRegisterRequest
import com.second_year.hkroadmap.Api.Interfaces.TokenManager
import com.second_year.hkroadmap.Api.Interfaces.RetrofitInstance
import com.second_year.hkroadmap.databinding.ActivitySignUpBinding
import kotlinx.coroutines.launch

class SignUpActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySignUpBinding
    private var progressBar: View? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySignUpBinding.inflate(layoutInflater)
        setContentView(binding.root)

        progressBar = binding.progressBar
        setupSignUpButton()
        setupNavigationListeners()
    }

    private fun setupNavigationListeners() {
        binding.textViewLogin.setOnClickListener {
            finish() // Return to LoginActivity
        }
    }

    private fun setupSignUpButton() {
        binding.buttonSignUp.setOnClickListener {
            if (validateInputs()) {
                performSignUp()
            }
        }
    }

    private fun validateInputs(): Boolean {
        val email = binding.etEmail.text.toString().trim()
        val username = binding.etUsername.text.toString().trim()
        val password = binding.etPassword.text.toString().trim()
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
            username.isEmpty() -> {
                binding.etUsername.error = "Username is required"
                return false
            }
            password.isEmpty() -> {
                binding.etPassword.error = "Password is required"
                return false
            }
            password.length < 6 -> {
                binding.etPassword.error = "Password must be at least 6 characters"
                return false
            }
            password != confirmPassword -> {
                binding.etConfirmPassword.error = "Passwords do not match"
                return false
            }
        }
        return true
    }

    private fun performSignUp() {
        showLoading(true)

        val signUpRequest = StudentRegisterRequest(
            name = binding.etUsername.text.toString().trim(),
            email = binding.etEmail.text.toString().trim(),
            password = binding.etPassword.text.toString().trim(),
            confirm_password = binding.etConfirmPassword.text.toString().trim()
        )

        lifecycleScope.launch {
            try {
                val response = RetrofitInstance.createApiService().studentRegister(signUpRequest)
                if (response.token != null) {
                    TokenManager.saveToken(this@SignUpActivity, response.token)
                    navigateToHome()
                } else {
                    showError(response.message)
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

    private fun showLoading(show: Boolean) {
        progressBar?.visibility = if (show) View.VISIBLE else View.GONE
        binding.buttonSignUp.isEnabled = !show
    }

    private fun navigateToHome() {
        startActivity(Intent(this, LoginActivity::class.java))
        finish()
    }
}