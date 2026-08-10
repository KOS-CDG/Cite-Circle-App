package com.example.data

import com.google.firebase.auth.FirebaseAuth

/** Who a new post is attributed to. */
data class AuthorIdentity(
    val name: String,
    val initials: String,
    val affiliation: String
) {
    companion object {
        private val FALLBACK = AuthorIdentity(
            name = "Unattributed Researcher",
            initials = "??",
            affiliation = "AFFILIATION: UNSPECIFIED"
        )

        /**
         * Reads the signed-in user, falling back to a placeholder.
         *
         * Wrapped in a catch because `google-services.json` ships as a placeholder in this
         * repository: with no real Firebase project, touching FirebaseAuth can throw, and a
         * missing display name must not stop someone from drafting a post.
         */
        fun current(): AuthorIdentity = try {
            val user = FirebaseAuth.getInstance().currentUser
            val name = user?.displayName?.takeIf { it.isNotBlank() }
                ?: user?.email?.substringBefore('@')?.takeIf { it.isNotBlank() }
            if (name == null) FALLBACK else AuthorIdentity(
                name = name,
                initials = initialsOf(name),
                affiliation = FALLBACK.affiliation
            )
        } catch (e: Exception) {
            FALLBACK
        }

        /** "Jane Doe" -> "JD"; a single name yields its first two letters. */
        fun initialsOf(name: String): String {
            val words = name.trim().split(' ', '.').filter { it.isNotBlank() }
            return when {
                words.isEmpty() -> "??"
                words.size == 1 -> words[0].take(2).uppercase()
                else -> "${words.first().first()}${words.last().first()}".uppercase()
            }
        }
    }
}
