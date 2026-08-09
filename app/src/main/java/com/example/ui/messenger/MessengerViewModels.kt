package com.example.ui.messenger

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.messenger.CURRENT_USER_ID
import com.example.data.messenger.Conversation
import com.example.data.messenger.Message
import com.example.data.messenger.MessengerRepository
import com.example.data.messenger.MessengerUser
import com.example.util.TimeFormat
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

/** A conversation plus everything the row needs to render, resolved once in the ViewModel. */
data class ConversationRowUi(
    val id: String,
    val title: String,
    val initials: String,
    val preview: String,
    val timeLabel: String,
    val unreadCount: Int,
    val isOnline: Boolean,
    val isGroup: Boolean,
)

class ConversationListViewModel(
    private val repository: MessengerRepository,
) : ViewModel() {

    val conversations: StateFlow<List<ConversationRowUi>> =
        repository.observeConversations()
            .map { list -> list.map { it.toRowUi() } }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val activeNow: StateFlow<List<MessengerUser>> =
        repository.observeConversations()
            .map { repository.allUsers().filter { user -> user.isOnline } }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private fun Conversation.toRowUi(): ConversationRowUi {
        val others = participantIds
            .filter { it != CURRENT_USER_ID }
            .mapNotNull { repository.user(it) }
        val primary = others.firstOrNull()
        return ConversationRowUi(
            id = id,
            title = groupTitle ?: primary?.name ?: "Unknown",
            initials = if (isGroup) {
                others.take(2).joinToString("") { it.initials.take(1) }
            } else {
                primary?.initials ?: "?"
            },
            preview = lastMessagePreview,
            timeLabel = TimeFormat.relative(lastMessageAt),
            unreadCount = unreadCount,
            isOnline = !isGroup && primary?.isOnline == true,
            isGroup = isGroup,
        )
    }
}

class ConversationListViewModelFactory(
    private val repository: MessengerRepository,
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return ConversationListViewModel(repository) as T
    }
}

// ---------------------------------------------------------------------- thread

/**
 * Day separators and bubble grouping are computed here, not in the composable. A LazyColumn item
 * cannot see its neighbours, so deciding "is this the last bubble of a run" during composition
 * means peeking at the list by index -- fragile and easy to get wrong once messages arrive live.
 */
sealed interface ThreadItem {
    val key: String

    data class Day(val label: String, override val key: String) : ThreadItem

    data class Bubble(
        val message: Message,
        val isMine: Boolean,
        val isFirstInGroup: Boolean,
        val isLastInGroup: Boolean,
        val senderInitials: String,
        val senderId: String,
        val showTimestamp: Boolean,
    ) : ThreadItem {
        override val key: String get() = message.id
    }
}

data class ThreadUiState(
    val items: List<ThreadItem> = emptyList(),
    val title: String = "",
    val subtitle: String = "",
    val initials: String = "",
    val isOnline: Boolean = false,
    val isTyping: Boolean = false,
    val showSeenReceipt: Boolean = false,
)

class ThreadViewModel(
    private val repository: MessengerRepository,
    private val conversationId: String,
) : ViewModel() {

    val state: StateFlow<ThreadUiState> = combine(
        repository.observeMessages(conversationId),
        repository.observeTyping(conversationId),
    ) { messages, isTyping ->
        val participants = repository.participantsOf(conversationId)
        val others = participants.filter { it.id != CURRENT_USER_ID }
        val primary = others.firstOrNull()
        val lastMine = messages.lastOrNull { it.senderId == CURRENT_USER_ID }

        ThreadUiState(
            items = buildThreadItems(messages, repository),
            title = primary?.name ?: "Conversation",
            subtitle = when {
                isTyping -> "typing…"
                primary?.isOnline == true -> "Active now"
                else -> primary?.affiliation.orEmpty()
            },
            initials = primary?.initials ?: "?",
            isOnline = primary?.isOnline == true,
            isTyping = isTyping,
            showSeenReceipt = lastMine?.status == com.example.data.messenger.DeliveryStatus.SEEN,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ThreadUiState())

    init {
        viewModelScope.launch { repository.markRead(conversationId) }
    }

    fun send(text: String) {
        viewModelScope.launch { repository.send(conversationId, text) }
    }

    fun react(messageId: String, emoji: String) {
        viewModelScope.launch { repository.react(conversationId, messageId, emoji) }
    }
}

class ThreadViewModelFactory(
    private val repository: MessengerRepository,
    private val conversationId: String,
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return ThreadViewModel(repository, conversationId) as T
    }
}

/** Messages from the same sender within this window collapse into one visual run. */
private val GROUP_WINDOW_MS = TimeUnit.MINUTES.toMillis(5)

private fun buildThreadItems(
    messages: List<Message>,
    repository: MessengerRepository,
): List<ThreadItem> = buildList {
    var lastDayKey: Long? = null

    messages.forEachIndexed { index, message ->
        val dayKey = TimeFormat.dayKey(message.sentAt)
        if (dayKey != lastDayKey) {
            add(ThreadItem.Day(TimeFormat.daySeparator(message.sentAt), "day-$dayKey"))
            lastDayKey = dayKey
        }

        val previous = messages.getOrNull(index - 1)
        val next = messages.getOrNull(index + 1)

        val startsRun = previous == null ||
            previous.senderId != message.senderId ||
            message.sentAt - previous.sentAt > GROUP_WINDOW_MS ||
            TimeFormat.dayKey(previous.sentAt) != dayKey

        val endsRun = next == null ||
            next.senderId != message.senderId ||
            next.sentAt - message.sentAt > GROUP_WINDOW_MS ||
            TimeFormat.dayKey(next.sentAt) != dayKey

        add(
            ThreadItem.Bubble(
                message = message,
                isMine = message.senderId == CURRENT_USER_ID,
                isFirstInGroup = startsRun,
                isLastInGroup = endsRun,
                senderInitials = repository.user(message.senderId)?.initials ?: "?",
                senderId = message.senderId,
                showTimestamp = endsRun,
            ),
        )
    }
}
