package com.second_year.hkroadmap.Views

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.gson.Gson
import com.second_year.hkroadmap.Api.Models.OtpVerificationRequest
import com.second_year.hkroadmap.Api.Interfaces.RetrofitInstance
import com.second_year.hkroadmap.databinding.OtpVerificationBinding
import kotlinx.coroutines.launch
import android.util.Log

class OtpVerificationActivity : AppCompatActivity() {
    private lateinit var binding: OtpVerificationBinding
    private var currentEmail: String = ""
    private lateinit var otpEditTexts: Array<EditText>
    private val TAG = "OTPVerification"
    private val gson = Gson()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = OtpVerificationBinding.inflate(layoutInflater)
        setContentView(binding.root)

        currentEmail = intent.getStringExtra("email") ?: ""

        Log.d(TAG, "Activity created with email: $currentEmail")
        if (currentEmail.isEmpty()) {
            Log.e(TAG, "No email provided in intent")
            showToast("Error: No email found")
            setResult(RESULT_CANCELED)
            finish()
            return
        }

        setupOtpInputs()
        setupClickListeners()
        updateDescription()
    }

    private fun setupClickListeners() {
        binding.btnVerifyOtp.setOnClickListener {
            Log.d(TAG, "Verify OTP button clicked")
            handleOtpVerification()
        }
    }

    private fun updateDescription() {
        binding.tvOtpDescription.text = "We have sent a 6-digit code to $currentEmail"
    }

    private fun setupOtpInputs() {
        otpEditTexts = arrayOf(
            binding.etOtp1,
            binding.etOtp2,
            binding.etOtp3,
            binding.etOtp4,
            binding.etOtp5,
            binding.etOtp6
        )

        otpEditTexts.forEachIndexed { index, editText ->
            editText.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                override fun afterTextChanged(s: Editable?) {
                    try {
                        if (s?.length == 1 && index < otpEditTexts.size - 1) {
                            otpEditTexts[index + 1].requestFocus()
                        }
                        if (isAllFieldsFilled()) {
                            Log.d(TAG, "All OTP fields filled, triggering verification")
                            handleOtpVerification()
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error in text watcher: ${e.message}")
                    }
                }
            })

            editText.setOnKeyListener { _, keyCode, _ ->
                try {
                    if (keyCode == android.view.KeyEvent.KEYCODE_DEL && editText.text.isEmpty() && index > 0) {
                        otpEditTexts[index - 1].apply {
                            requestFocus()
                            text?.clear()
                        }
                        true
                    } else false
                } catch (e: Exception) {
                    Log.e(TAG, "Error in key listener: ${e.message}")
                    false
                }
            }
        }
    }

    private fun isAllFieldsFilled(): Boolean {
        return otpEditTexts.all { it.text.length == 1 }
    }

    private fun handleOtpVerification() {
        val enteredOtp = getOtpFromInputs().trim()
        val email = currentEmail.trim().lowercase()

        Log.d(TAG, "Starting OTP verification process")
        Log.d(TAG, "Sanitized inputs:")
        Log.d(TAG, "Email: $email")
        Log.d(TAG, "Entered OTP: $enteredOtp")

        if (!validateOtp(enteredOtp)) {
            Log.w(TAG, "OTP validation failed")
            return
        }

        showProgress()
        Log.d(TAG, "Starting API call")

        lifecycleScope.launch {
            try {
                val request = OtpVerificationRequest(email, enteredOtp)
                Log.d(TAG, "Sending request: ${gson.toJson(request)}")

                val response = RetrofitInstance.createApiService()
                    .studentVerifyOtp(request)

                Log.d(TAG, "Received raw response: ${gson.toJson(response)}")
                Log.d(TAG, "Response message: ${response.message}")

                hideProgress()

                if (response.message.contains("verified", ignoreCase = true)) {
                    Log.d(TAG, "OTP verification successful")
                    showToast("OTP verified successfully")
                    navigateToPasswordChange(email)
                } else {
                    Log.w(TAG, "OTP verification failed with message: ${response.message}")
                    showToast(response.message)
                    clearOtpInputs()
                }
            } catch (e: Exception) {
                Log.e(TAG, "API call failed", e)
                Log.e(TAG, "Stack trace: ${e.stackTrace.joinToString("\n")}")
                hideProgress()
                showToast("Failed to verify OTP: ${e.message}")
                clearOtpInputs()
            }
        }
    }

    private fun navigateToPasswordChange(email: String) {
        try {
            Log.d(TAG, "Navigating to password change screen")
            val intent = Intent(this, ForgotPasswordActivity::class.java).apply {
                putExtra("email", email)
            }
            startActivity(intent)
            finish()
        } catch (e: Exception) {
            Log.e(TAG, "Navigation Error: ${e.message}")
            showToast("Error launching password change screen")
        }
    }

    private fun getOtpFromInputs(): String {
        val otp = otpEditTexts.joinToString("") { it.text.toString() }
        Log.d(TAG, "Collected OTP from inputs: $otp")
        return otp
    }

    private fun validateOtp(otp: String): Boolean {
        Log.d(TAG, "Validating OTP: $otp")
        Log.d(TAG, "OTP length: ${otp.length}")
        Log.d(TAG, "All digits: ${otp.all { it.isDigit() }}")

        if (otp.length != 6) {
            showToast("Please enter all 6 digits")
            return false
        }
        if (!otp.all { it.isDigit() }) {
            showToast("OTP must contain only numbers")
            return false
        }
        return true
    }

    private fun clearOtpInputs() {
        otpEditTexts.forEach { it.text.clear() }
        otpEditTexts[0].requestFocus()
    }

    private fun showProgress() {
        binding.progressBar.visibility = View.VISIBLE
        binding.btnVerifyOtp.isEnabled = false
    }

    private fun hideProgress() {
        binding.progressBar.visibility = View.GONE
        binding.btnVerifyOtp.isEnabled = true
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}