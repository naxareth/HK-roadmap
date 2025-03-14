package com.second_year.hkroadmap.ViewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.second_year.hkroadmap.Api.Interfaces.ApiService
import com.second_year.hkroadmap.Api.Repository.RequirementRepository
import com.second_year.hkroadmap.Repository.DocumentRepository

class ViewModelFactory(
    private val documentRepository: DocumentRepository? = null,
    private val requirementRepository: RequirementRepository? = null,
    private val apiService: ApiService? = null
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when {
            modelClass.isAssignableFrom(DocumentViewModel::class.java) -> {
                documentRepository?.let {
                    DocumentViewModel(it) as T
                } ?: throw IllegalArgumentException("DocumentRepository required for DocumentViewModel")
            }
            modelClass.isAssignableFrom(RequirementViewModel::class.java) -> {
                requirementRepository?.let {
                    RequirementViewModel(it) as T
                } ?: throw IllegalArgumentException("RequirementRepository required for RequirementViewModel")
            }
            modelClass.isAssignableFrom(ProfileViewModel::class.java) -> {
                apiService?.let {
                    ProfileViewModel(it) as T
                } ?: throw IllegalArgumentException("ApiService required for ProfileViewModel")
            }
            else -> throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}