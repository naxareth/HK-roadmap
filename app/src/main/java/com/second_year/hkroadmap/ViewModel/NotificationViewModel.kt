package com.second_year.hkroadmap.ViewModel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.second_year.hkroadmap.Api.Models.MarkReadResponse
import com.second_year.hkroadmap.Api.Models.NotificationResponse
import com.second_year.hkroadmap.Api.Models.UnreadCountResponse
import com.second_year.hkroadmap.Repository.NotificationRepository
import kotlinx.coroutines.launch

class NotificationViewModel(
    private val repository: NotificationRepository,
    private val token: String
) : ViewModel() {

    private val _notifications = MutableLiveData<List<NotificationResponse>>()
    val notifications: LiveData<List<NotificationResponse>> = _notifications

    private val _unreadCount = MutableLiveData<UnreadCountResponse>()
    val unreadCount: LiveData<UnreadCountResponse> = _unreadCount

    private val _markReadStatus = MutableLiveData<MarkReadResponse>()
    val markReadStatus: LiveData<MarkReadResponse> = _markReadStatus

    private val _error = MutableLiveData<String>()
    val error: LiveData<String> = _error

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    init {
        fetchNotifications()
        fetchUnreadCount()
    }

    fun fetchNotifications() {
        viewModelScope.launch {
            _isLoading.value = true
            repository.getStudentNotifications(token).fold(
                onSuccess = {
                    _notifications.value = it
                    _isLoading.value = false
                },
                onFailure = {
                    _error.value = it.message ?: "Failed to fetch notifications"
                    _isLoading.value = false
                }
            )
        }
    }

    fun fetchUnreadCount() {
        viewModelScope.launch {
            repository.getUnreadCount(token).fold(
                onSuccess = { _unreadCount.value = it },
                onFailure = { _error.value = it.message ?: "Failed to fetch unread count" }
            )
        }
    }

    fun toggleNotificationRead(notificationId: Int, read: Boolean) {
        viewModelScope.launch {
            _isLoading.value = true
            repository.toggleNotificationRead(token, notificationId, read).fold(
                onSuccess = {
                    fetchNotifications()
                    fetchUnreadCount()
                },
                onFailure = {
                    _error.value = it.message ?: "Failed to update notification"
                    _isLoading.value = false
                }
            )
        }
    }

    fun markAllAsRead() {
        viewModelScope.launch {
            _isLoading.value = true
            repository.markAllAsRead(token).fold(
                onSuccess = {
                    _markReadStatus.value = it
                    fetchNotifications()
                    fetchUnreadCount()
                },
                onFailure = {
                    _error.value = it.message ?: "Failed to mark all as read"
                    _isLoading.value = false
                }
            )
        }
    }
}