package com.second_year.hkroadmap.ViewModel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.second_year.hkroadmap.Api.Interfaces.ApiService
import com.second_year.hkroadmap.data.models.Profile
import com.second_year.hkroadmap.data.models.ProfileUpdateRequest
import kotlinx.coroutines.launch
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response

class ProfileViewModel(
    private val apiService: ApiService
) : ViewModel() {

    private val _profile = MutableLiveData<Profile>()
    val profile: LiveData<Profile> = _profile

    private val _departments = MutableLiveData<Map<String, String>>()
    val departments: LiveData<Map<String, String>> = _departments

    private val _programs = MutableLiveData<List<String>>()
    val programs: LiveData<List<String>> = _programs

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _error = MutableLiveData<String>()
    val error: LiveData<String> = _error

    fun fetchProfile(token: String) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                val response = apiService.getProfile("Bearer $token")
                if (response.isSuccessful) {
                    _profile.value = response.body()
                } else {
                    _error.value = "Failed to load profile"
                }
            } catch (e: Exception) {
                _error.value = e.message ?: "Unknown error occurred"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun fetchDepartments(token: String) {
        viewModelScope.launch {
            try {
                val response = apiService.getDepartments("Bearer $token")
                if (response.isSuccessful) {
                    _departments.value = response.body()?.departments
                } else {
                    _error.value = "Failed to load departments"
                }
            } catch (e: Exception) {
                _error.value = e.message ?: "Unknown error occurred"
            }
        }
    }

    fun fetchPrograms(token: String) {
        viewModelScope.launch {
            try {
                val response = apiService.getPrograms("Bearer $token")
                if (response.isSuccessful) {
                    _programs.value = response.body()?.programs
                } else {
                    _error.value = "Failed to load programs"
                }
            } catch (e: Exception) {
                _error.value = e.message ?: "Unknown error occurred"
            }
        }
    }

    fun updateProfile(token: String, profileUpdate: ProfileUpdateRequest) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                val response = apiService.updateProfile("Bearer $token", profileUpdate)
                if (response.isSuccessful) {
                    response.body()?.let { updateResponse ->
                        _profile.value = updateResponse.profile
                        _error.value = updateResponse.message  // This is a success message
                    }
                } else {
                    _error.value = "Failed to update profile"
                }
            } catch (e: Exception) {
                _error.value = e.message ?: "Unknown error occurred"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun updateProfileWithPicture(
        token: String,
        profilePicture: MultipartBody.Part,
        profile: RequestBody
    ) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                val response = apiService.updateProfileWithPicture(
                    "Bearer $token",
                    profilePicture,
                    profile
                )
                handleProfileUpdateResponse(response)
            } catch (e: Exception) {
                _error.value = e.message ?: "Unknown error occurred"
            } finally {
                _isLoading.value = false
            }
        }
    }

    private fun handleProfileUpdateResponse(response: Response<Profile>) {
        if (response.isSuccessful) {
            _profile.value = response.body()
            _error.value = "Profile updated successfully"
        } else {
            _error.value = "Failed to update profile"
        }
    }

    // Helper function to get department code from name
    fun getDepartmentCode(departmentName: String): String {
        return _departments.value?.entries?.find { it.value == departmentName }?.key ?: "Others"
    }

    // Helper function to get department name from code
    fun getDepartmentName(departmentCode: String): String? {
        return _departments.value?.get(departmentCode)
    }
}