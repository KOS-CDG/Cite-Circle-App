package com.example.data.auth

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.data.AppDatabase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.io.IOException
import java.security.MessageDigest
import java.util.UUID

private val Context.sessionDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "user_session"
)

sealed class AuthResult {
    data class Success(val user: UserAccount) : AuthResult()
    data class Error(val message: String) : AuthResult()
    data class RateLimited(val waitSeconds: Int) : AuthResult()
}

/**
 * Manages user authentication, session persistence, security rate limiting,
 * and privacy consent state across the application.
 */
class UserSessionManager(
    private val context: Context,
    private val database: AppDatabase
) {

    private val store = context.applicationContext.sessionDataStore
    private val userDao = database.userAccountDao()

    private val keyIsLoggedIn = booleanPreferencesKey("is_logged_in")
    private val keyUserEmail = stringPreferencesKey("user_email")
    private val keyUserName = stringPreferencesKey("user_name")
    private val keyUserAffiliation = stringPreferencesKey("user_affiliation")
    private val keyUserField = stringPreferencesKey("user_field")
    private val keyPrivacyAccepted = booleanPreferencesKey("privacy_policy_accepted")

    private val preferences: Flow<Preferences> = store.data.catch { cause ->
        if (cause is IOException) emit(emptyPreferences()) else throw cause
    }

    val isLoggedIn: Flow<Boolean> = preferences.map { it[keyIsLoggedIn] ?: false }
    val currentUserEmail: Flow<String> = preferences.map { it[keyUserEmail].orEmpty() }
    val currentUserName: Flow<String> = preferences.map { it[keyUserName].orEmpty() }
    val currentUserAffiliation: Flow<String> = preferences.map { it[keyUserAffiliation].orEmpty() }
    val currentUserField: Flow<String> = preferences.map { it[keyUserField].orEmpty() }
    val isPrivacyAccepted: Flow<Boolean> = preferences.map { it[keyPrivacyAccepted] ?: false }

    companion object {
        private const val MAX_LOGIN_ATTEMPTS = 5
        private const val COOLDOWN_DURATION_MS = 60_000L

        private val failedAttempts = mutableListOf<Long>()

        @Synchronized
        fun checkLoginRateLimit(): Boolean {
            val now = System.currentTimeMillis()
            failedAttempts.removeAll { now - it > COOLDOWN_DURATION_MS }
            return failedAttempts.size < MAX_LOGIN_ATTEMPTS
        }

        @Synchronized
        fun recordFailedAttempt() {
            failedAttempts.add(System.currentTimeMillis())
        }

        @Synchronized
        fun resetFailedAttempts() {
            failedAttempts.clear()
        }

        @Synchronized
        fun getRemainingAttempts(): Int {
            val now = System.currentTimeMillis()
            failedAttempts.removeAll { now - it > COOLDOWN_DURATION_MS }
            return (MAX_LOGIN_ATTEMPTS - failedAttempts.size).coerceAtLeast(0)
        }

        @Synchronized
        fun getCooldownRemainingSeconds(): Int {
            if (failedAttempts.isEmpty()) return 0
            val oldest = failedAttempts.first()
            val elapsed = System.currentTimeMillis() - oldest
            return ((COOLDOWN_DURATION_MS - elapsed) / 1000).toInt().coerceAtLeast(1)
        }

        fun hashPassword(password: String): String {
            val bytes = MessageDigest.getInstance("SHA-256")
                .digest("cite_circle_salt_$password".toByteArray(Charsets.UTF_8))
            return bytes.joinToString("") { "%02x".format(it) }
        }
    }

    suspend fun login(email: String, password: String): AuthResult {
        val cleanEmail = email.trim().lowercase()
        val cleanPass = password.trim()

        if (cleanEmail.isBlank() || cleanPass.isBlank()) {
            return AuthResult.Error("Please enter your academic email and password.")
        }

        if (!checkLoginRateLimit()) {
            return AuthResult.RateLimited(getCooldownRemainingSeconds())
        }

        val existing = userDao.findUserByEmail(cleanEmail)
        if (existing == null) {
            recordFailedAttempt()
            val remaining = getRemainingAttempts()
            return AuthResult.Error(
                if (remaining > 0) "Account not found. ($remaining attempts remaining)"
                else "Account not found. Login rate limit exceeded, please wait 60 seconds."
            )
        }

        val inputHash = hashPassword(cleanPass)
        if (existing.passwordHash != inputHash) {
            recordFailedAttempt()
            val remaining = getRemainingAttempts()
            return AuthResult.Error(
                if (remaining > 0) "Incorrect password. ($remaining attempts remaining)"
                else "Incorrect password. Rate limit reached, please wait 60 seconds."
            )
        }

        resetFailedAttempts()
        userDao.deactivateAllUsers()
        userDao.recordLogin(cleanEmail, System.currentTimeMillis())

        saveSession(
            email = existing.email,
            name = existing.displayName,
            affiliation = existing.affiliation,
            field = existing.researchField
        )

        return AuthResult.Success(existing)
    }

    suspend fun register(
        name: String,
        email: String,
        password: String,
        affiliation: String,
        researchField: String,
        acceptedPrivacy: Boolean
    ): AuthResult {
        val cleanName = name.trim()
        val cleanEmail = email.trim().lowercase()
        val cleanPass = password.trim()
        val cleanAffiliation = affiliation.trim()
        val cleanField = researchField.trim()

        if (cleanName.isBlank()) return AuthResult.Error("Please enter your full name.")
        if (cleanEmail.isBlank() || !cleanEmail.contains('@')) {
            return AuthResult.Error("Please provide a valid academic email address.")
        }
        if (cleanPass.length < 6) {
            return AuthResult.Error("Password must be at least 6 characters long.")
        }
        if (!acceptedPrivacy) {
            return AuthResult.Error("You must agree to the Terms of Service and Privacy Policy to create an account.")
        }

        val existing = userDao.findUserByEmail(cleanEmail)
        if (existing != null) {
            return AuthResult.Error("An account with this email already exists. Please log in.")
        }

        val newUser = UserAccount(
            id = UUID.randomUUID().toString(),
            email = cleanEmail,
            displayName = cleanName,
            passwordHash = hashPassword(cleanPass),
            affiliation = cleanAffiliation.ifBlank { "Independent Researcher" },
            researchField = cleanField.ifBlank { "Interdisciplinary Research" },
            isActive = true,
            createdAt = System.currentTimeMillis(),
            lastLoginAt = System.currentTimeMillis()
        )

        userDao.deactivateAllUsers()
        userDao.insertUser(newUser)

        saveSession(
            email = newUser.email,
            name = newUser.displayName,
            affiliation = newUser.affiliation,
            field = newUser.researchField,
            privacyAccepted = true
        )

        return AuthResult.Success(newUser)
    }

    /**
     * One-tap instant demo login as an Academic Researcher for zero-friction testing.
     */
    suspend fun loginAsDemoResearcher(): AuthResult {
        val demoEmail = "demo.researcher@cite.circle"
        var demo = userDao.findUserByEmail(demoEmail)
        if (demo == null) {
            demo = UserAccount(
                id = UUID.randomUUID().toString(),
                email = demoEmail,
                displayName = "Dr. Morgan Vance",
                passwordHash = hashPassword("citecircle2026"),
                affiliation = "Institute for Advanced Study",
                researchField = "Computational Neuroscience & AI",
                isActive = true,
                createdAt = System.currentTimeMillis(),
                lastLoginAt = System.currentTimeMillis()
            )
            userDao.insertUser(demo)
        } else {
            userDao.recordLogin(demoEmail, System.currentTimeMillis())
        }

        userDao.deactivateAllUsers()
        userDao.recordLogin(demoEmail, System.currentTimeMillis())

        saveSession(
            email = demo.email,
            name = demo.displayName,
            affiliation = demo.affiliation,
            field = demo.researchField,
            privacyAccepted = true
        )

        return AuthResult.Success(demo)
    }

    suspend fun acceptPrivacyPolicy() {
        store.edit { it[keyPrivacyAccepted] = true }
    }

    suspend fun signOut() {
        val email = currentUserEmail.first()
        if (email.isNotBlank()) {
            userDao.deactivateUser(email)
        }
        store.edit {
            it[keyIsLoggedIn] = false
            it[keyUserEmail] = ""
            it[keyUserName] = ""
            it[keyUserAffiliation] = ""
            it[keyUserField] = ""
        }
    }

    suspend fun deleteAccount() {
        val email = currentUserEmail.first()
        if (email.isNotBlank()) {
            userDao.deleteUser(email)
        }
        signOut()
    }

    private suspend fun saveSession(
        email: String,
        name: String,
        affiliation: String,
        field: String,
        privacyAccepted: Boolean? = null
    ) {
        store.edit {
            it[keyIsLoggedIn] = true
            it[keyUserEmail] = email
            it[keyUserName] = name
            it[keyUserAffiliation] = affiliation
            it[keyUserField] = field
            if (privacyAccepted != null) {
                it[keyPrivacyAccepted] = privacyAccepted
            }
        }
    }
}
