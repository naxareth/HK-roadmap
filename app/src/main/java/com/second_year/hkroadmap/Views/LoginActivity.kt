package com.second_year.hkroadmap.Views

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.second_year.hkroadmap.Api.Models.LoginRequest
import com.second_year.hkroadmap.Api.Interfaces.TokenManager
import com.second_year.hkroadmap.Api.Interfaces.RetrofitInstance
import com.second_year.hkroadmap.databinding.ActivityLoginBinding
import kotlinx.coroutines.launch

class LoginActivity : AppCompatActivity() {
    private lateinit var binding: ActivityLoginBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupLoginButton()
        setupNavigationListeners()
    }

    private fun setupNavigationListeners() {
        binding.textViewSignUp.setOnClickListener {
            startActivity(Intent(this, SignUpActivity::class.java))
        }

        binding.textViewForgotPassword.setOnClickListener {
            startActivity(Intent(this, ForgotPasswordActivity::class.java))
        }
    }

    private fun setupLoginButton() {
        binding.btnLogin.setOnClickListener {
            Log.d("LoginActivity", "Login button clicked")
            if (validateInputs()) {
                performLogin()
            } else {
                Log.w("LoginActivity", "Invalid inputs")
            }
        }
    }

    private fun validateInputs(): Boolean {
        val email = binding.etEmail.text.toString().trim()
        val password = binding.etPassword.text.toString().trim()

        when {
            email.isEmpty() -> {
                binding.etEmail.error = "Email is required"
                return false
            }
            !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches() -> {
                binding.etEmail.error = "Invalid email format"
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
        }
        return true
    }

    private fun performLogin() {
        Log.d("LoginActivity", "Starting login process")
        showLoading(true)

        val loginRequest = LoginRequest(
            email = binding.etEmail.text.toString().trim(),
            password = binding.etPassword.text.toString().trim()
        )

        lifecycleScope.launch {
            try {
                val response = RetrofitInstance.createApiService().studentLogin(loginRequest)
                if (response.token != null) {
                    Log.d("LoginActivity", "Login successful, token received")
                    TokenManager.saveToken(this@LoginActivity, response.token)
                    navigateToHome()
                } else {
                    Log.w("LoginActivity", "Login failed: ${response.message}")
                    showError(response.message)
                }
            } catch (e: retrofit2.HttpException) {
                val errorBody = e.response()?.errorBody()?.string()
                Log.e("LoginActivity", "HTTP error during login: $errorBody", e)
                showError("HTTP error: $errorBody")
            } catch (e: Exception) {
                Log.e("LoginActivity", "Exception during login", e)
                handleError(e)
            } finally {
                Log.d("LoginActivity", "Login process ended")
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
        Log.e("LoginActivity", "Error occurred: $errorMessage", error)
        showError(errorMessage)
    }

    private fun showError(message: String) {
        Toast.makeText(this@LoginActivity, message, Toast.LENGTH_SHORT).show()
    }

    private fun showLoading(show: Boolean) {
        binding.progressBar.visibility = if (show) View.VISIBLE else View.GONE
        binding.btnLogin.isEnabled = !show
    }

    private fun navigateToHome() {
        // Navigate to the home activity
        Log.d("LoginActivity", "Navigating to HomeActivity")
        startActivity(Intent(this, HomeActivity::class.java))
        finish()
    }
}