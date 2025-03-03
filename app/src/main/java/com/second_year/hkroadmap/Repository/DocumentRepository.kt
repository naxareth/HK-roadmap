package com.second_year.hkroadmap.Repository

import com.google.gson.Gson
import com.second_year.hkroadmap.Api.Interfaces.ApiService
import com.second_year.hkroadmap.Api.Models.DocumentResponse
import com.second_year.hkroadmap.Api.Models.DocumentListResponse
import com.second_year.hkroadmap.Api.Models.DocumentStatusResponse
import com.second_year.hkroadmap.Api.Models.ErrorResponse
import retrofit2.HttpException
import java.io.File
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody

class DocumentRepository(private val apiService: ApiService) {

    suspend fun getStudentDocuments(token: String): Result<List<DocumentResponse>> {
        return try {
            val response = apiService.getStudentDocuments("Bearer $token")
            if (response.isSuccessful) {
                val documentList = response.body()?.documents ?: emptyList()
                Result.success(documentList)
            } else {
                val errorBody = response.errorBody()?.string()
                val errorMessage = try {
                    Gson().fromJson(errorBody, ErrorResponse::class.java).message
                } catch (e: Exception) {
                    response.message() ?: "Unknown error occurred"
                }
                Result.failure(Exception(errorMessage))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun uploadDocument(
        token: String,
        file: File,
        eventId: Int,
        requirementId: Int
    ): Result<DocumentStatusResponse> {
        return try {
            val requestFile = file.asRequestBody("multipart/form-data".toMediaTypeOrNull())
            val filePart = MultipartBody.Part.createFormData("documents", file.name, requestFile)

            val eventIdBody = eventId.toString().toRequestBody("text/plain".toMediaTypeOrNull())
            val requirementIdBody = requirementId.toString().toRequestBody("text/plain".toMediaTypeOrNull())

            val response = apiService.uploadDocument(
                "Bearer $token",
                filePart,
                eventIdBody,
                requirementIdBody
            )

            if (response.isSuccessful) {
                response.body()?.let {
                    Result.success(it)
                } ?: Result.failure(Exception("Empty response body"))
            } else {
                val errorBody = response.errorBody()?.string()
                val errorMessage = try {
                    Gson().fromJson(errorBody, ErrorResponse::class.java).message
                } catch (e: Exception) {
                    response.message() ?: "Unknown error occurred"
                }
                Result.failure(Exception(errorMessage))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun submitDocument(token: String, documentId: Int): Result<DocumentStatusResponse> {
        return try {
            val requestBody = mapOf("document_id" to documentId)
            val response = apiService.submitDocument("Bearer $token", requestBody)

            when {
                response.isSuccessful -> {
                    response.body()?.let {
                        Result.success(it)
                    } ?: Result.failure(Exception("Empty response body"))
                }
                else -> {
                    val errorBody = response.errorBody()?.string()
                    val errorMessage = try {
                        Gson().fromJson(errorBody, ErrorResponse::class.java).message
                    } catch (e: Exception) {
                        response.message() ?: "Unknown error occurred"
                    }
                    Result.failure(Exception(errorMessage))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun unsubmitDocument(token: String, documentId: Int): Result<DocumentStatusResponse> {
        return try {
            val requestBody = mapOf("document_id" to documentId)
            val response = apiService.unsubmitDocument("Bearer $token", requestBody)

            when {
                response.isSuccessful -> {
                    response.body()?.let {
                        Result.success(it)
                    } ?: Result.failure(Exception("Empty response body"))
                }
                response.code() == 500 -> {
                    val errorBody = response.errorBody()?.string()
                    val errorMessage = try {
                        Gson().fromJson(errorBody, ErrorResponse::class.java).message
                    } catch (e: Exception) {
                        "Server error occurred"
                    }
                    Result.failure(Exception(errorMessage))
                }
                else -> {
                    val errorBody = response.errorBody()?.string()
                    val message = try {
                        Gson().fromJson(errorBody, ErrorResponse::class.java).message
                    } catch (e: Exception) {
                        response.message() ?: "Unknown error occurred"
                    }
                    Result.failure(Exception(message))
                }
            }
        } catch (e: HttpException) {
            Result.failure(Exception("Network error: ${e.message()}"))
        } catch (e: Exception) {
            Result.failure(Exception("Failed to unsubmit document: ${e.message}"))
        }
    }

    suspend fun deleteDocument(token: String, documentId: Int): Result<DocumentStatusResponse> {
        return try {
            val requestBody = mapOf("document_id" to documentId)
            val response = apiService.deleteDocument("Bearer $token", requestBody)

            if (response.isSuccessful) {
                response.body()?.let {
                    Result.success(it)
                } ?: Result.failure(Exception("Empty response body"))
            } else {
                val errorBody = response.errorBody()?.string()
                val errorMessage = try {
                    Gson().fromJson(errorBody, ErrorResponse::class.java).message
                } catch (e: Exception) {
                    response.message() ?: "Unknown error occurred"
                }
                Result.failure(Exception(errorMessage))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}