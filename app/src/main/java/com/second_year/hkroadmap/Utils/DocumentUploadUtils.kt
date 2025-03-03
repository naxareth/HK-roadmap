package com.second_year.hkroadmap.Utils

import android.util.Log
import android.webkit.MimeTypeMap
import com.second_year.hkroadmap.Api.Models.FileConstants
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File

object DocumentUploadUtils {
    private const val TAG = "DocumentUploadUtils"

    fun validateFile(file: File): Boolean {
        val mimeType = getMimeType(file)
        Log.d(TAG, """
            Validating file:
            - Name: ${file.name}
            - Size: ${file.length()}
            - MIME Type: $mimeType
            - Exists: ${file.exists()}
        """.trimIndent())

        return when {
            !file.exists() -> {
                Log.d(TAG, "File does not exist")
                false
            }
            file.length() > FileConstants.MAX_FILE_SIZE -> {
                Log.d(TAG, "File size exceeds limit: ${file.length()} > ${FileConstants.MAX_FILE_SIZE}")
                false
            }
            mimeType == null -> {
                Log.d(TAG, "Could not determine MIME type")
                false
            }
            !FileConstants.ALLOWED_MIME_TYPES.contains(mimeType) -> {
                Log.d(TAG, "MIME type not allowed: $mimeType")
                false
            }
            else -> {
                Log.d(TAG, "File validation successful")
                true
            }
        }
    }

    fun getMimeType(file: File): String? {
        val extension = file.extension.toLowerCase()
        val mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension)
        Log.d(TAG, """
            Getting MIME type:
            - File: ${file.name}
            - Extension: $extension
            - Detected MIME type: $mimeType
        """.trimIndent())
        return mimeType
    }

    fun createMultipartBody(file: File): MultipartBody.Part? {
        Log.d(TAG, "Creating MultipartBody for file: ${file.name}")

        if (!validateFile(file)) {
            Log.d(TAG, "File validation failed")
            return null
        }

        val mimeType = getMimeType(file) ?: run {
            Log.d(TAG, "Failed to get MIME type")
            return null
        }

        Log.d(TAG, "Creating RequestBody with MIME type: $mimeType")
        val requestFile = file.asRequestBody(mimeType.toMediaTypeOrNull())

        return MultipartBody.Part.createFormData(
            "documents",
            file.name,
            requestFile
        ).also {
            Log.d(TAG, "MultipartBody created successfully")
        }
    }

    fun getFileError(file: File): String? {
        val mimeType = getMimeType(file)
        Log.d(TAG, "Checking for file errors - MIME type: $mimeType")

        return when {
            !file.exists() -> {
                Log.d(TAG, "Error: File does not exist")
                "File does not exist"
            }
            file.length() > FileConstants.MAX_FILE_SIZE -> {
                Log.d(TAG, "Error: File size exceeds limit")
                "File size exceeds 5MB limit"
            }
            mimeType == null -> {
                Log.d(TAG, "Error: Could not determine MIME type")
                "Invalid file type"
            }
            !FileConstants.ALLOWED_MIME_TYPES.contains(mimeType) -> {
                Log.d(TAG, "Error: Invalid MIME type. Allowed types: ${FileConstants.ALLOWED_MIME_TYPES}")
                "Invalid file type. Allowed types: PDF, JPEG, PNG"
            }
            else -> null
        }
    }
}