package com.second_year.hkroadmap.Utils

import android.content.Context
import android.net.Uri
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

object FileUtils {
    @Throws(IOException::class)
    fun getFileFromUri(context: Context, uri: Uri): File? {
        val fileName = getFileName(context, uri) ?: return null
        val tempFile = File(context.cacheDir, fileName)

        try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                FileOutputStream(tempFile).use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
            }
            return tempFile
        } catch (e: IOException) {
            e.printStackTrace()
            return null
        }
    }

    private fun getFileName(context: Context, uri: Uri): String? {
        val cursor = context.contentResolver.query(uri, null, null, null, null)
        return cursor?.use {
            val nameIndex = it.getColumnIndex("_display_name")
            if (nameIndex >= 0 && it.moveToFirst()) {
                return@use it.getString(nameIndex)
            }
            "file_${System.currentTimeMillis()}"
        }
    }
}