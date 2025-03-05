package com.second_year.hkroadmap.Api.Interfaces

import android.util.Log
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import okhttp3.JavaNetCookieJar
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.net.CookieManager
import java.net.CookiePolicy
import java.util.concurrent.TimeUnit

object RetrofitInstance {
    private const val BASE_URL = "http://192.168.0.12:8000/hk-roadmap/"  // Keep original base URL for API
    private const val UPLOADS_URL = "http://192.168.0.12:8000/uploads/"   // Add separate URL for uploads
    private const val TAG = "RetrofitInstance"
    private const val TIMEOUT_SECONDS = 30L

    private val cookieManager = CookieManager().apply {
        setCookiePolicy(CookiePolicy.ACCEPT_ALL)
    }

    private val logging = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val networkInterceptor = HttpLoggingInterceptor { message ->
        Log.d(TAG, "Network: $message")
    }.apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val httpClient = OkHttpClient.Builder().apply {
        connectTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
        readTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
        writeTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
        cookieJar(JavaNetCookieJar(cookieManager))
        addInterceptor(logging)
        addInterceptor(networkInterceptor)
        addInterceptor { chain ->
            val request = chain.request()
            Log.d(TAG, """
                Request Details:
                URL: ${request.url}
                Method: ${request.method}
                Headers: ${request.headers}
            """.trimIndent())

            try {
                val response = chain.proceed(request)
                when (response.code) {
                    in 200..299 -> Log.d(TAG, "Successful response: ${response.code}")
                    in 400..499 -> Log.w(TAG, "Client error: ${response.code}")
                    in 500..599 -> Log.e(TAG, "Server error: ${response.code}")
                    else -> Log.w(TAG, "Unexpected response code: ${response.code}")
                }
                response.body?.let {
                    Log.d(TAG, "Response body available")
                } ?: Log.w(TAG, "Empty response body")
                response
            } catch (e: Exception) {
                Log.e(TAG, "Network error: ${e.message}", e)
                throw e
            }
        }
    }.build()

    private val retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(httpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    fun createApiService(): ApiService {
        return retrofit.create(ApiService::class.java)
    }

    // Add helper method to get uploads URL
    fun getUploadsUrl(fileName: String): String {
        return UPLOADS_URL + fileName
    }
}