package com.example.ui.profile

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.google.firebase.auth.FirebaseAuth

/** The signed-in researcher as the profile screens need them. */
data class ResearcherIdentity(
    val name: String,
    val initials: String,
    val affiliation: String,
    val email: String?,
    val isSignedIn: Boolean,
)

/** Stand-in shown when Firebase is unconfigured or the demo bypass was used to get in. */
val demoIdentity = ResearcherIdentity(
    name = "Dr. Jane Doe",
    initials = "JD",
    affiliation = "Senior Researcher • Oxford",
    email = null,
    isSignedIn = false,
)

/**
 * Reads the current Firebase user, falling back to [demoIdentity].
 *
 * `google-services.json` is a placeholder in this project and `AuthScreen` lets users in
 * without a real sign-in, so the fallback is the common path rather than an edge case. The
 * lookup is wrapped because `FirebaseAuth.getInstance()` throws when Firebase was never
 * initialised — which is exactly the situation in Robolectric tests and Compose previews.
 */
@Composable
fun rememberResearcherIdentity(): ResearcherIdentity = remember {
    runCatching {
        val user = FirebaseAuth.getInstance().currentUser ?: return@runCatching demoIdentity
        val name = user.displayName?.takeIf { it.isNotBlank() }
            ?: user.email?.substringBefore('@')
            ?: demoIdentity.name
        ResearcherIdentity(
            name = name,
            initials = initialsOf(name),
            affiliation = demoIdentity.affiliation,
            email = user.email,
            isSignedIn = true,
        )
    }.getOrDefault(demoIdentity)
}

/** "Dr. Jane Doe" -> "JD"; falls back to the first two characters for single-word names. */
fun initialsOf(name: String): String {
    val words = name
        .split(' ', '.', '-')
        .filter { it.isNotBlank() && it.first().isLetter() }
        .filterNot { it.equals("dr", ignoreCase = true) || it.equals("prof", ignoreCase = true) }

    return when {
        words.size >= 2 -> "${words[0].first()}${words[1].first()}".uppercase()
        words.size == 1 -> words[0].take(2).uppercase()
        else -> "??"
    }
}
