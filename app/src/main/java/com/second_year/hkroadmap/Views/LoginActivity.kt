package com.second_year.hkroadmap.Views

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.snackbar.Snackbar
import com.second_year.hkroadmap.Api.Models.LoginRequest
import com.second_year.hkroadmap.Api.Interfaces.TokenManager
import com.second_year.hkroadmap.Api.Interfaces.RetrofitInstance
import com.second_year.hkroadmap.databinding.ActivityLoginBinding
import kotlinx.coroutines.launch
import org.json.JSONObject
import retrofit2.HttpException

class LoginActivity : AppCompatActivity() {
    private lateinit var binding: ActivityLoginBinding
    private val TAG = "LoginActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupLoginButton()
        setupNavigationListeners()
        setupInputListeners()
    }

    private fun setupNavigationListeners() {
        binding.textViewSignUp.setOnClickListener {
            startActivity(Intent(this, SignUpActivity::class.java))
        }

        binding.textViewForgotPassword.setOnClickListener {
            startActivity(Intent(this, ForgotPasswordActivity::class.java))
        }

        // Add welcome screen navigation
        binding.viewWelcomeScreen.setOnClickListener {
            startActivity(Intent(this, WelcomeActivity::class.java))
        }
    }

    private fun setupInputListeners() {
        // Clear errors when user starts typing
        binding.etEmail.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) binding.etEmail.error = null
        }

        binding.etPassword.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) binding.etPassword.error = null
        }
    }

    private fun setupLoginButton() {
        binding.btnLogin.setOnClickListener {
            Log.d(TAG, "Login button clicked")
            if (validateInputs()) {
                performLogin()
            } else {
                Log.w(TAG, "Invalid inputs")
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
        Log.d(TAG, "Starting login process")
        showLoading(true)

        val loginRequest = LoginRequest(
            email = binding.etEmail.text.toString().trim(),
            password = binding.etPassword.text.toString().trim()
        )

        lifecycleScope.launch {
            try {
                val response = RetrofitInstance.createApiService().studentLogin(loginRequest)

                if (response.token != null) {
                    Log.d(TAG, "Login successful, token received")
                    TokenManager.saveToken(this@LoginActivity, response.token)
                    navigateToHome()
                } else {
                    Log.w(TAG, "Login failed: ${response.message}")

                    // Handle specific error messages
                    when {
                        response.message.contains("password", ignoreCase = true) -> {
                            binding.etPassword.error = response.message
                        }
                        response.message.contains("email", ignoreCase = true) ||
                                response.message.contains("user", ignoreCase = true) -> {
                            binding.etEmail.error = response.message
                        }
                        else -> {
                            showError(response.message)
                        }
                    }
                }
            } catch (e: HttpException) {
                Log.e(TAG, "HTTP error during login: ${e.code()}", e)

                try {
                    val errorBody = e.response()?.errorBody()?.string()
                    if (!errorBody.isNullOrEmpty()) {
                        val errorJson = JSONObject(errorBody)
                        val errorMessage = errorJson.optString("message", "Unknown error")

                        // Handle specific error messages from error body
                        when {
                            errorMessage.contains("password", ignoreCase = true) -> {
                                binding.etPassword.error = errorMessage
                            }
                            errorMessage.contains("email", ignoreCase = true) ||
                                    errorMessage.contains("user", ignoreCase = true) -> {
                                binding.etEmail.error = errorMessage
                            }
                            else -> {
                                showError(errorMessage)
                            }
                        }
                    } else {
                        showError("Error: ${e.code()}")
                    }
                } catch (jsonEx: Exception) {
                    Log.e(TAG, "Error parsing error response", jsonEx)
                    showError("Server error: ${e.code()}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Exception during login", e)
                handleError(e)
            } finally {
                Log.d(TAG, "Login process ended")
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
        Log.e(TAG, "Error occurred: $errorMessage", error)
        showError(errorMessage)
    }

    private fun showError(message: String) {
        // Use Snackbar for better visibility of error messages
        Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG).show()
    }

    private fun showLoading(show: Boolean) {
        binding.progressBar.visibility = if (show) View.VISIBLE else View.GONE
        binding.btnLogin.isEnabled = !show
    }

    private fun navigateToHome() {
        // Navigate to the home activity
        Log.d(TAG, "Navigating to HomeActivity")
        startActivity(Intent(this, HomeActivity::class.java))
        finish()
    }
}