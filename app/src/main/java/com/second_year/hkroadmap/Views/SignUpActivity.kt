package com.second_year.hkroadmap.Views

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
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

    companion object {
        private const val MIN_PASSWORD_LENGTH = 8
        private const val MAX_PASSWORD_LENGTH = 20
        private const val MIN_USERNAME_LENGTH = 3
        private const val MAX_USERNAME_LENGTH = 50
    }

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
            finish()
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

            !isValidEmail(email) -> {
                binding.etEmail.error = "Invalid email format"
                return false
            }

            !email.contains("@") -> {
                binding.etEmail.error = "Email must contain @"
                return false
            }

            username.isEmpty() -> {
                binding.etUsername.error = "Name is required"
                return false
            }

            username.length < MIN_USERNAME_LENGTH -> {
                binding.etUsername.error = "Name must be at least $MIN_USERNAME_LENGTH characters"
                return false
            }

            username.length > MAX_USERNAME_LENGTH -> {
                binding.etUsername.error = "Name must be less than $MAX_USERNAME_LENGTH characters"
                return false
            }

            !isValidUsername(username) -> {
                binding.etUsername.error = "Name can only contain letters, numbers, and spaces"
                return false
            }

            password.isEmpty() -> {
                binding.etPassword.error = "Password is required"
                return false
            }

            password.length < MIN_PASSWORD_LENGTH -> {
                binding.etPassword.error =
                    "Password must be at least $MIN_PASSWORD_LENGTH characters"
                return false
            }

            password.length > MAX_PASSWORD_LENGTH -> {
                binding.etPassword.error =
                    "Password must be less than $MAX_PASSWORD_LENGTH characters"
                return false
            }

            !isValidPassword(password) -> {
                binding.etPassword.error =
                    "Password must contain uppercase, lowercase, number, and special character"
                return false
            }

            confirmPassword.isEmpty() -> {
                binding.etConfirmPassword.error = "Please confirm your password"
                return false
            }

            password != confirmPassword -> {
                binding.etConfirmPassword.error = "Passwords do not match"
                return false
            }
        }
        return true
    }

    private fun isValidEmail(email: String): Boolean {
        val pattern = Patterns.EMAIL_ADDRESS
        return pattern.matcher(email).matches()
    }

    private fun isValidUsername(username: String): Boolean {
        val pattern = "^[a-zA-Z0-9 ]*$".toRegex()
        return pattern.matches(username)
    }

    private fun isValidPassword(password: String): Boolean {
        val pattern =
            "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=!])(?=\\S+$).{$MIN_PASSWORD_LENGTH,$MAX_PASSWORD_LENGTH}$".toRegex()
        return pattern.matches(password)
    }

    private fun performSignUp() {
        showLoading(true)

        val signUpRequest = StudentRegisterRequest(
            name = binding.etUsername.text.toString().trim(),
            email = binding.etEmail.text.toString().trim().toLowerCase(),
            password = binding.etPassword.text.toString().trim(),
            confirm_password = binding.etConfirmPassword.text.toString().trim()
        )

        lifecycleScope.launch {
            try {
                val response = RetrofitInstance.createApiService().studentRegister(signUpRequest)
                if (response.message.contains("success", ignoreCase = true)) {
                    showSuccess("Registration successful! Please login.")
                    navigateToLogin()
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

    private fun showSuccess(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
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

    private fun navigateToLogin() {
        Intent(this, LoginActivity::class.java).also { intent ->
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
            startActivity(intent)
            finish()
        }
    }
}