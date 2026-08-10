package com.example.data.lists

import com.example.ui.theme.AcademicField
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class ReadingListFolder(
    val id: String,
    val title: String,
    val description: String,
    val paperCount: Int,
    val isPrivate: Boolean,
    val field: AcademicField,
)

/**
 * Reading lists were a top-level `val sampleLists` read directly by the screen, which is why the
 * "New folder" button had nowhere to write and was left as a no-op.
 *
 * In-memory and application-scoped, matching the messenger, people and notification layers. Held
 * behind an interface for the same reason they are: the screen should not learn where its data
 * comes from, so a real store can replace this without touching the UI.
 */
interface ReadingListRepository {
    fun observeFolders(): Flow<List<ReadingListFolder>>
    fun createFolder(title: String, description: String, isPrivate: Boolean, field: AcademicField)
    fun deleteFolder(id: String)
}

class InMemoryReadingListRepository : ReadingListRepository {

    private val folders = MutableStateFlow(seedFolders())

    override fun observeFolders(): Flow<List<ReadingListFolder>> = folders.asStateFlow()

    override fun createFolder(
        title: String,
        description: String,
        isPrivate: Boolean,
        field: AcademicField,
    ) {
        val trimmed = title.trim()
        if (trimmed.isEmpty()) return
        folders.update { current ->
            // Newest first: a folder you just made should not appear at the bottom of a long list.
            listOf(
                ReadingListFolder(
                    id = "rl-${System.currentTimeMillis()}",
                    title = trimmed,
                    description = description.trim(),
                    paperCount = 0,
                    isPrivate = isPrivate,
                    field = field,
                ),
            ) + current
        }
    }

    override fun deleteFolder(id: String) {
        folders.update { current -> current.filterNot { it.id == id } }
    }
}

private fun seedFolders(): List<ReadingListFolder> = listOf(
    ReadingListFolder(
        id = "rl-1",
        title = "LLM Epistemology",
        description = "Papers covering the semantic shifts in large models.",
        paperCount = 12,
        isPrivate = true,
        field = AcademicField.LINGUISTICS,
    ),
    ReadingListFolder(
        id = "rl-2",
        title = "Thermodynamics in Archival Tech",
        description = "Foundational texts for my upcoming grant proposal.",
        paperCount = 4,
        isPrivate = false,
        field = AcademicField.PHYSICS,
    ),
    ReadingListFolder(
        id = "rl-3",
        title = "Decentralized Science",
        description = "DAO structures and reputation mechanics.",
        paperCount = 28,
        isPrivate = false,
        field = AcademicField.ECONOMICS,
    ),
)
