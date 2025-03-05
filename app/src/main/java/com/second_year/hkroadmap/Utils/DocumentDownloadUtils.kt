package com.second_year.hkroadmap.Utils

import android.content.Context
import android.util.Log
import com.second_year.hkroadmap.Api.Interfaces.RetrofitInstance
import com.second_year.hkroadmap.Api.Models.DocumentResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.IOException

object DocumentDownloadUtils {
    private const val TAG = "DocumentDownloadUtils"
    private const val CACHE_DIR = "document_cache"

    private val client = OkHttpClient()

    suspend fun getLocalFile(context: Context, document: DocumentResponse): Result<File> {
        return try {
            if (document.document_type == "link") {
                return Result.failure(Exception("Links don't need to be downloaded"))
            }

            val cachedFile = getCachedFile(context, document.document_id)
            if (cachedFile.exists()) {
                Log.d(TAG, "Using cached file: ${cachedFile.path}")
                return Result.success(cachedFile)
            }

            // Remove duplicate 'uploads/' from file_path if it exists
            val cleanFilePath = document.file_path.removePrefix("uploads/")
            // Use RetrofitInstance's helper method to get the correct URL
            val fileUrl = "http://192.168.0.12:8000/uploads/${cleanFilePath}"

            Log.d(TAG, "Downloading file from: $fileUrl")
            downloadFile(fileUrl, cachedFile)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting local file", e)
            Result.failure(e)
        }
    }

    private fun getCachedFile(context: Context, documentId: Int): File {
        val cacheDir = File(context.cacheDir, CACHE_DIR).apply { mkdirs() }
        return File(cacheDir, "document_$documentId")
    }

    private suspend fun downloadFile(url: String, destination: File): Result<File> {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Downloading file from: $url")

                val request = Request.Builder()
                    .url(url)
                    .build()

                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        throw IOException("Failed to download file: ${response.code}")
                    }

                    response.body?.let { body ->
                        destination.outputStream().use { fileOut ->
                            body.byteStream().use { bodyIn ->
                                bodyIn.copyTo(fileOut)
                            }
                        }
                        Log.d(TAG, "File downloaded successfully to: ${destination.path}")
                        Result.success(destination)
                    } ?: Result.failure(IOException("Empty response body"))
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error downloading file", e)
                Result.failure(e)
            }
        }
    }

    fun clearCache(context: Context) {
        val cacheDir = File(context.cacheDir, CACHE_DIR)
        if (cacheDir.exists()) {
            cacheDir.deleteRecursively()
        }
    }
}