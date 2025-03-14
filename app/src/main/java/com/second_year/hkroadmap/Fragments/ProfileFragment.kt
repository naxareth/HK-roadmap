package com.second_year.hkroadmap.Fragments

import android.app.Activity
import android.content.ContentValues.TAG
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.gson.Gson
import com.second_year.hkroadmap.Api.Interfaces.RetrofitInstance
import com.second_year.hkroadmap.Api.Interfaces.TokenManager
import com.second_year.hkroadmap.R
import com.second_year.hkroadmap.ViewModel.ProfileViewModel
import com.second_year.hkroadmap.ViewModel.ViewModelFactory
import com.second_year.hkroadmap.data.models.Profile
import com.second_year.hkroadmap.data.models.ProfileUpdateRequest
import com.second_year.hkroadmap.databinding.FragmentProfileBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.io.FileOutputStream

class ProfileFragment : Fragment() {

    private val TAG = "ProfileFragment"

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

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
        Log.d(TAG, "onCreate called")
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        Log.d(TAG, "onCreateView called")
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.d(TAG, "onViewCreated called")

        try {
            Log.d(TAG, "Setting up ViewModel")
            if (!setupViewModel()) {
                // If setupViewModel returns false, stop here
                return
            }

            Log.d(TAG, "Setting up Observers")
            setupObservers()
            Log.d(TAG, "Setting up Views")
            setupViews()
            Log.d(TAG, "Loading Data")
            loadData()
        } catch (e: Exception) {
            Log.e(TAG, "Error in onViewCreated", e)
            showError("Failed to initialize profile: ${e.message}")
        }
    }

    private fun setupViewModel(): Boolean {
        try {
            // Use TokenManager instead of direct SharedPreferences
            token = TokenManager.getToken(requireContext())
            Log.d(TAG, "Token retrieved: ${token != null}")

            if (token == null) {
                Log.e(TAG, "No token found")
                showError("Not logged in")
                // Optionally redirect to login
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

    private fun setupObservers() {
        try {
            viewModel.profile.observe(viewLifecycleOwner) { profile ->
                Log.d(TAG, "Profile data received")
                profile?.let {
                    Log.d(TAG, "Updating UI with profile data")
                    updateProfileUI(it)
                }
            }

            viewModel.departments.observe(viewLifecycleOwner) { departments ->
                Log.d(TAG, "Departments data received: ${departments?.size} departments")
                departments?.let { setupDepartmentSpinner(it) }
            }

            viewModel.programs.observe(viewLifecycleOwner) { programs ->
                Log.d(TAG, "Programs data received: ${programs?.size} programs")
                programs?.let { setupProgramSpinner(it) }
            }

            viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
                Log.d(TAG, "Loading state changed: $isLoading")
                binding.progressBar.isVisible = isLoading
                binding.btnSave.isEnabled = !isLoading
            }

            viewModel.error.observe(viewLifecycleOwner) { message ->
                message?.let {
                    if (it.contains("successfully")) {
                        // Show success message
                        showSuccess(it)
                    } else {
                        // Show error message
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
            requireContext(),
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
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                // Not used but must be implemented
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                // Not used but must be implemented
            }

            override fun afterTextChanged(s: Editable?) {
                validateInput()
            }
        })

        binding.etContactNumber.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                // Not used but must be implemented
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                // Not used but must be implemented
            }

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
    }

    private fun validateInput(): Boolean {
        var isValid = true
        Log.d(TAG, "Validating input")

        // Validate student number only if it's not empty
        val studentNumber = binding.etStudentNumber.text.toString()
        Log.d(TAG, "Validating student number: '$studentNumber'")
        if (studentNumber.isNotEmpty() && !studentNumber.matches(Regex("\\d{2}-\\d{4}-\\d{6}"))) {
            binding.etStudentNumber.error = "Invalid student number format (XX-XXXX-XXXXXX)"
            isValid = false
            Log.d(TAG, "Student number validation failed: Invalid format")
        }

        // Validate contact number only if it's not empty
        val contactNumber = binding.etContactNumber.text.toString()
        Log.d(TAG, "Validating contact number: '$contactNumber'")
        if (contactNumber.isNotEmpty() && !contactNumber.matches(Regex("^(09)\\d{9}$"))) {
            binding.etContactNumber.error = "Invalid contact number format (09XXXXXXXXX)"
            isValid = false
            Log.d(TAG, "Contact number validation failed: Invalid format")
        }

        Log.d(TAG, "Input validation result: $isValid")
        binding.btnSave.isEnabled = true  // Always enable save button
        return isValid
    }

    private fun showSaveConfirmationDialog() {
        MaterialAlertDialogBuilder(requireContext())
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
            Log.d(TAG, "Setting student number: ${profile.studentNumber}")
            etStudentNumber.setText(profile.studentNumber)
            etScholarshipType.setText(profile.scholarshipType)
            Log.d(TAG, "Setting contact number: ${profile.contactNumber}")
            etContactNumber.setText(profile.contactNumber)

            // Set spinners if values exist
            profile.department?.let { department ->
                val position = (spinnerDepartment.adapter as? ArrayAdapter<String>)?.getPosition(department)
                if (position != -1) {
                    spinnerDepartment.setText(department, false)
                    tilDepartmentOthers.isVisible = department == "Others"
                }
            }

            profile.departmentOthers?.let { others ->
                etDepartmentOthers.setText(others)
            }

            profile.collegeProgram?.let { program ->
                val position = (spinnerProgram.adapter as? ArrayAdapter<String>)?.getPosition(program)
                if (position != -1) {
                    spinnerProgram.setText(program, false)
                }
            }

            // Convert year level to string format before setting
            profile.yearLevel?.let { yearLevel ->
                val yearLevelText = when (yearLevel) {
                    1 -> "1"
                    2 -> "2"
                    3 -> "3"
                    4 -> "4"
                    5 -> "5"
                    else -> null
                }
                yearLevelText?.let {
                    spinnerYearLevel.setText(it, false)
                }
            }

            // Load profile picture
            profile.profilePictureUrl?.let { fileName ->
                val fullImageUrl = RetrofitInstance.getProfilePictureUrl(fileName)
                loadProfileImage(fullImageUrl)
            } ?: loadDefaultProfileImage()
        }
    }

    private fun loadProfileImage(imageUrl: String) {
        Glide.with(requireContext())
            .load(imageUrl)
            .placeholder(R.drawable.default_profile)
            .error(R.drawable.default_profile)
            .into(binding.ivProfilePicture)
    }

    private fun loadDefaultProfileImage() {
        Glide.with(requireContext())
            .load(R.drawable.default_profile)
            .into(binding.ivProfilePicture)
    }

    private fun setupDepartmentSpinner(departments: Map<String, String>) {
        val adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_dropdown_item_1line,
            departments.values.toList()
        )
        binding.spinnerDepartment.setAdapter(adapter)
    }

    private fun setupProgramSpinner(programs: List<String>) {
        val adapter = ArrayAdapter(
            requireContext(),
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
            scholarshipType = binding.etScholarshipType.text.toString(),
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

    private fun openImagePicker() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        pickImage.launch(intent)
    }

    private fun updateProfilePicture(uri: Uri) {
        Glide.with(requireContext())
            .load(uri)
            .placeholder(R.drawable.default_profile)
            .error(R.drawable.default_profile)
            .into(binding.ivProfilePicture)
    }

    private fun updateProfileWithImage(uri: Uri, profileUpdate: ProfileUpdateRequest) {
        Log.d(TAG, "Starting image upload process")
        binding.progressBar.isVisible = true
        binding.btnSave.isEnabled = false

        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            try {
                Log.d(TAG, "Converting image to file")
                // Convert Uri to File
                val inputStream = requireContext().contentResolver.openInputStream(uri)
                val file = File(requireContext().cacheDir, "profile_picture.jpg")
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
        snackbar.setBackgroundTint(ContextCompat.getColor(requireContext(), R.color.success_color))
        snackbar.show()
    }


    private fun showError(message: String) {
        val snackbar = Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG)
        snackbar.setAction("OK") { snackbar.dismiss() }
        snackbar.show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        fun newInstance() = ProfileFragment()
    }
}