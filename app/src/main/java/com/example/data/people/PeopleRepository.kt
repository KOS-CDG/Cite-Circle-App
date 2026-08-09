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
)

interface PeopleRepository {
    fun observePeople(): Flow<List<Person>>
    fun observeSuggested(): Flow<List<Person>>
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
    )

    private companion object {
        val defaultProfile = ProfileFacts("Researcher", 0, 0, 0, 0)

        val profiles = mapOf(
            "u-thorne" to ProfileFacts(
                "Entropy and distributed knowledge systems", 12, 48, 2140, 24,
            ),
            "u-okafor" to ProfileFacts(
                "Expression variance in regulatory pathways", 8, 31, 1580, 19,
            ),
            "u-lindqvist" to ProfileFacts(
                "Archival practice in early modern Scandinavia", 3, 22, 410, 11,
            ),
            "u-navarro" to ProfileFacts(
                "Attention and working memory under load", 15, 27, 990, 16,
            ),
            "u-tanaka" to ProfileFacts(
                "Elasticity models in post-scarcity economies", 5, 19, 620, 13,
            ),
            "u-mbeki" to ProfileFacts(
                "Population genomics of arid-zone flora", 2, 24, 780, 14,
            ),
        )
    }
}
