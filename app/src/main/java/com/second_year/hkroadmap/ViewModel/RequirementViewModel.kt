package com.second_year.hkroadmap.ViewModel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.second_year.hkroadmap.Api.Models.RequirementItem
import com.second_year.hkroadmap.Api.Repository.RequirementRepository
import kotlinx.coroutines.launch

class RequirementViewModel(private val requirementRepository: RequirementRepository) : ViewModel() {

    private val _requirements = MutableLiveData<List<RequirementItem>>()
    val requirements: LiveData<List<RequirementItem>> = _requirements

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    fun getRequirementsByEventId(token: String, eventId: Int) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _error.value = null

                val requirements = requirementRepository.getRequirementsByEventId(token, eventId)
                _requirements.value = requirements

            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to fetch requirements"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun clearError() {
        _error.value = null
    }
}