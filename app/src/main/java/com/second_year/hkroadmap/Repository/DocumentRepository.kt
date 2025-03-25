package com.second_year.hkroadmap.Repository

import android.util.Log
import com.google.gson.Gson
import com.second_year.hkroadmap.Api.Interfaces.ApiService
import com.second_year.hkroadmap.Api.Models.*
import com.second_year.hkroadmap.Utils.DocumentUploadUtils
import retrofit2.HttpException
import retrofit2.Response
import java.io.File
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody

class DocumentRepository(private val apiService: ApiService) {
    companion object {
        private const val TAG = "DocumentRepository"
    }

    // Document Management Methods
    suspend fun getStudentDocuments(token: String): Result<List<DocumentResponse>> {
        return try {
            Log.d(TAG, "Fetching student documents with token length: ${token.length}")
            val response = apiService.getStudentDocuments("Bearer $token")
            handleResponse(response) { it.documents ?: emptyList() }
        } catch (e: Exception) {
            logAndWrapError("getStudentDocuments", e)
        }
    }

    suspend fun uploadDocument(
        token: String,
        file: File,
        eventId: Int,
        requirementId: Int
    ): Result<DocumentStatusResponse> {
        return try {
            Log.d(TAG, """
                Starting document upload:
                - File name: ${file.name}
                - File size: ${file.length()}
                - MIME type: ${DocumentUploadUtils.getMimeType(file)}
                - Event ID: $eventId
                - Requirement ID: $requirementId
            """.trimIndent())

            // Validate and create file part
            val filePart = DocumentUploadUtils.createMultipartBody(file) ?: run {
                val error = DocumentUploadUtils.getFileError(file) ?: "Failed to process file"
                Log.e(TAG, "File processing failed: $error")
                return Result.failure(Exception(error))
            }

            val response = apiService.uploadDocument(
                "Bearer $token",
                filePart,
                eventId.toString().toRequestBody("text/plain".toMediaTypeOrNull()),
                requirementId.toString().toRequestBody("text/plain".toMediaTypeOrNull())
            )

            handleResponse(response)
        } catch (e: Exception) {
            logAndWrapError("uploadDocument", e)
        }
    }

    suspend fun uploadLinkDocument(
        token: String,
        eventId: Int,
        requirementId: Int,
        linkUrl: String
    ): Result<DocumentStatusResponse> {
        return try {
            Log.d(TAG, "Uploading link document: $linkUrl")
            val response = apiService.uploadLinkDocument(
                "Bearer $token",
                eventId.toString().toRequestBody("text/plain".toMediaTypeOrNull()),
                requirementId.toString().toRequestBody("text/plain".toMediaTypeOrNull()),
                linkUrl.toRequestBody("text/plain".toMediaTypeOrNull())
            )
            handleResponse(response)
        } catch (e: Exception) {
            logAndWrapError("uploadLinkDocument", e)
        }
    }

    suspend fun submitMultipleDocuments(
        token: String,
        documentIds: List<Int>
    ): Result<DocumentStatusResponse> {
        return try {
            Log.d(TAG, "Submitting multiple documents: $documentIds")
            val response = apiService.submitMultipleDocuments(
                "Bearer $token",
                DocumentIdsRequest(documentIds)
            )
            handleResponse(response)
        } catch (e: Exception) {
            logAndWrapError("submitMultipleDocuments", e)
        }
    }

    suspend fun unsubmitMultipleDocuments(
        token: String,
        documentIds: List<Int>
    ): Result<DocumentStatusResponse> {
        return try {
            Log.d(TAG, "Unsubmitting multiple documents: $documentIds")
            val response = apiService.unsubmitMultipleDocuments(
                "Bearer $token",
                DocumentIdsRequest(documentIds)
            )
            handleResponse(response)
        } catch (e: Exception) {
            logAndWrapError("unsubmitMultipleDocuments", e)
        }
    }

    suspend fun submitDocument(
        token: String,
        documentId: Int
    ): Result<DocumentStatusResponse> {
        return try {
            Log.d(TAG, "Submitting document: $documentId")
            val response = apiService.submitDocument(
                "Bearer $token",
                mapOf("document_id" to documentId)
            )
            handleResponse(response)
        } catch (e: Exception) {
            logAndWrapError("submitDocument", e)
        }
    }

    suspend fun unsubmitDocument(
        token: String,
        documentId: Int
    ): Result<DocumentStatusResponse> {
        return try {
            Log.d(TAG, "Unsubmitting document: $documentId")
            val response = apiService.unsubmitDocument(
                "Bearer $token",
                mapOf("document_id" to documentId)
            )
            handleResponse(response)
        } catch (e: Exception) {
            logAndWrapError("unsubmitDocument", e)
        }
    }

    suspend fun deleteDocument(
        token: String,
        documentId: Int
    ): Result<DocumentStatusResponse> {
        return try {
            Log.d(TAG, "Deleting document: $documentId")
            val response = apiService.deleteDocument(
                "Bearer $token",
                mapOf("document_id" to documentId)
            )
            handleResponse(response)
        } catch (e: Exception) {
            logAndWrapError("deleteDocument", e)
        }
    }

    // Comment Management Methods
    suspend fun getComments(
        token: String,
        requirementId: Int,
        studentId: Int
    ): Result<List<Comment>> {
        return try {
            Log.d(TAG, "Fetching comments for requirement: $requirementId, student: $studentId")
            val response = apiService.getComments("Bearer $token", requirementId, studentId)
            handleResponse(response) { commentResponses ->
                commentResponses.map { mapToComment(it) }
            }
        } catch (e: Exception) {
            logAndWrapError("getComments", e)
        }
    }

    suspend fun addComment(
        token: String,
        comment: CommentRequest
    ): Result<CommentOperationResponse> {
        return try {
            Log.d(TAG, "Adding comment for requirement: ${comment.requirementId}")
            val response = apiService.addComment("Bearer $token", comment)
            handleResponse(response)
        } catch (e: Exception) {
            logAndWrapError("addComment", e)
        }
    }

    suspend fun updateComment(
        token: String,
        comment: CommentUpdateRequest
    ): Result<CommentOperationResponse> {
        return try {
            Log.d(TAG, "Updating comment: ${comment.commentId}")
            val response = apiService.updateComment("Bearer $token", comment)
            handleResponse(response)
        } catch (e: Exception) {
            logAndWrapError("updateComment", e)
        }
    }

    suspend fun deleteComment(
        token: String,
        commentId: Int
    ): Result<Unit> {
        return try {
            Log.d(TAG, "Deleting comment: $commentId")
            val response = apiService.deleteComment(
                "Bearer $token",
                mapOf("comment_id" to commentId)
            )
            handleResponse(response)
        } catch (e: Exception) {
            logAndWrapError("deleteComment", e)
        }
    }

    suspend fun refreshComments(
        token: String,
        requirementId: Int,
        studentId: Int
    ): Result<List<Comment>> {
        return getComments(token, requirementId, studentId)
    }


    // Helper Methods
    private fun <T, R> handleResponse(
        response: Response<T>,
        transform: (T) -> R = { it as R }
    ): Result<R> {
        return when {
            response.isSuccessful -> {
                response.body()?.let {
                    Result.success(transform(it))
                } ?: Result.failure(Exception("Empty response body"))
            }
            else -> {
                val errorMessage = parseErrorResponse(response)
                Result.failure(Exception(errorMessage))
            }
        }
    }

    private fun parseErrorResponse(response: Response<*>): String {
        val errorBody = response.errorBody()?.string()
        return try {
            Gson().fromJson(errorBody, ErrorResponse::class.java).message
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing error response", e)
            response.message() ?: "Unknown error occurred"
        }
    }

    private fun mapToComment(response: CommentResponse): Comment {
        return Comment(
            commentId = response.commentId,
            documentId = response.documentId,
            requirementId = response.requirementId,
            studentId = response.studentId,
            userType = response.userType,
            userName = response.userName,
            body = response.body,
            createdAt = response.createdAt,
            updatedAt = response.updatedAt,
            isOwner = response.isOwner
        )
    }

    private fun logAndWrapError(operation: String, error: Exception): Result<Nothing> {
        Log.e(TAG, "Exception in $operation", error)
        val message = when (error) {
            is HttpException -> "Network error: ${error.message()}"
            else -> "Operation failed: ${error.message}"
        }
        return Result.failure(Exception(message))
    }
}