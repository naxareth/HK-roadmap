// DocumentViewModel.kt
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

    private val _studentDocuments = MutableLiveData<List<DocumentResponse>>()
    val studentDocuments: LiveData<List<DocumentResponse>> = _studentDocuments

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage

    private val _successMessage = MutableLiveData<String?>()
    val successMessage: LiveData<String?> = _successMessage

    private val _currentDocument = MutableLiveData<DocumentResponse?>()
    val currentDocument: LiveData<DocumentResponse?> = _currentDocument

    fun getStudentDocuments(token: String) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                documentRepository.getStudentDocuments(token).fold(
                    onSuccess = { documents ->
                        _studentDocuments.value = documents
                        _errorMessage.value = null
                    },
                    onFailure = { exception ->
                        _errorMessage.value = exception.message
                    }
                )
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "An unexpected error occurred"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun uploadDocument(token: String, file: File, eventId: Int, requirementId: Int) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                documentRepository.uploadDocument(token, file, eventId, requirementId).fold(
                    onSuccess = { response ->
                        _successMessage.value = "Document uploaded successfully"
                        getStudentDocuments(token)
                    },
                    onFailure = { exception ->
                        _errorMessage.value = exception.message
                    }
                )
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "An unexpected error occurred"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun submitDocument(token: String, documentId: Int) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                documentRepository.submitDocument(token, documentId).fold(
                    onSuccess = { response ->
                        _successMessage.value = "Document submitted successfully"
                        getStudentDocuments(token)
                    },
                    onFailure = { exception ->
                        _errorMessage.value = exception.message
                    }
                )
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "An unexpected error occurred"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun unsubmitDocument(token: String, documentId: Int) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                documentRepository.unsubmitDocument(token, documentId).fold(
                    onSuccess = { response ->
                        _successMessage.value = "Document unsubmitted successfully"
                        getStudentDocuments(token)
                    },
                    onFailure = { exception ->
                        _errorMessage.value = exception.message
                    }
                )
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "An unexpected error occurred"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun deleteDocument(token: String, documentId: Int) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                documentRepository.deleteDocument(token, documentId).fold(
                    onSuccess = { response ->
                        _successMessage.value = "Document deleted successfully"
                        getStudentDocuments(token)
                    },
                    onFailure = { exception ->
                        _errorMessage.value = exception.message
                    }
                )
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "An unexpected error occurred"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun clearMessages() {
        _errorMessage.value = null
        _successMessage.value = null
    }

    fun setCurrentDocument(document: DocumentResponse) {
        _currentDocument.value = document
    }

    fun clearCurrentDocument() {
        _currentDocument.value = null
    }
}