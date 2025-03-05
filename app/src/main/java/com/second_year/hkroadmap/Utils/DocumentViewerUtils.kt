package com.second_year.hkroadmap.Utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.webkit.MimeTypeMap
import androidx.core.content.FileProvider
import com.second_year.hkroadmap.Api.Models.DocumentResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

object DocumentViewerUtils {
    private const val TAG = "DocumentViewerUtils"

    suspend fun createViewIntent(context: Context, document: DocumentResponse): Result<Intent> {
        return withContext(Dispatchers.IO) {
            try {
                when (document.document_type) {
                    "link" -> createLinkViewIntent(document.link_url)
                    else -> {
                        // Get local file (downloads if needed)
                        DocumentDownloadUtils.getLocalFile(context, document).fold(
                            onSuccess = { file -> createFileViewIntent(context, file) },
                            onFailure = { Result.failure(it) }
                        )
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error creating view intent", e)
                Result.failure(e)
            }
        }
    }

    private fun createLinkViewIntent(linkUrl: String?): Result<Intent> {
        if (linkUrl.isNullOrEmpty()) {
            return Result.failure(Exception("Invalid link URL"))
        }
        return Result.success(Intent(Intent.ACTION_VIEW, Uri.parse(linkUrl)))
    }

    private fun createFileViewIntent(context: Context, file: File): Result<Intent> {
        if (!file.exists()) {
            return Result.failure(Exception("File not found"))
        }

        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.provider",
            file
        )

        val mimeType = getMimeType(file)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, mimeType)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        return Result.success(intent)
    }

    private fun getMimeType(file: File): String {
        val extension = file.extension.lowercase()
        return MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension) ?: when (extension) {
            "pdf" -> "application/pdf"
            "jpg", "jpeg" -> "image/jpeg"
            "png" -> "image/png"
            else -> "*/*"
        }
    }
}