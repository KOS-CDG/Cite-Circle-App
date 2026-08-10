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

        /**
         * Honorifics and post-nominals, which are not part of a name. Without this an
         * academic display name like "Dr. Jane Doe" would initialise to "DD".
         */
        private val NON_NAME_PARTS = setOf(
            "dr", "prof", "professor", "mr", "mrs", "ms", "mx", "sir", "dame", "rev",
            "phd", "md", "dphil", "jr", "sr", "ii", "iii", "iv"
        )

        /** "Dr. Jane Doe" -> "JD"; a single name yields its first two letters. */
        fun initialsOf(name: String): String {
            val words = name.trim()
                .split(' ', '.', ',')
                .filter { it.isNotBlank() }
                .filterNot { it.lowercase() in NON_NAME_PARTS }
            return when {
                words.isEmpty() -> "??"
                words.size == 1 -> words[0].take(2).uppercase()
                else -> "${words.first().first()}${words.last().first()}".uppercase()
            }
        }
    }
}
