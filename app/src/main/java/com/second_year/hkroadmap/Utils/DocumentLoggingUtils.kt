package com.second_year.hkroadmap.Utils

import android.util.Log
import com.second_year.hkroadmap.Api.Models.DocumentResponse
import com.second_year.hkroadmap.Api.Models.DocumentStatusResponse
import retrofit2.Response

object DocumentLoggingUtils {
    private const val TAG = "DocumentOperation"
    private const val MAX_LOG_LENGTH = 4000

    fun logRequest(methodName: String, documentId: Int?, token: String?) {
        val log = """
            $methodName Request:
            - Document ID: $documentId
            - Token Present: ${!token.isNullOrEmpty()}
            - Token Length: ${token?.length ?: 0}
            - Request Time: ${System.currentTimeMillis()}
        """.trimIndent()

        logLongString(TAG, log)
    }

    fun logResponse(methodName: String, response: Response<*>) {
        val log = """
            $methodName Response:
            - Status Code: ${response.code()}
            - Is Successful: ${response.isSuccessful}
            - Headers: ${response.headers()}
            - Raw Response: ${response.raw()}
            - Error Body: ${response.errorBody()?.string()}
            - Response Time: ${System.currentTimeMillis()}
        """.trimIndent()

        logLongString(TAG, log)
    }

    fun logError(methodName: String, e: Exception) {
        val log = """
            $methodName Error:
            - Error Type: ${e.javaClass.simpleName}
            - Message: ${e.message}
            - Stack Trace: ${e.stackTraceToString()}
            - Error Time: ${System.currentTimeMillis()}
        """.trimIndent()

        logLongString(TAG, "Error", log)
    }

    fun logDocumentStatus(document: DocumentResponse) {
        val log = """
            Document Status:
            - Document ID: ${document.document_id}
            - Current Status: ${document.status}
            - Is Submitted: ${document.is_submitted}
            - File Path: ${document.file_path}
            - Event ID: ${document.event_id}
            - Requirement ID: ${document.requirement_id}
            - Upload Time: ${document.upload_at}
            - Submit Time: ${document.submitted_at}
            - Check Time: ${System.currentTimeMillis()}
        """.trimIndent()

        logLongString(TAG, log)
    }

    private fun logLongString(tag: String, message: String) {
        logLongString(tag, "Debug", message)
    }

    private fun logLongString(tag: String, level: String, message: String) {
        message.chunked(MAX_LOG_LENGTH).forEach { chunk ->
            when (level.lowercase()) {
                "error" -> Log.e(tag, chunk)
                "warning" -> Log.w(tag, chunk)
                else -> Log.d(tag, chunk)
            }
        }
    }
}