package com.example.ui.people

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.messenger.MessengerRepository
import com.example.data.people.PeopleRepository
import com.example.data.people.Person
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class PeopleViewModel(
    private val repository: PeopleRepository,
    private val messenger: MessengerRepository,
) : ViewModel() {

    val people: StateFlow<List<Person>> = repository.observePeople()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val suggested: StateFlow<List<Person>> = repository.observeSuggested()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun connect(userId: String) = repository.connect(userId)

    fun disconnect(userId: String) = repository.disconnect(userId)

    /**
     * Opens (or creates) the conversation with this person and hands back its id, so People can
     * deep-link straight into the thread. This is what ties the connections layer to the
     * messenger rather than leaving them as two unrelated features.
     */
    fun openConversation(userId: String, onReady: (String) -> Unit) {
        viewModelScope.launch {
            onReady(messenger.startConversation(listOf(userId)))
        }
    }
}

class PeopleViewModelFactory(
    private val repository: PeopleRepository,
    private val messenger: MessengerRepository,
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return PeopleViewModel(repository, messenger) as T
    }
}
