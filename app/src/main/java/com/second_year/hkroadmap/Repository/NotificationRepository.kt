package com.second_year.hkroadmap.Repository

import com.second_year.hkroadmap.Api.Interfaces.ApiService
import com.second_year.hkroadmap.Api.Models.MarkReadResponse
import com.second_year.hkroadmap.Api.Models.NotificationResponse
import com.second_year.hkroadmap.Api.Models.NotificationToggleRequest
import com.second_year.hkroadmap.Api.Models.UnreadCountResponse

class NotificationRepository(private val apiService: ApiService) {

    suspend fun getStudentNotifications(token: String): Result<List<NotificationResponse>> {
        return try {
            val response = apiService.getStudentNotifications("Bearer $token")
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Failed to fetch notifications"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getUnreadCount(token: String): Result<UnreadCountResponse> {
        return try {
            val response = apiService.getStudentUnreadCount("Bearer $token")
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Failed to fetch unread count"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun toggleNotificationRead(
        token: String,
        notificationId: Int,
        read: Boolean
    ): Result<NotificationResponse> {
        return try {
            val response = apiService.toggleStudentNotification(
                "Bearer $token",
                NotificationToggleRequest(notificationId, read)
            )
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Failed to update notification"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun markAllAsRead(token: String): Result<MarkReadResponse> {
        return try {
            val response = apiService.markAllStudentRead("Bearer $token")
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Failed to mark all as read"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}