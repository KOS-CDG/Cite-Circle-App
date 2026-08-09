package com.example.data.people

import com.example.data.messenger.CURRENT_USER_ID
import com.example.data.messenger.MessengerRepository
import com.example.data.messenger.MessengerUser
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

enum class ConnectionState { NONE, PENDING, CONNECTED }

/**
 * A researcher as the People screens need them: the messenger's user record plus the bits that
 * only matter for discovery.
 */
data class Person(
    val user: MessengerUser,
    val connectionState: ConnectionState,
    val mutualConnections: Int,
    val headline: String,
    val publications: Int,
    val citations: Int,
    val hIndex: Int,
    /** Citations per year, oldest first. Drives the profile chart. */
    val citationSeries: List<Float>,
)

interface PeopleRepository {
    fun observePeople(): Flow<List<Person>>
    fun observeSuggested(): Flow<List<Person>>
    /** Reactive, so a profile reflects a connect/disconnect without being reopened. */
    fun observePerson(userId: String): Flow<Person?>
    fun person(userId: String): Person?
    fun connect(userId: String)
    fun disconnect(userId: String)
}

/**
 * In-memory, like the messenger. Deliberately reuses MessengerRepository as the source of user
 * records rather than seeding a second, divergent copy of the same people -- otherwise the name
 * on a profile could disagree with the name on a conversation.
 */
class InMemoryPeopleRepository(
    private val messenger: MessengerRepository,
) : PeopleRepository {

    private val connections = MutableStateFlow(
        mapOf(
            "u-thorne" to ConnectionState.CONNECTED,
            "u-okafor" to ConnectionState.CONNECTED,
            "u-navarro" to ConnectionState.PENDING,
        ),
    )

    private fun buildPerson(user: MessengerUser, state: ConnectionState): Person {
        val profile = profiles[user.id] ?: defaultProfile
        return Person(
            user = user,
            connectionState = state,
            mutualConnections = profile.mutuals,
            headline = profile.headline,
            publications = profile.publications,
            citations = profile.citations,
            hIndex = profile.hIndex,
            citationSeries = profile.series,
        )
    }

    override fun observePeople(): Flow<List<Person>> = connections.map { states ->
        messenger.allUsers().map { user ->
            buildPerson(user, states[user.id] ?: ConnectionState.NONE)
        }
    }

    override fun observeSuggested(): Flow<List<Person>> =
        observePeople().map { people ->
            people.filter { it.connectionState == ConnectionState.NONE }
        }

    override fun observePerson(userId: String): Flow<Person?> = connections.map { states ->
        messenger.user(userId)?.let { user ->
            buildPerson(user, states[userId] ?: ConnectionState.NONE)
        }
    }

    override fun person(userId: String): Person? {
        val user = messenger.user(userId) ?: return null
        return buildPerson(user, connections.value[userId] ?: ConnectionState.NONE)
    }

    override fun connect(userId: String) {
        if (userId == CURRENT_USER_ID) return
        connections.update { it + (userId to ConnectionState.PENDING) }
    }

    override fun disconnect(userId: String) {
        connections.update { it - userId }
    }

    private data class ProfileFacts(
        val headline: String,
        val mutuals: Int,
        val publications: Int,
        val citations: Int,
        val hIndex: Int,
        val series: List<Float>,
    )

    private companion object {
        val defaultProfile = ProfileFacts(
            "Researcher", 0, 0, 0, 0, listOf(0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f),
        )

        val profiles = mapOf(
            // The signed-in user. ProfileScreen used to hardcode these values inline.
            CURRENT_USER_ID to ProfileFacts(
                "Semantic structures in large language models", 0, 34, 1820, 24,
                listOf(10f, 15f, 30f, 25f, 40f, 60f, 85f, 110f),
            ),
            "u-thorne" to ProfileFacts(
                "Entropy and distributed knowledge systems", 12, 48, 2140, 24,
                listOf(18f, 26f, 41f, 58f, 74f, 96f, 121f, 148f),
            ),
            "u-okafor" to ProfileFacts(
                "Expression variance in regulatory pathways", 8, 31, 1580, 19,
                listOf(8f, 19f, 27f, 44f, 61f, 78f, 99f, 118f),
            ),
            "u-lindqvist" to ProfileFacts(
                "Archival practice in early modern Scandinavia", 3, 22, 410, 11,
                listOf(4f, 7f, 11f, 16f, 22f, 28f, 33f, 41f),
            ),
            "u-navarro" to ProfileFacts(
                "Attention and working memory under load", 15, 27, 990, 16,
                listOf(12f, 21f, 29f, 38f, 52f, 63f, 79f, 94f),
            ),
            "u-tanaka" to ProfileFacts(
                "Elasticity models in post-scarcity economies", 5, 19, 620, 13,
                listOf(6f, 12f, 18f, 25f, 34f, 43f, 51f, 62f),
            ),
            "u-mbeki" to ProfileFacts(
                "Population genomics of arid-zone flora", 2, 24, 780, 14,
                listOf(9f, 14f, 22f, 31f, 39f, 50f, 64f, 77f),
            ),
        )
    }
}
