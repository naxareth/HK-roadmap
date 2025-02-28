package com.second_year.hkroadmap.Repository

import com.second_year.hkroadmap.Api.Interfaces.ApiService
import com.second_year.hkroadmap.Api.Models.DocumentDeleteRequest
import com.second_year.hkroadmap.Api.Models.DocumentResponse
import com.second_year.hkroadmap.Api.Models.DocumentUploadResponse
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File

class DocumentRepository(private val apiService: ApiService) {

    // Get all documents for the authenticated student
    suspend fun getStudentDocuments(token: String): Result<List<DocumentResponse>> {
        return try {
            val response = apiService.getStudentDocuments("Bearer $token")
            Result.success(response.documents)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Upload document
    suspend fun uploadDocument(
        token: String,
        file: File,
        eventId: Int,
        requirementId: Int
    ): Result<DocumentUploadResponse> {
        return try {
            // Create request body for file
            val requestFile = file.asRequestBody("multipart/form-data".toMediaTypeOrNull())
            val filePart = MultipartBody.Part.createFormData(
                "documents",
                file.name,
                requestFile
            )

            // Create request bodies for other parameters
            val eventIdBody = eventId.toString().toRequestBody("text/plain".toMediaTypeOrNull())
            val requirementIdBody = requirementId.toString().toRequestBody("text/plain".toMediaTypeOrNull())

            val response = apiService.uploadDocument(
                "Bearer $token",
                filePart,
                eventIdBody,
                requirementIdBody
            )

            Result.success(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Delete document
    suspend fun deleteDocument(
        token: String,
        eventId: Int,
        requirementId: Int,
        documentId: Int
    ): Result<String> {
        return try {
            val request = DocumentDeleteRequest(eventId, requirementId, documentId)
            val response = apiService.deleteDocument("Bearer $token", request)
            Result.success(response.message)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}