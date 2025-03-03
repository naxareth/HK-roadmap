package com.second_year.hkroadmap.Utils

import android.content.Context
import android.net.Uri
import android.webkit.MimeTypeMap
import java.io.File
import com.second_year.hkroadmap.Api.Models.FileConstants
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody

object DocumentUploadUtils {

    fun validateFile(file: File): Boolean {
        val mimeType = getMimeType(file)
        return when {
            !file.exists() -> false
            file.length() > FileConstants.MAX_FILE_SIZE -> false
            mimeType == null -> false
            !FileConstants.ALLOWED_MIME_TYPES.contains(mimeType) -> false
            else -> true
        }
    }

    fun getMimeType(file: File): String? {
        val extension = file.extension
        return MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension.toLowerCase())
    }

    fun createMultipartBody(file: File): MultipartBody.Part? {
        if (!validateFile(file)) {
            return null
        }

        val mimeType = getMimeType(file) ?: return null
        val requestFile = file.asRequestBody(mimeType.toMediaTypeOrNull())

        return MultipartBody.Part.createFormData(
            "documents",
            file.name,
            requestFile
        )
    }

    fun getFileError(file: File): String? {
        return when {
            !file.exists() -> "File does not exist"
            file.length() > FileConstants.MAX_FILE_SIZE -> "File size exceeds 5MB limit"
            getMimeType(file) == null -> "Invalid file type"
            !FileConstants.ALLOWED_MIME_TYPES.contains(getMimeType(file)) ->
                "Invalid file type. Allowed types: PDF, JPEG, PNG"
            else -> null
        }
    }
}