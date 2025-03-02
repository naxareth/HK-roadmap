package com.second_year.hkroadmap.ViewModel

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.second_year.hkroadmap.Api.Models.DocumentResponse
import com.second_year.hkroadmap.Api.Models.DocumentUploadResponse
import com.second_year.hkroadmap.Repository.DocumentRepository
import kotlinx.coroutines.launch
import java.io.File

class DocumentViewModel(private val documentRepository: DocumentRepository) : ViewModel() {
    companion object {
        private const val TAG = "DocumentViewModel"
    }

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
            try {
                _isLoading.value = true
                Log.d(TAG, "Fetching student documents")

                documentRepository.getStudentDocuments(token).fold(
                    onSuccess = { documents ->
                        Log.d(TAG, "Successfully fetched ${documents.size} documents")
                        _studentDocuments.value = documents
                        _isLoading.value = false
                    },
                    onFailure = { exception ->
                        Log.e(TAG, "Failed to fetch documents", exception)
                        _errorMessage.value = exception.message ?: "Failed to get documents"
                        _isLoading.value = false
                    }
                )
            } catch (e: Exception) {
                Log.e(TAG, "Exception in getStudentDocuments", e)
                _errorMessage.value = "An unexpected error occurred"
                _isLoading.value = false
            }
        }
    }

    // Upload document
    fun uploadDocument(token: String, file: File, eventId: Int, requirementId: Int) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                Log.d(TAG, "Uploading document: ${file.name}")

                documentRepository.uploadDocument(token, file, eventId, requirementId).fold(
                    onSuccess = { response ->
                        Log.d(TAG, "Document upload successful")
                        _successMessage.value = response.message
                        _isLoading.value = false
                        // Refresh the student documents list
                        getStudentDocuments(token)
                    },
                    onFailure = { exception ->
                        Log.e(TAG, "Failed to upload document", exception)
                        _errorMessage.value = exception.message ?: "Failed to upload document"
                        _isLoading.value = false
                    }
                )
            } catch (e: Exception) {
                Log.e(TAG, "Exception in uploadDocument", e)
                _errorMessage.value = "An unexpected error occurred during upload"
                _isLoading.value = false
            }
        }
    }

    // Delete document
    fun deleteDocument(token: String, eventId: Int, requirementId: Int, documentId: Int) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                Log.d(TAG, "Deleting document: $documentId")

                documentRepository.deleteDocument(token, eventId, requirementId, documentId).fold(
                    onSuccess = { response ->
                        Log.d(TAG, "Document deletion successful")
                        _successMessage.value = response ?: "Document deleted successfully"
                        _isLoading.value = false
                        // Refresh the student documents list
                        getStudentDocuments(token)
                    },
                    onFailure = { exception ->
                        Log.e(TAG, "Failed to delete document", exception)
                        _errorMessage.value = exception.message ?: "Failed to delete document"
                        _isLoading.value = false
                    }
                )
            } catch (e: Exception) {
                Log.e(TAG, "Exception in deleteDocument", e)
                _errorMessage.value = "An unexpected error occurred during deletion"
                _isLoading.value = false
            }
        }
    }

    // Clear error message
    fun clearError() {
        _errorMessage.value = null
    }

    // Clear success message
    fun clearSuccess() {
        _successMessage.value = null
    }
}