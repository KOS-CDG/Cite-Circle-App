package com.example

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.PaperRepository
import com.example.data.SavedPaper
import com.example.data.prefs.SettingsStore
import com.example.data.prefs.ThemeMode
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.launch

class HomeViewModel(
    private val repository: PaperRepository,
    private val settingsStore: SettingsStore,
) : ViewModel() {

    /**
     * SYSTEM / LIGHT / DARK, persisted via DataStore. The composable layer resolves SYSTEM into an
     * actual boolean, since that needs isSystemInDarkTheme().
     */
    val themeMode: StateFlow<ThemeMode> = settingsStore.themeMode
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = ThemeMode.SYSTEM,
        )

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { settingsStore.setThemeMode(mode) }
    }

    val savedPapers: StateFlow<List<SavedPaper>> = repository.allPapers
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    init {
        viewModelScope.launch {
            repository.allPapers.take(1).collect { papers ->
                if (papers.isEmpty()) {
                    val defaultPaper = SavedPaper(
                        id = "1",
                        authorInitials = "JD",
                        authorName = "Dr. Jane Doe",
                        timeAgo = "2h ago",
                        affiliation = "AFFILIATION: OXFORD",
                        content = "I just published a new preprint analyzing the semantic structures of large language models. The findings suggest a stark shift in latent knowledge representations.",
                        citation = "Doe, J. (2026). Semantic Structures in LLMs. Folio Preprints, CC-882-XJ. https://cite.circle/refs/882xj",
                        isEndorsed = false
                    )
                    repository.savePaper(defaultPaper)
                }
            }
        }
    }

    private val firestoreRepo = com.example.data.FirestoreRepository()

    fun savePaper(paper: SavedPaper) {
        viewModelScope.launch {
            repository.savePaper(paper)
            // Sync all papers to cloud (in a real app this might be more targeted)
            repository.allPapers.take(1).collect { papers ->
                firestoreRepo.syncPapersToCloud(papers + paper)
            }
        }
    }

    fun removePaper(id: String) {
        viewModelScope.launch {
            repository.deletePaper(id)
        }
    }
    
    fun toggleEndorsement(id: String, currentStatus: Boolean) {
        viewModelScope.launch {
            repository.toggleEndorsement(id, currentStatus)
        }
    }
}

class HomeViewModelFactory(
    private val repository: PaperRepository,
    private val settingsStore: SettingsStore,
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(HomeViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return HomeViewModel(repository, settingsStore) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
