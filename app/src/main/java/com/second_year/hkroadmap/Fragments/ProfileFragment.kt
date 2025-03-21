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
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.bumptech.glide.Glide
import com.google.android.material.snackbar.Snackbar
import com.second_year.hkroadmap.Api.Interfaces.RetrofitInstance
import com.second_year.hkroadmap.Api.Interfaces.TokenManager
import com.second_year.hkroadmap.R
import com.second_year.hkroadmap.ViewModel.ProfileViewModel
import com.second_year.hkroadmap.ViewModel.ViewModelFactory
import com.second_year.hkroadmap.data.models.Profile
import com.second_year.hkroadmap.databinding.FragmentProfileBinding


class ProfileFragment : Fragment() {
    private val TAG = "ProfileFragment"
    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: ProfileViewModel
    private var token: String? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.d(TAG, "onViewCreated called")

        try {
            if (!setupViewModel()) {
                return
            }
            setupObservers()
            loadData()
        } catch (e: Exception) {
            Log.e(TAG, "Error in onViewCreated", e)
            showError("Failed to initialize profile: ${e.message}")
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

            viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
                Log.d(TAG, "Loading state changed: $isLoading")
                binding.progressBar.isVisible = isLoading
            }

            viewModel.error.observe(viewLifecycleOwner) { message ->
                message?.let {
                    showError(it)
                }
            }
            Log.d(TAG, "Observers setup complete")
        } catch (e: Exception) {
            Log.e(TAG, "Error setting up observers", e)
            throw e
        }
    }

    private fun loadData() {
        token?.let { token ->
            viewModel.fetchProfile(token)
        } ?: run {
            showError("Not logged in")
        }
    }

    private fun updateProfileUI(profile: Profile) {
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