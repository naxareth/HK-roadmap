package com.second_year.hkroadmap.ViewModel

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.second_year.hkroadmap.Api.Models.Comment
import com.second_year.hkroadmap.Api.Models.CommentRequest
import com.second_year.hkroadmap.Api.Models.CommentUpdateRequest
import com.second_year.hkroadmap.Api.Models.DocumentResponse
import com.second_year.hkroadmap.Api.Models.DocumentStatusResponse
import com.second_year.hkroadmap.Repository.DocumentRepository
import kotlinx.coroutines.launch
import java.io.File

class DocumentViewModel(private val documentRepository: DocumentRepository) : ViewModel() {
    // Existing document-related LiveData
    private val _studentDocuments = MutableLiveData<List<DocumentResponse>>()
    val studentDocuments: LiveData<List<DocumentResponse>> = _studentDocuments

    private val _filteredDocuments = MutableLiveData<List<DocumentResponse>>()
    val filteredDocuments: LiveData<List<DocumentResponse>> = _filteredDocuments

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

    // New comment-related LiveData
    private val _comments = MutableLiveData<List<Comment>>()
    val comments: LiveData<List<Comment>> = _comments

    private val _isLoadingComments = MutableLiveData<Boolean>()
    val isLoadingComments: LiveData<Boolean> = _isLoadingComments

    private var currentFilter: String? = null

    fun getStudentDocuments(token: String) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                documentRepository.getStudentDocuments(token).fold(
                    onSuccess = { documents ->
                        _studentDocuments.value = documents
                        applyFilter(currentFilter) // Apply current filter after getting documents
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

    fun getDocumentsByEventId(token: String, eventId: Int) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                documentRepository.getStudentDocuments(token).fold(
                    onSuccess = { documents ->
                        val eventDocuments = documents.filter { it.event_id == eventId }
                        _studentDocuments.value = eventDocuments
                        applyFilter(currentFilter)
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

    fun filterDocuments(status: String?) {
        currentFilter = status
        applyFilter(status)
    }

    private fun applyFilter(status: String?) {
        val documents = _studentDocuments.value ?: emptyList()

        // First, group documents by requirement_id and take the first document for each requirement
        val uniqueDocuments = documents
            .groupBy { it.requirement_id }
            .mapValues { entry -> entry.value.first() }
            .values
            .toList()

        // Then apply status filtering
        _filteredDocuments.value = when (status?.lowercase()) {
            null, "all", "submitted" -> uniqueDocuments.filter {
                it.status.lowercase() in listOf("pending", "approved", "rejected")
            }
            else -> uniqueDocuments.filter { it.status.lowercase() == status.lowercase() }
        }

        Log.d("DocumentViewModel", """
        Filtering documents:
        - Total documents: ${documents.size}
        - Unique requirements: ${uniqueDocuments.size}
        - Filter status: $status
        - Filtered count: ${_filteredDocuments.value?.size}
    """.trimIndent())
    }

    fun getDocumentStatistics(): Map<String, Int> {
        return _studentDocuments.value
            ?.groupBy { it.requirement_id } // First group by requirement_id
            ?.mapValues { it.value.first() } // Take first document from each requirement
            ?.values
            ?.groupBy { it.status.lowercase() } // Then group by status
            ?.mapValues { it.value.size } // Count unique requirements per status
            ?: emptyMap()
    }

    fun uploadDocument(token: String, file: File, eventId: Int, requirementId: Int) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                documentRepository.uploadDocument(token, file, eventId, requirementId).fold(
                    onSuccess = { response ->
                        _successMessage.value = "Document uploaded successfully"
                        val currentIds = _uploadedDocumentIds.value?.toMutableList() ?: mutableListOf()
                        currentIds.add(response.document_id)
                        _uploadedDocumentIds.value = currentIds
                        getStudentDocuments(token)
                    },
                    onFailure = { exception ->
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
                        val currentIds = _uploadedDocumentIds.value?.toMutableList() ?: mutableListOf()
                        currentIds.add(response.document_id)
                        _uploadedDocumentIds.value = currentIds
                        getStudentDocuments(token)
                    },
                    onFailure = { exception ->
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

    // New comment-related functions
    fun getComments(token: String, requirementId: Int, studentId: Int) {
        viewModelScope.launch {
            try {
                _isLoadingComments.value = true
                documentRepository.getComments(token, requirementId, studentId).fold(
                    onSuccess = { commentsList ->
                        _comments.value = commentsList
                    },
                    onFailure = { exception ->
                        _errorMessage.value = exception.message
                    }
                )
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "An unexpected error occurred"
            } finally {
                _isLoadingComments.value = false
            }
        }
    }

    fun addComment(token: String, requirementId: Int, studentId: Int, body: String) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                val comment = CommentRequest(
                    requirementId = requirementId,
                    studentId = studentId,
                    body = body
                )

                // First add the comment
                documentRepository.addComment(token, comment).fold(
                    onSuccess = { response ->
                        // If successful, refresh the comments list
                        documentRepository.refreshComments(token, requirementId, studentId).fold(
                            onSuccess = { comments ->
                                _comments.value = comments
                                _successMessage.value = response.message
                            },
                            onFailure = { exception ->
                                _errorMessage.value = exception.message
                            }
                        )
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

    fun updateComment(token: String, commentId: Int, body: String, requirementId: Int, studentId: Int) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                val comment = CommentUpdateRequest(commentId, body)

                // First update the comment
                documentRepository.updateComment(token, comment).fold(
                    onSuccess = { response ->
                        // If successful, refresh the comments list
                        documentRepository.refreshComments(token, requirementId, studentId).fold(
                            onSuccess = { comments ->
                                _comments.value = comments
                                _successMessage.value = response.message
                            },
                            onFailure = { exception ->
                                _errorMessage.value = exception.message
                            }
                        )
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

    fun deleteComment(token: String, commentId: Int) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                documentRepository.deleteComment(token, commentId).fold(
                    onSuccess = {
                        // Remove comment from the list
                        val currentComments = _comments.value.orEmpty().toMutableList()
                        currentComments.removeAll { it.commentId == commentId }
                        _comments.value = currentComments
                        _successMessage.value = "Comment deleted successfully"
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

    // Existing utility functions
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

    // New utility function for comments
    fun clearComments() {
        _comments.value = emptyList()
    }
}