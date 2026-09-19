package com.example.data

import android.content.Context
import com.example.R
import com.google.firebase.auth.FirebaseAuth

/** Who a new post is attributed to. */
data class AuthorIdentity(
    val name: String,
    val initials: String,
    val affiliation: String
) {
    companion object {
        /** Placeholder for a signed-out user. Takes a Context so the copy can be translated. */
        private fun fallback(context: Context) = AuthorIdentity(
            name = context.getString(R.string.identity_unattributed),
            initials = UNKNOWN_INITIALS,
            affiliation = context.getString(R.string.identity_affiliation_unspecified)
        )

        private const val UNKNOWN_INITIALS = "??"

        /**
         * Reads the signed-in user, falling back to a placeholder.
         *
         * Wrapped in a catch because `google-services.json` ships as a placeholder in this
         * repository: with no real Firebase project, touching FirebaseAuth can throw, and a
         * missing display name must not stop someone from drafting a post.
         */
        fun current(context: Context): AuthorIdentity = try {
            val app = context.applicationContext as? com.example.MyApplication
            val localUser = app?.database?.userAccountDao()?.let { dao ->
                kotlinx.coroutines.runBlocking { dao.getActiveUserOnce() }
            }
            if (localUser != null && localUser.displayName.isNotBlank()) {
                AuthorIdentity(
                    name = localUser.displayName,
                    initials = initialsOf(localUser.displayName),
                    affiliation = localUser.affiliation.ifBlank { fallback(context).affiliation }
                )
            } else {
                val user = FirebaseAuth.getInstance().currentUser
                val name = user?.displayName?.takeIf { it.isNotBlank() }
                    ?: user?.email?.substringBefore('@')?.takeIf { it.isNotBlank() }
                if (name == null) fallback(context) else AuthorIdentity(
                    name = name,
                    initials = initialsOf(name),
                    affiliation = fallback(context).affiliation
                )
            }
        } catch (e: Exception) {
            fallback(context)
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
                words.isEmpty() -> UNKNOWN_INITIALS
                words.size == 1 -> words[0].take(2).uppercase()
                else -> "${words.first().first()}${words.last().first()}".uppercase()
            }
        }
    }
}
