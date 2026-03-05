package com.intimocoffee.waiter.feature.notifications.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.intimocoffee.waiter.feature.notifications.domain.model.Notification
import com.intimocoffee.waiter.feature.notifications.domain.service.NotificationService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class NotificationViewModel @Inject constructor(
    private val notificationService: NotificationService
) : ViewModel() {

    private val _notifications = MutableStateFlow<List<Notification>>(emptyList())
    val notifications: StateFlow<List<Notification>> = _notifications.asStateFlow()

    private val _unreadCount = MutableStateFlow(0)
    val unreadCount: StateFlow<Int> = _unreadCount.asStateFlow()

    init {
        loadNotifications()
    }

    private fun loadNotifications() {
        viewModelScope.launch {
            notificationService.getNotificationsFlow()
                .catch { /* Handle error */ }
                .collect { notificationList ->
                    _notifications.value = notificationList.sortedByDescending { it.createdAt }
                    _unreadCount.value = notificationList.count { !it.isRead }
                }
        }
    }

    fun markAsRead(notificationId: Long) {
        viewModelScope.launch {
            notificationService.markAsRead(notificationId)
        }
    }

    fun markAllAsRead() {
        viewModelScope.launch {
            notificationService.markAllAsRead()
        }
    }

    fun clearOldNotifications() {
        viewModelScope.launch {
            notificationService.deleteOldNotifications(24) // Delete notifications older than 24 hours
        }
    }
}