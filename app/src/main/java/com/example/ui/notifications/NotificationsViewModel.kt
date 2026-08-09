package com.example.ui.notifications

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.notifications.AppNotification
import com.example.data.notifications.NotificationRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class NotificationsViewModel(
    private val repository: NotificationRepository,
) : ViewModel() {

    val notifications: StateFlow<List<AppNotification>> = repository.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val unreadCount: StateFlow<Int> = repository.observeUnreadCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    fun byId(id: String): AppNotification? = repository.byId(id)

    fun markRead(id: String) = repository.markRead(id)

    fun markAllRead() = repository.markAllRead()
}

class NotificationsViewModelFactory(
    private val repository: NotificationRepository,
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return NotificationsViewModel(repository) as T
    }
}
