package com.second_year.hkroadmap.Repository

import android.util.Log
import com.google.gson.Gson
import com.second_year.hkroadmap.Api.Interfaces.ApiService
import com.second_year.hkroadmap.Api.Models.DocumentIdsRequest
import com.second_year.hkroadmap.Api.Models.DocumentResponse
import com.second_year.hkroadmap.Api.Models.DocumentStatusResponse
import com.second_year.hkroadmap.Api.Models.ErrorResponse
import com.second_year.hkroadmap.Utils.DocumentUploadUtils
import retrofit2.HttpException
import java.io.File
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody

class DocumentRepository(private val apiService: ApiService) {
    companion object {
        private const val TAG = "DocumentRepository"
    }

    suspend fun getStudentDocuments(token: String): Result<List<DocumentResponse>> {
        return try {
            Log.d(TAG, "Fetching student documents with token length: ${token.length}")

            val response = apiService.getStudentDocuments("Bearer $token")

            Log.d(TAG, "Student documents response code: ${response.code()}")

            if (response.isSuccessful) {
                val documentList = response.body()?.documents ?: emptyList()
                Log.d(TAG, "Retrieved ${documentList.size} documents")
                Result.success(documentList)
            } else {
                val errorBody = response.errorBody()?.string()
                Log.e(TAG, "Error fetching documents: $errorBody")
                val errorMessage = try {
                    Gson().fromJson(errorBody, ErrorResponse::class.java).message
                } catch (e: Exception) {
                    Log.e(TAG, "Error parsing error response", e)
                    response.message() ?: "Unknown error occurred"
                }
                Result.failure(Exception(errorMessage))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception in getStudentDocuments", e)
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

            // Create other parts
            val eventIdBody = eventId.toString().toRequestBody("text/plain".toMediaTypeOrNull())
            val requirementIdBody = requirementId.toString().toRequestBody("text/plain".toMediaTypeOrNull())

            Log.d(TAG, "Sending upload request to server")
            val response = apiService.uploadDocument(
                "Bearer $token",
                filePart,
                eventIdBody,
                requirementIdBody
            )

            Log.d(TAG, "Upload response code: ${response.code()}")

            if (response.isSuccessful) {
                Log.d(TAG, "Upload successful: ${response.body()}")
                Result.success(response.body()!!)
            } else {
                Log.e(TAG, "Upload failed: ${response.errorBody()?.string()}")
                Result.failure(Exception(response.message()))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception during upload", e)
            Result.failure(e)
        }
    }

    suspend fun uploadLinkDocument(
        token: String,
        eventId: Int,
        requirementId: Int,
        linkUrl: String
    ): Result<DocumentStatusResponse> {
        return try {
            Log.d(TAG, "Uploading link: $linkUrl")

            val eventIdBody = eventId.toString().toRequestBody("text/plain".toMediaTypeOrNull())
            val requirementIdBody = requirementId.toString().toRequestBody("text/plain".toMediaTypeOrNull())
            val linkUrlBody = linkUrl.toRequestBody("text/plain".toMediaTypeOrNull())

            val response = apiService.uploadLinkDocument(
                "Bearer $token",
                eventIdBody,
                requirementIdBody,
                linkUrlBody
            )

            if (response.isSuccessful) {
                Log.d(TAG, "Link upload successful")
                Result.success(response.body()!!)
            } else {
                Log.e(TAG, "Link upload failed: ${response.errorBody()?.string()}")
                Result.failure(Exception(response.message()))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception during link upload", e)
            Result.failure(e)
        }
    }

    suspend fun submitMultipleDocuments(
        token: String,
        documentIds: List<Int>
    ): Result<DocumentStatusResponse> {
        return try {
            Log.d(TAG, """
                Submitting multiple documents:
                - Document IDs: $documentIds
                - Token length: ${token.length}
            """.trimIndent())

            // Use DocumentIdsRequest instead of Map
            val requestBody = DocumentIdsRequest(documentIds)
            val response = apiService.submitMultipleDocuments("Bearer $token", requestBody)

            Log.d(TAG, """
                Submit multiple response:
                - Status code: ${response.code()}
                - Is successful: ${response.isSuccessful}
            """.trimIndent())

            when {
                response.isSuccessful -> {
                    response.body()?.let {
                        Log.d(TAG, "Multiple documents submitted successfully")
                        Result.success(it)
                    } ?: run {
                        Log.e(TAG, "Submit multiple successful but empty response body")
                        Result.failure(Exception("Empty response body"))
                    }
                }
                else -> {
                    val errorBody = response.errorBody()?.string()
                    Log.e(TAG, "Submit multiple failed: $errorBody")
                    val errorMessage = try {
                        Gson().fromJson(errorBody, ErrorResponse::class.java).message
                    } catch (e: Exception) {
                        Log.e(TAG, "Error parsing submit multiple error response", e)
                        response.message() ?: "Unknown error occurred"
                    }
                    Result.failure(Exception(errorMessage))
                }
            }
        } catch (e: HttpException) {
            Log.e(TAG, "HTTP Exception in submitMultipleDocuments", e)
            Result.failure(Exception("Network error: ${e.message()}"))
        } catch (e: Exception) {
            Log.e(TAG, "Exception in submitMultipleDocuments", e)
            Result.failure(Exception("Failed to submit documents: ${e.message}"))
        }
    }

    suspend fun unsubmitMultipleDocuments(
        token: String,
        documentIds: List<Int>
    ): Result<DocumentStatusResponse> {
        return try {
            Log.d(TAG, """
                Unsubmitting multiple documents:
                - Document IDs: $documentIds
                - Token length: ${token.length}
            """.trimIndent())

            // Use DocumentIdsRequest instead of Map
            val requestBody = DocumentIdsRequest(documentIds)
            val response = apiService.unsubmitMultipleDocuments("Bearer $token", requestBody)

            Log.d(TAG, """
                Unsubmit multiple response:
                - Status code: ${response.code()}
                - Is successful: ${response.isSuccessful}
            """.trimIndent())

            when {
                response.isSuccessful -> {
                    response.body()?.let {
                        Log.d(TAG, "Multiple documents unsubmitted successfully")
                        Result.success(it)
                    } ?: run {
                        Log.e(TAG, "Unsubmit multiple successful but empty response body")
                        Result.failure(Exception("Empty response body"))
                    }
                }
                else -> {
                    val errorBody = response.errorBody()?.string()
                    Log.e(TAG, "Unsubmit multiple failed: $errorBody")
                    val errorMessage = try {
                        Gson().fromJson(errorBody, ErrorResponse::class.java).message
                    } catch (e: Exception) {
                        Log.e(TAG, "Error parsing unsubmit multiple error response", e)
                        response.message() ?: "Unknown error occurred"
                    }
                    Result.failure(Exception(errorMessage))
                }
            }
        } catch (e: HttpException) {
            Log.e(TAG, "HTTP Exception in unsubmitMultipleDocuments", e)
            Result.failure(Exception("Network error: ${e.message()}"))
        } catch (e: Exception) {
            Log.e(TAG, "Exception in unsubmitMultipleDocuments", e)
            Result.failure(Exception("Failed to unsubmit documents: ${e.message}"))
        }
    }

    suspend fun submitDocument(token: String, documentId: Int): Result<DocumentStatusResponse> {
        return try {
            Log.d(TAG, "Submitting document: $documentId")

            val requestBody = mapOf("document_id" to documentId)
            val response = apiService.submitDocument("Bearer $token", requestBody)

            Log.d(TAG, "Submit response code: ${response.code()}")

            when {
                response.isSuccessful -> {
                    response.body()?.let {
                        Log.d(TAG, "Document submitted successfully: ${it.document_id}")
                        Result.success(it)
                    } ?: run {
                        Log.e(TAG, "Submit successful but empty response body")
                        Result.failure(Exception("Empty response body"))
                    }
                }
                else -> {
                    val errorBody = response.errorBody()?.string()
                    Log.e(TAG, "Submit failed: $errorBody")
                    val errorMessage = try {
                        Gson().fromJson(errorBody, ErrorResponse::class.java).message
                    } catch (e: Exception) {
                        Log.e(TAG, "Error parsing submit error response", e)
                        response.message() ?: "Unknown error occurred"
                    }
                    Result.failure(Exception(errorMessage))
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception in submitDocument", e)
            Result.failure(e)
        }
    }

    suspend fun unsubmitDocument(token: String, documentId: Int): Result<DocumentStatusResponse> {
        return try {
            Log.d(TAG, """
                Unsubmit request:
                - Document ID: $documentId
                - Token length: ${token.length}
            """.trimIndent())

            val requestBody = mapOf("document_id" to documentId)
            val response = apiService.unsubmitDocument("Bearer $token", requestBody)

            Log.d(TAG, """
                Unsubmit response:
                - Status code: ${response.code()}
                - Is successful: ${response.isSuccessful}
                - Raw response: ${response.raw()}
            """.trimIndent())

            when {
                response.isSuccessful -> {
                    response.body()?.let {
                        Log.d(TAG, "Document unsubmitted successfully: ${it.document_id}")
                        Result.success(it)
                    } ?: run {
                        Log.e(TAG, "Unsubmit successful but empty response body")
                        Result.failure(Exception("Empty response body"))
                    }
                }
                response.code() == 500 -> {
                    val errorBody = response.errorBody()?.string()
                    Log.e(TAG, "Server error during unsubmit: $errorBody")
                    val errorMessage = try {
                        Gson().fromJson(errorBody, ErrorResponse::class.java).message
                    } catch (e: Exception) {
                        Log.e(TAG, "Error parsing unsubmit error response", e)
                        "Server error occurred"
                    }
                    Result.failure(Exception(errorMessage))
                }
                else -> {
                    val errorBody = response.errorBody()?.string()
                    Log.e(TAG, "Unsubmit failed with code ${response.code()}: $errorBody")
                    val message = try {
                        Gson().fromJson(errorBody, ErrorResponse::class.java).message
                    } catch (e: Exception) {
                        Log.e(TAG, "Error parsing unsubmit error response", e)
                        response.message() ?: "Unknown error occurred"
                    }
                    Result.failure(Exception(message))
                }
            }
        } catch (e: HttpException) {
            Log.e(TAG, "HTTP Exception in unsubmitDocument", e)
            Result.failure(Exception("Network error: ${e.message()}"))
        } catch (e: Exception) {
            Log.e(TAG, "Exception in unsubmitDocument", e)
            Result.failure(Exception("Failed to unsubmit document: ${e.message}"))
        }
    }

    suspend fun deleteDocument(token: String, documentId: Int): Result<DocumentStatusResponse> {
        return try {
            Log.d(TAG, "Deleting document: $documentId")

            val requestBody = mapOf("document_id" to documentId)
            val response = apiService.deleteDocument("Bearer $token", requestBody)

            Log.d(TAG, "Delete response code: ${response.code()}")

            if (response.isSuccessful) {
                response.body()?.let {
                    Log.d(TAG, "Document deleted successfully: ${it.document_id}")
                    Result.success(it)
                } ?: run {
                    Log.e(TAG, "Delete successful but empty response body")
                    Result.failure(Exception("Empty response body"))
                }
            } else {
                val errorBody = response.errorBody()?.string()
                Log.e(TAG, "Delete failed: $errorBody")
                val errorMessage = try {
                    Gson().fromJson(errorBody, ErrorResponse::class.java).message
                } catch (e: Exception) {
                    Log.e(TAG, "Error parsing delete error response", e)
                    response.message() ?: "Unknown error occurred"
                }
                Result.failure(Exception(errorMessage))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception in deleteDocument", e)
            Result.failure(e)
        }
    }
}