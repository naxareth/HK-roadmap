// DocumentViewModel.kt
package com.second_year.hkroadmap.ViewModel

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.second_year.hkroadmap.Api.Models.DocumentResponse
import com.second_year.hkroadmap.Api.Models.DocumentStatusResponse
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

    private val _uploadedDocumentIds = MutableLiveData<List<Int>>()
    val uploadedDocumentIds: LiveData<List<Int>> = _uploadedDocumentIds

    private val _currentDocument = MutableLiveData<DocumentResponse?>()
    val currentDocument: LiveData<DocumentResponse?> = _currentDocument

    fun getStudentDocuments(token: String) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                documentRepository.getStudentDocuments(token).fold(
                    onSuccess = { documents ->
                        _studentDocuments.value = documents
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
                        // Add the new document ID to the list
                        val currentIds = _uploadedDocumentIds.value?.toMutableList() ?: mutableListOf()
                        currentIds.add(response.document_id)
                        _uploadedDocumentIds.value = currentIds
                        getStudentDocuments(token)
                    },
                    onFailure = { exception ->
                        // Check if there are pending documents
                        val hasPendingDocs = _studentDocuments.value?.any {
                            it.event_id == eventId &&
                                    it.requirement_id == requirementId &&
                                    it.status.lowercase() == "pending"
                        } ?: false

                        _errorMessage.value = if (hasPendingDocs) {
                            "Cannot upload new documents while there are pending submissions"
                        } else {
                            exception.message
                        }
                    }
                )
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "An unexpected error occurred"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun uploadLinkDocument(token: String, linkUrl: String, eventId: Int, requirementId: Int) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                documentRepository.uploadLinkDocument(token, eventId, requirementId, linkUrl).fold(
                    onSuccess = { response ->
                        _successMessage.value = "Link uploaded successfully"
                        // Add the new document ID to the list
                        val currentIds = _uploadedDocumentIds.value?.toMutableList() ?: mutableListOf()
                        currentIds.add(response.document_id)
                        _uploadedDocumentIds.value = currentIds
                        getStudentDocuments(token)
                    },
                    onFailure = { exception ->
                        // Check if there are pending documents
                        val hasPendingDocs = _studentDocuments.value?.any {
                            it.event_id == eventId &&
                                    it.requirement_id == requirementId &&
                                    it.status.lowercase() == "pending"
                        } ?: false

                        _errorMessage.value = if (hasPendingDocs) {
                            "Cannot upload new links while there are pending submissions"
                        } else {
                            exception.message
                        }
                    }
                )
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "An unexpected error occurred"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun submitMultipleDocuments(token: String, documentIds: List<Int>) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                documentRepository.submitMultipleDocuments(token, documentIds).fold(
                    onSuccess = { response ->
                        _successMessage.value = "All documents submitted successfully"
                        // Clear the uploaded document IDs after successful submission
                        _uploadedDocumentIds.value = emptyList()
                        getStudentDocuments(token)
                    },
                    onFailure = { exception ->
                        _errorMessage.value = exception.message
                    }
                )
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun unsubmitMultipleDocuments(token: String, documentIds: List<Int>) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                documentRepository.unsubmitMultipleDocuments(token, documentIds).fold(
                    onSuccess = { response ->
                        _successMessage.value = "All documents unsubmitted successfully"
                        // Clear the uploaded document IDs after successful unsubmission
                        _uploadedDocumentIds.value = emptyList()
                        getStudentDocuments(token)
                    },
                    onFailure = { exception ->
                        _errorMessage.value = exception.message
                    }
                )
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
                        // Remove the deleted document ID from the list if it exists
                        val currentIds = _uploadedDocumentIds.value?.toMutableList() ?: mutableListOf()
                        currentIds.remove(documentId)
                        _uploadedDocumentIds.value = currentIds
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

    fun clearUploadedDocumentIds() {
        _uploadedDocumentIds.value = emptyList()
    }

    fun setCurrentDocument(document: DocumentResponse) {
        _currentDocument.value = document
    }

    fun clearCurrentDocument() {
        _currentDocument.value = null
    }
}