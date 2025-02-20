package com.second_year.hkroadmap.Api.Interfaces

import com.google.gson.GsonBuilder
import com.google.gson.JsonDeserializer
import com.second_year.hkroadmap.Api.Models.LoginResponse
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitInstance {
    private const val BASE_URL = "http://192.168.0.12:8000/hk-roadmap/"

    private val logging = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(100, TimeUnit.SECONDS)
        .readTimeout(100, TimeUnit.SECONDS)
        .writeTimeout(100, TimeUnit.SECONDS)
        .addInterceptor(logging)
        .build()

    private val gson = GsonBuilder()
        .setLenient()
        .registerTypeAdapter(LoginResponse::class.java, JsonDeserializer { json, _, _ ->
            try {
                if (json.isJsonPrimitive) {
                    // Handle string response (usually error messages)
                    LoginResponse(message = json.asString, token = null)
                } else {
                    // Handle object response (successful login)
                    val obj = json.asJsonObject
                    LoginResponse(
                        message = obj.get("message")?.asString ?: "",
                        token = obj.get("token")?.asString
                    )
                }
            } catch (e: Exception) {
                // Fallback for unexpected response format
                LoginResponse(
                    message = "Error parsing response",
                    token = null
                )
            }
        })
        .create()

    private val retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(httpClient)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
    }

    fun createApiService(): ApiService {
        return retrofit.create(ApiService::class.java)
    }
}