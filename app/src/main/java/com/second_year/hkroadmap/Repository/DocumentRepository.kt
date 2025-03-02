package com.second_year.hkroadmap.Repository

import android.util.Log
import com.google.gson.Gson
import com.second_year.hkroadmap.Api.Interfaces.ApiService
import com.second_year.hkroadmap.Api.Models.DocumentDeleteRequest
import com.second_year.hkroadmap.Api.Models.DocumentResponse
import com.second_year.hkroadmap.Api.Models.DocumentUploadResponse
import com.second_year.hkroadmap.Api.Models.ErrorResponse
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.HttpException
import java.io.File

class DocumentRepository(private val apiService: ApiService) {
    companion object {
        private const val TAG = "DocumentRepository"
    }

    suspend fun getStudentDocuments(token: String): Result<List<DocumentResponse>> {
        return try {
            val bearerToken = if (!token.startsWith("Bearer ")) "Bearer $token" else token
            Log.d(TAG, "Fetching documents with token: ${bearerToken.take(15)}...")

            val response = apiService.getStudentDocuments(bearerToken)
            Log.d(TAG, "Successfully fetched ${response.size} documents")
            Result.success(response)
        } catch (e: HttpException) {
            handleHttpError(e)
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching documents: ${e.message}")
            Result.failure(e)
        }
    }

    suspend fun uploadDocument(
        token: String,
        file: File,
        eventId: Int,
        requirementId: Int
    ): Result<DocumentUploadResponse> {
        return try {
            val bearerToken = if (!token.startsWith("Bearer ")) "Bearer $token" else token
            Log.d(TAG, """
                Uploading document:
                - File: ${file.name} (${file.length()} bytes)
                - Event ID: $eventId
                - Requirement ID: $requirementId
            """.trimIndent())

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
                bearerToken,
                filePart,
                eventIdBody,
                requirementIdBody
            )

            Log.d(TAG, "Document upload successful")
            Result.success(response)
        } catch (e: HttpException) {
            handleHttpError(e)
        } catch (e: Exception) {
            Log.e(TAG, "Error uploading document: ${e.message}")
            Result.failure(e)
        }
    }

    suspend fun deleteDocument(
        token: String,
        eventId: Int,
        requirementId: Int,
        documentId: Int
    ): Result<String> {
        return try {
            val bearerToken = if (!token.startsWith("Bearer ")) "Bearer $token" else token
            Log.d(TAG, """
                Deleting document:
                - Document ID: $documentId
                - Event ID: $eventId
                - Requirement ID: $requirementId
            """.trimIndent())

            val request = DocumentDeleteRequest(eventId, requirementId, documentId)
            val response = apiService.deleteDocument(bearerToken, request)

            Log.d(TAG, "Document deletion successful")
            Result.success(response.message)
        } catch (e: HttpException) {
            handleHttpError(e)
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting document: ${e.message}")
            Result.failure(e)
        }
    }

    private fun <T> handleHttpError(e: HttpException): Result<T> {
        return try {
            val errorBody = e.response()?.errorBody()?.string()
            val errorResponse = Gson().fromJson(errorBody, ErrorResponse::class.java)
            Log.e(TAG, "HTTP error: ${errorResponse.message}")
            Result.failure(Exception(errorResponse.message))
        } catch (e2: Exception) {
            Log.e(TAG, "Error parsing error response", e2)
            Result.failure(e)
        }
    }
}