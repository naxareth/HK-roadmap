package com.second_year.hkroadmap.Views

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.widget.ArrayAdapter
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.gson.Gson
import com.second_year.hkroadmap.Api.Interfaces.RetrofitInstance
import com.second_year.hkroadmap.Api.Interfaces.TokenManager
import com.second_year.hkroadmap.Api.Models.LoginRequest
import com.second_year.hkroadmap.Api.Models.PasswordChangeRequest
import com.second_year.hkroadmap.data.models.ProfileUpdateRequest
import com.second_year.hkroadmap.R
import com.second_year.hkroadmap.ViewModel.ProfileViewModel
import com.second_year.hkroadmap.ViewModel.ViewModelFactory
import com.second_year.hkroadmap.data.models.Profile
import com.second_year.hkroadmap.databinding.ActivityProfileBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.io.FileOutputStream

class ProfileActivity : AppCompatActivity() {
    private val TAG = "ProfileActivity"
    private lateinit var binding: ActivityProfileBinding
    private lateinit var viewModel: ProfileViewModel
    private var token: String? = null
    private var selectedImageUri: Uri? = null

    private val pickImage = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                selectedImageUri = uri
                updateProfilePicture(uri)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)
        Log.d(TAG, "onCreate called")

        try {
            if (!setupViewModel()) {
                return
            }
            setupToolbar()
            setupObservers()
            setupViews()
            loadData()
        } catch (e: Exception) {
            Log.e(TAG, "Error in onCreate", e)
            showError("Failed to initialize profile: ${e.message}")
        }
    }

    private fun setupViewModel(): Boolean {
        try {
            token = TokenManager.getToken(this)
            Log.d(TAG, "Token retrieved: ${token != null}")

            if (token == null) {
                Log.e(TAG, "No token found")
                showError("Not logged in")
                return false
            }

            val factory = ViewModelFactory(apiService = RetrofitInstance.createApiService())
            viewModel = ViewModelProvider(this, factory)[ProfileViewModel::class.java]
            Log.d(TAG, "ViewModel setup complete")
            return true
        } catch (e: Exception) {
            Log.e(TAG, "Error setting up ViewModel", e)
            throw e
        }
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowHomeEnabled(true)
            title = "Profile"
        }
    }

    private fun setupObservers() {
        try {
            viewModel.profile.observe(this) { profile ->
                Log.d(TAG, "Profile data received")
                profile?.let {
                    Log.d(TAG, "Updating UI with profile data")
                    updateProfileUI(it)
                }
            }

            viewModel.departments.observe(this) { departments ->
                Log.d(TAG, "Departments data received: ${departments?.size} departments")
                departments?.let { setupDepartmentSpinner(it) }
            }

            viewModel.programs.observe(this) { programs ->
                Log.d(TAG, "Programs data received: ${programs?.size} programs")
                programs?.let { setupProgramSpinner(it) }
            }

            viewModel.isLoading.observe(this) { isLoading ->
                Log.d(TAG, "Loading state changed: $isLoading")
                binding.progressBar.isVisible = isLoading
                binding.btnSave.isEnabled = !isLoading
            }

            viewModel.error.observe(this) { message ->
                message?.let {
                    if (it.contains("successfully")) {
                        showSuccess(it)
                    } else {
                        showError(it)
                    }
                }
            }
            Log.d(TAG, "Observers setup complete")
        } catch (e: Exception) {
            Log.e(TAG, "Error setting up observers", e)
            throw e
        }
    }

    private fun setupViews() {
        // Setup year level spinner
        val yearLevels = arrayOf("1", "2", "3", "4", "5")
        val yearAdapter = ArrayAdapter(
            this,
            android.R.layout.simple_dropdown_item_1line,
            yearLevels
        )
        binding.spinnerYearLevel.setAdapter(yearAdapter)

        // Setup department spinner listener
        binding.spinnerDepartment.setOnItemClickListener { _, _, position, _ ->
            binding.tilDepartmentOthers.isVisible =
                position == binding.spinnerDepartment.adapter.count - 1
        }

        // Setup input validation
        binding.etStudentNumber.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                validateInput()
            }
        })

        // Setup save button
        binding.btnSave.setOnClickListener {
            Log.d(TAG, "Save button clicked")
            if (validateInput()) {
                Log.d(TAG, "Input validation passed")
                showSaveConfirmationDialog()
            } else {
                Log.d(TAG, "Input validation failed")
            }
        }

        // Setup profile picture edit button
        binding.btnEditProfilePicture.setOnClickListener {
            openImagePicker()
        }
        setupPasswordChange()
        setupLogout()
    }


    private fun validateInput(): Boolean {
        var isValid = true
        Log.d(TAG, "Validating input")

        // Validate student number
        val studentNumber = binding.etStudentNumber.text.toString()
        if (studentNumber.isNotEmpty() && !studentNumber.matches(Regex("\\d{2}-\\d{4}-\\d{6}"))) {
            binding.etStudentNumber.error = "Invalid student number format (XX-XXXX-XXXXXX)"
            isValid = false
        }

        Log.d(TAG, "Input validation result: $isValid")
        binding.btnSave.isEnabled = true
        return isValid
    }

    private fun showSaveConfirmationDialog() {
        MaterialAlertDialogBuilder(this)
            .setTitle("Save Changes")
            .setMessage("Are you sure you want to save these changes?")
            .setPositiveButton("Save") { _, _ ->
                saveProfile()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun loadData() {
        token?.let { token ->
            viewModel.fetchProfile(token)
            viewModel.fetchDepartments(token)
            viewModel.fetchPrograms(token)
        } ?: run {
            showError("Not logged in")
        }
    }

    private fun updateProfileUI(profile: Profile) {
        Log.d(TAG, "Profile data: $profile")
        with(binding) {
            tvName.text = profile.name
            tvEmail.text = profile.email
            etStudentNumber.setText(profile.studentNumber)

            // Set department if exists
            profile.department?.let { department ->
                spinnerDepartment.setText(department, false)
                tilDepartmentOthers.isVisible = department == "Others"
            }

            profile.departmentOthers?.let { others ->
                etDepartmentOthers.setText(others)
            }

            profile.collegeProgram?.let { program ->
                spinnerProgram.setText(program, false)
            }

            // Set year level
            profile.yearLevel?.toString()?.let { yearLevel ->
                spinnerYearLevel.setText(yearLevel, false)
            }

            // Set scholarship type and contact number
            etScholarshipType.setText(profile.scholarshipType)
            etContactNumber.setText(profile.contactNumber)

            // Load profile picture
            profile.profilePictureUrl?.let { fileName ->
                val fullImageUrl = RetrofitInstance.getProfilePictureUrl(fileName)
                loadProfileImage(fullImageUrl)
            } ?: loadDefaultProfileImage()
        }
    }

    private fun loadProfileImage(imageUrl: String) {
        Glide.with(this)
            .load(imageUrl)
            .placeholder(R.drawable.default_profile)
            .error(R.drawable.default_profile)
            .circleCrop()
            .into(binding.ivProfilePicture)
    }

    private fun loadDefaultProfileImage() {
        Glide.with(this)
            .load(R.drawable.default_profile)
            .circleCrop()
            .into(binding.ivProfilePicture)
    }

    private fun setupDepartmentSpinner(departments: Map<String, String>) {
        val adapter = ArrayAdapter(
            this,
            android.R.layout.simple_dropdown_item_1line,
            departments.values.toList()
        )
        binding.spinnerDepartment.setAdapter(adapter)
    }

    private fun setupProgramSpinner(programs: List<String>) {
        val adapter = ArrayAdapter(
            this,
            android.R.layout.simple_dropdown_item_1line,
            programs
        )
        binding.spinnerProgram.setAdapter(adapter)
    }

    private fun saveProfile() {
        Log.d(TAG, "saveProfile called")
        val profileUpdate = ProfileUpdateRequest(
            name = null,  // Managed by backend
            email = null, // Managed by backend
            department = binding.spinnerDepartment.text.toString(),
            departmentOthers = if (binding.tilDepartmentOthers.isVisible)
                binding.etDepartmentOthers.text.toString() else null,
            studentNumber = binding.etStudentNumber.text.toString().ifEmpty { null },
            collegeProgram = binding.spinnerProgram.text.toString(),
            yearLevel = binding.spinnerYearLevel.text.toString(),
            scholarshipType = binding.etScholarshipType.text.toString().ifEmpty { null },
            contactNumber = binding.etContactNumber.text.toString().ifEmpty { null }
        )
        Log.d(TAG, "Profile update data: $profileUpdate")

        selectedImageUri?.let { uri ->
            Log.d(TAG, "Updating profile with image")
            updateProfileWithImage(uri, profileUpdate)
        } ?: run {
            Log.d(TAG, "Updating profile without image")
            token?.let {
                viewModel.updateProfile(it, profileUpdate)
            } ?: run {
                Log.e(TAG, "Cannot update profile: Token is null")
                showError("Not logged in")
            }
        }
    }


    private fun setupPasswordChange() {
        binding.btnChangePassword.setOnClickListener {
            val currentPassword = binding.etCurrentPassword.text.toString()
            val newPassword = binding.etNewPassword.text.toString()
            val confirmPassword = binding.etConfirmNewPassword.text.toString()

            // Validate inputs
            when {
                currentPassword.isEmpty() -> {
                    binding.tilCurrentPassword.error = "Please enter current password"
                    return@setOnClickListener
                }
                newPassword.isEmpty() -> {
                    binding.tilNewPassword.error = "Please enter new password"
                    return@setOnClickListener
                }
                confirmPassword.isEmpty() -> {
                    binding.tilConfirmNewPassword.error = "Please confirm new password"
                    return@setOnClickListener
                }
                newPassword != confirmPassword -> {
                    binding.tilConfirmNewPassword.error = "Passwords do not match"
                    return@setOnClickListener
                }
                newPassword.length < 8 -> {
                    binding.tilNewPassword.error = "Password must be at least 8 characters"
                    return@setOnClickListener
                }
            }

            // Show confirmation dialog
            MaterialAlertDialogBuilder(this)
                .setTitle("Change Password")
                .setMessage("Are you sure you want to change your password?")
                .setPositiveButton("Yes") { _, _ ->
                    verifyCurrentPasswordAndChange(currentPassword, newPassword)
                }
                .setNegativeButton("No", null)
                .show()
        }
    }

    private fun verifyCurrentPasswordAndChange(currentPassword: String, newPassword: String) {
        lifecycleScope.launch {
            try {
                binding.progressBar.isVisible = true

                // First verify current password by attempting to login
                val loginRequest = LoginRequest(
                    email = binding.tvEmail.text.toString(),
                    password = currentPassword
                )

                val loginResponse = RetrofitInstance.createApiService().studentLogin(loginRequest)

                if (loginResponse.token != null) {
                    // Current password is correct, proceed with password change
                    val passwordChangeRequest = PasswordChangeRequest(
                        email = binding.tvEmail.text.toString(),
                        new_password = newPassword
                    )

                    val response = RetrofitInstance.createApiService().changePassword(passwordChangeRequest)

                    // Clear password fields
                    binding.etCurrentPassword.text?.clear()
                    binding.etNewPassword.text?.clear()
                    binding.etConfirmNewPassword.text?.clear()

                    showSuccess(response.message)

                    // Important: After successful password change, log out the user
                    MaterialAlertDialogBuilder(this@ProfileActivity)
                        .setTitle("Password Changed")
                        .setMessage("Your password has been changed successfully. Please log in again with your new password.")
                        .setPositiveButton("OK") { _, _ ->
                            // Clear token and redirect to login
                            TokenManager.clearToken(this@ProfileActivity)
                            val intent = Intent(this@ProfileActivity, LoginActivity::class.java)
                            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                            startActivity(intent)
                            finish()
                        }
                        .setCancelable(false)
                        .show()
                } else {
                    showError("Current password is incorrect")
                }
            } catch (e: Exception) {
                showError("Failed to change password: ${e.message}")
            } finally {
                binding.progressBar.isVisible = false
            }
        }
    }

    private fun setupLogout() {
        binding.btnLogout.setOnClickListener {
            MaterialAlertDialogBuilder(this)
                .setTitle("Logout")
                .setMessage("Are you sure you want to logout?")
                .setPositiveButton("Yes") { _, _ ->
                    logout()
                }
                .setNegativeButton("No", null)
                .show()
        }
    }

    private fun logout() {
        lifecycleScope.launch {
            try {
                binding.progressBar.isVisible = true
                token?.let { token ->
                    val response = RetrofitInstance.createApiService().studentLogout("Bearer $token")
                    if (response.message.isNotEmpty()) {
                        // Clear token
                        TokenManager.clearToken(this@ProfileActivity)

                        // Navigate to login screen
                        val intent = Intent(this@ProfileActivity, LoginActivity::class.java)
                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        startActivity(intent)
                        finish()
                    }
                }
            } catch (e: Exception) {
                showError("Failed to logout: ${e.message}")
            } finally {
                binding.progressBar.isVisible = false
            }
        }
    }

    private fun openImagePicker() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        pickImage.launch(intent)
    }

    private fun updateProfilePicture(uri: Uri) {
        Glide.with(this)
            .load(uri)
            .placeholder(R.drawable.default_profile)
            .error(R.drawable.default_profile)
            .circleCrop()
            .into(binding.ivProfilePicture)
    }

    private fun updateProfileWithImage(uri: Uri, profileUpdate: ProfileUpdateRequest) {
        Log.d(TAG, "Starting image upload process")
        binding.progressBar.isVisible = true
        binding.btnSave.isEnabled = false

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                Log.d(TAG, "Converting image to file")
                // Convert Uri to File
                val inputStream = contentResolver.openInputStream(uri)
                val file = File(cacheDir, "profile_picture.jpg")
                FileOutputStream(file).use { outputStream ->
                    inputStream?.copyTo(outputStream)
                }
                Log.d(TAG, "Image file created: ${file.exists()}")

                // Create MultipartBody.Part for the image
                val requestFile = file.asRequestBody("image/*".toMediaTypeOrNull())
                val imagePart = MultipartBody.Part.createFormData(
                    "profile_picture",
                    file.name,
                    requestFile
                )

                // Convert ProfileUpdateRequest to JSON string then to RequestBody
                val gson = Gson()
                val profileJson = gson.toJson(profileUpdate)
                val profileRequestBody = profileJson.toRequestBody("application/json".toMediaTypeOrNull())
                Log.d(TAG, "Profile update JSON: $profileJson")

                // Send to backend
                withContext(Dispatchers.Main) {
                    token?.let { token ->
                        Log.d(TAG, "Sending profile update request with image")
                        viewModel.updateProfileWithPicture(
                            token,
                            imagePart,
                            profileRequestBody
                        )
                    } ?: run {
                        Log.e(TAG, "Cannot update profile: Token is null")
                        showError("Not logged in")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error processing image", e)
                withContext(Dispatchers.Main) {
                    showError("Failed to process image: ${e.message}")
                }
            } finally {
                withContext(Dispatchers.Main) {
                    binding.progressBar.isVisible = false
                    binding.btnSave.isEnabled = true
                }
            }
        }
    }

    private fun showSuccess(message: String) {
        val snackbar = Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG)
        snackbar.setBackgroundTint(ContextCompat.getColor(this, R.color.success_color))
        snackbar.show()
    }

    private fun showError(message: String) {
        val snackbar = Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG)
        snackbar.setAction("OK") { snackbar.dismiss() }
        snackbar.show()
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}