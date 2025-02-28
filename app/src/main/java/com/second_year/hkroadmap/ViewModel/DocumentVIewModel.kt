package com.second_year.hkroadmap.ViewModel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.second_year.hkroadmap.Api.Models.DocumentResponse
import com.second_year.hkroadmap.Repository.DocumentRepository
import kotlinx.coroutines.launch
import java.io.File

class DocumentViewModel(private val documentRepository: DocumentRepository) : ViewModel() {

    // LiveData for student documents
    private val _studentDocuments = MutableLiveData<List<DocumentResponse>>()
    val studentDocuments: LiveData<List<DocumentResponse>> = _studentDocuments

    // LiveData for loading state
    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    // LiveData for error messages
    private val _errorMessage = MutableLiveData<String>()
    val errorMessage: LiveData<String> = _errorMessage

    // LiveData for success messages
    private val _successMessage = MutableLiveData<String>()
    val successMessage: LiveData<String> = _successMessage

    // Get all documents for the authenticated student
    fun getStudentDocuments(token: String) {
        viewModelScope.launch {
            _isLoading.value = true
            documentRepository.getStudentDocuments(token).fold(
                onSuccess = { documents ->
                    _studentDocuments.value = documents
                    _isLoading.value = false
                },
                onFailure = { exception ->
                    _errorMessage.value = exception.message ?: "Failed to get documents"
                    _isLoading.value = false
                }
            )
        }
    }

    // Upload document
    fun uploadDocument(token: String, file: File, eventId: Int, requirementId: Int) {
        viewModelScope.launch {
            _isLoading.value = true
            documentRepository.uploadDocument(token, file, eventId, requirementId).fold(
                onSuccess = { response ->
                    _successMessage.value = response.message
                    _isLoading.value = false
                    // Refresh the student documents list
                    getStudentDocuments(token)
                },
                onFailure = { exception ->
                    _errorMessage.value = exception.message ?: "Failed to upload document"
                    _isLoading.value = false
                }
            )
        }
    }

    // Delete document
    fun deleteDocument(token: String, eventId: Int, requirementId: Int, documentId: Int) {
        viewModelScope.launch {
            _isLoading.value = true
            documentRepository.deleteDocument(token, eventId, requirementId, documentId).fold(
                onSuccess = { response ->
                    // Use a default message if the response doesn't have a message property
                    _successMessage.value = "Document deleted successfully"
                    _isLoading.value = false
                    // Refresh the student documents list
                    getStudentDocuments(token)
                },
                onFailure = { exception ->
                    _errorMessage.value = exception.message ?: "Failed to delete document"
                    _isLoading.value = false
                }
            )
        }
    }
}