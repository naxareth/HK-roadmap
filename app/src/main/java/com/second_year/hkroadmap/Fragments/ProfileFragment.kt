package com.second_year.hkroadmap.Fragments

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.core.widget.NestedScrollView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.google.android.material.snackbar.Snackbar
import com.second_year.hkroadmap.Adapters.ProfileRequirementsAdapter
import com.second_year.hkroadmap.Api.Interfaces.RetrofitInstance
import com.second_year.hkroadmap.Api.Interfaces.TokenManager
import com.second_year.hkroadmap.R
import com.second_year.hkroadmap.Utils.NetworkUtils
import com.second_year.hkroadmap.ViewModel.ProfileRequirementsUiState
import com.second_year.hkroadmap.ViewModel.ProfileRequirementsViewModel
import com.second_year.hkroadmap.ViewModel.ViewModelFactory
import com.second_year.hkroadmap.data.models.ProfileData
import com.second_year.hkroadmap.data.models.ProfileRequirementsData
import com.second_year.hkroadmap.databinding.FragmentProfileBinding

class ProfileFragment : Fragment() {
    private val TAG = "ProfileFragment"
    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: ProfileRequirementsViewModel
    private var token: String? = null
    private lateinit var requirementsAdapter: ProfileRequirementsAdapter
    private var isShareDialogShown = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Removed setHasOptionsMenu(true) to disable the options menu
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == SHARE_REQUEST_CODE) {
            // Share dialog has been dismissed
            isShareDialogShown = false
            viewModel.resetLoadingState()
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.d(TAG, "onViewCreated called")

        try {
            setupRecyclerView()
            setupScrollIndicator()
            if (!setupViewModel()) {
                return
            }
            setupObservers()
            setupExportButton()
            loadData()
        } catch (e: Exception) {
            Log.e(TAG, "Error in onViewCreated", e)
            showError("Failed to initialize profile: ${e.message}")
        }
    }

    private fun setupScrollIndicator() {
        binding.nestedScrollView.setOnScrollChangeListener(NestedScrollView.OnScrollChangeListener { v, _, scrollY, _, _ ->
            val child = v.getChildAt(0)
            if (child != null) {
                val childHeight = child.height
                val scrollViewHeight = v.height
                val isScrollable = childHeight > scrollViewHeight
                val hasReachedBottom = scrollY >= childHeight - scrollViewHeight

                binding.scrollIndicator.apply {
                    if (isScrollable && !hasReachedBottom) {
                        if (visibility != View.VISIBLE) {
                            show()
                        }
                    } else {
                        if (visibility == View.VISIBLE) {
                            hide()
                        }
                    }
                }
            }
        })

        // Scroll to bottom when indicator is clicked
        binding.scrollIndicator.setOnClickListener {
            binding.nestedScrollView.post {
                binding.nestedScrollView.fullScroll(View.FOCUS_DOWN)
            }
        }
    }

    // Removed onCreateOptionsMenu and onOptionsItemSelected methods

    private fun setupRecyclerView() {
        requirementsAdapter = ProfileRequirementsAdapter()
        binding.rvRequirements.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = requirementsAdapter
        }
    }

    private fun setupViewModel(): Boolean {
        try {
            token = TokenManager.getToken(requireContext())
            Log.d(TAG, "Token retrieved: ${token != null}")

            if (token == null) {
                Log.e(TAG, "No token found")
                showError("Not logged in")
                return false
            }

            val factory = ViewModelFactory(apiService = RetrofitInstance.createApiService())
            viewModel = ViewModelProvider(this, factory)[ProfileRequirementsViewModel::class.java]
            Log.d(TAG, "ViewModel setup complete")
            return true
        } catch (e: Exception) {
            Log.e(TAG, "Error setting up ViewModel", e)
            throw e
        }
    }

    private fun setupObservers() {
        try {
            viewModel.uiState.observe(viewLifecycleOwner) { state ->
                when (state) {
                    is ProfileRequirementsUiState.Loading -> {
                        showLoading(true)
                    }
                    is ProfileRequirementsUiState.Success -> {
                        showLoading(false)
                        updateUI(state.data)
                    }
                    is ProfileRequirementsUiState.Error -> {
                        showLoading(false)
                        showError(state.message)
                    }
                }
            }

            viewModel.exportFileUri.observe(viewLifecycleOwner) { uri ->
                uri?.let {
                    shareExportedFile(it)
                }
            }

            // Observe loading state
            viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
                if (!isShareDialogShown) {
                    showLoading(isLoading)
                }
            }

            Log.d(TAG, "Observers setup complete")
        } catch (e: Exception) {
            Log.e(TAG, "Error setting up observers", e)
            throw e
        }
    }

    private fun setupExportButton() {
        binding.btnExport.setOnClickListener {
            exportProfileData()
        }
    }

    private fun loadData() {
        if (!NetworkUtils.isNetworkAvailable(requireContext())) {
            showError("No internet connection. Please check your network and try again.")
            return
        }

        token?.let { token ->
            viewModel.fetchProfileRequirements(token, requireContext())
        } ?: run {
            showError("Not logged in")
        }
    }

    private fun updateUI(data: ProfileRequirementsData) {
        Log.d(TAG, "Profile requirements data received")
        updateProfileUI(data.profile)
        updateRequirementsUI(data)
    }

    private fun updateProfileUI(profile: ProfileData) {
        Log.d(TAG, "Profile data: $profile")
        with(binding) {
            // Basic Info
            tvName.text = profile.name ?: "Not set"
            tvEmail.text = profile.email ?: "Not set"
            tvStudentNumber.text = profile.studentNumber ?: "Not set"
            tvDepartment.text = profile.department ?: "Not set"
            tvProgram.text = profile.collegeProgram ?: "Not set"
            tvYearLevel.text = profile.yearLevel?.toString() ?: "Not set"
            tvScholarshipType.text = profile.scholarshipType ?: "Not set"
            tvContactNumber.text = profile.contactNumber ?: "Not set"

            // Load profile picture
            profile.profilePictureUrl?.let { fileName ->
                val fullImageUrl = RetrofitInstance.getProfilePictureUrl(fileName)
                loadProfileImage(fullImageUrl)
            } ?: loadDefaultProfileImage()
        }
    }

    private fun updateRequirementsUI(data: ProfileRequirementsData) {
        val requirementGroups = viewModel.getRequirementGroups(data)
        requirementsAdapter.submitList(requirementGroups)

        // Show or hide the requirements section based on whether there are requirements
        binding.requirementsSection.isVisible = requirementGroups.isNotEmpty()
        binding.tvNoRequirements.isVisible = requirementGroups.isEmpty() && binding.requirementsSection.isVisible
    }

    private fun loadProfileImage(imageUrl: String) {
        Glide.with(requireContext())
            .load(imageUrl)
            .placeholder(R.drawable.default_profile)
            .error(R.drawable.default_profile)
            .circleCrop()
            .into(binding.ivProfilePicture)
    }

    private fun loadDefaultProfileImage() {
        Glide.with(requireContext())
            .load(R.drawable.default_profile)
            .circleCrop()
            .into(binding.ivProfilePicture)
    }

    private fun exportProfileData() {
        if (!NetworkUtils.isNetworkAvailable(requireContext())) {
            showError("No internet connection. Please check your network and try again.")
            return
        }

        val currentState = viewModel.uiState.value
        if (currentState !is ProfileRequirementsUiState.Success) {
            showError("No data available to export")
            return
        }

        showLoading(true)
        viewModel.exportProfileRequirements(requireContext())
    }

    private fun shareExportedFile(uri: Uri) {
        // Set flag to indicate share dialog is shown
        isShareDialogShown = true

        // Hide loading indicator
        showLoading(false)

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"  // Changed from text/csv to application/pdf
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        // Create a chooser intent
        val chooserIntent = Intent.createChooser(intent, "Share Profile Requirements")

        // Start activity for result to detect when dialog is dismissed
        startActivityForResult(chooserIntent, SHARE_REQUEST_CODE)
    }

    private fun showLoading(isLoading: Boolean) {
        binding.progressBar.isVisible = isLoading
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
        private const val SHARE_REQUEST_CODE = 123
    }
}