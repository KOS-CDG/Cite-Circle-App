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
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.UserProfileChangeRequest
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.IOException
import java.security.MessageDigest
import java.util.UUID
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

private val Context.sessionDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "user_session"
)

private suspend fun <T> com.google.android.gms.tasks.Task<T>.awaitTask(): T =
    suspendCancellableCoroutine { cont ->
        addOnSuccessListener { result -> cont.resume(result) }
        addOnFailureListener { exception -> cont.resumeWithException(exception) }
    }

sealed class AuthResult {
    data class Success(val user: UserAccount) : AuthResult()
    data class Error(val message: String) : AuthResult()
    data class RateLimited(val waitSeconds: Int) : AuthResult()
}

/**
 * Manages user authentication with Firebase Auth cloud synchronization,
 * Room v5 local offline database persistence, security rate limiting,
 * and privacy consent state across the application.
 */
class UserSessionManager(
    private val context: Context,
    private val database: AppDatabase
) {

    private val store = context.applicationContext.sessionDataStore
    private val userDao = database.userAccountDao()

    private val keyIsLoggedIn = booleanPreferencesKey("is_logged_in")
    private val keyUserUid = stringPreferencesKey("user_uid")
    private val keyUserEmail = stringPreferencesKey("user_email")
    private val keyUserName = stringPreferencesKey("user_name")
    private val keyUserAffiliation = stringPreferencesKey("user_affiliation")
    private val keyUserField = stringPreferencesKey("user_field")
    private val keyPrivacyAccepted = booleanPreferencesKey("privacy_policy_accepted")

    private val preferences: Flow<Preferences> = store.data.catch { cause ->
        if (cause is IOException) emit(emptyPreferences()) else throw cause
    }

    val isLoggedIn: Flow<Boolean> = preferences.map { it[keyIsLoggedIn] ?: false }
    val currentUserUid: Flow<String> = preferences.map { it[keyUserUid].orEmpty() }
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

    /**
     * Signs in using Firebase Authentication with automatic offline fallback to local Room SQLite.
     */
    suspend fun login(email: String, password: String): AuthResult {
        val cleanEmail = email.trim().lowercase()
        val cleanPass = password.trim()

        if (cleanEmail.isBlank() || cleanPass.isBlank()) {
            return AuthResult.Error("Please enter your academic email and password.")
        }

        if (!checkLoginRateLimit()) {
            return AuthResult.RateLimited(getCooldownRemainingSeconds())
        }

        // 1. Attempt Firebase Auth Cloud Sign-In
        var firebaseUid: String? = null
        var firebaseDisplayName: String? = null

        try {
            val auth = FirebaseAuth.getInstance()
            val result = auth.signInWithEmailAndPassword(cleanEmail, cleanPass).awaitTask()
            firebaseUid = result.user?.uid
            firebaseDisplayName = result.user?.displayName
        } catch (e: FirebaseAuthInvalidUserException) {
            recordFailedAttempt()
            val remaining = getRemainingAttempts()
            return AuthResult.Error(
                if (remaining > 0) "Account not found on Firebase. ($remaining attempts remaining)"
                else "Account not found. Login rate limit exceeded, please wait 60 seconds."
            )
        } catch (e: FirebaseAuthInvalidCredentialsException) {
            recordFailedAttempt()
            val remaining = getRemainingAttempts()
            return AuthResult.Error(
                if (remaining > 0) "Incorrect password for this academic account. ($remaining attempts remaining)"
                else "Incorrect password. Rate limit reached, please wait 60 seconds."
            )
        } catch (e: Exception) {
            // Network unavailable or Firebase uninitialized: fall back to local Room database verification
        }

        // 2. Synchronize with Local Room Database
        var user = userDao.findUserByEmail(cleanEmail)
        val inputHash = hashPassword(cleanPass)

        if (user != null) {
            // If offline, check local password hash
            if (firebaseUid == null && user.passwordHash != inputHash) {
                recordFailedAttempt()
                val remaining = getRemainingAttempts()
                return AuthResult.Error("Incorrect password for offline account. ($remaining attempts remaining)")
            }
        } else {
            // New device login with valid Firebase Auth credentials: seed local Room user
            if (firebaseUid != null) {
                user = UserAccount(
                    id = firebaseUid,
                    email = cleanEmail,
                    displayName = firebaseDisplayName ?: cleanEmail.substringBefore('@').replace('.', ' ').capitalize(),
                    passwordHash = inputHash,
                    affiliation = "Academic Institution",
                    researchField = "All Disciplines",
                    isActive = true,
                    createdAt = System.currentTimeMillis(),
                    lastLoginAt = System.currentTimeMillis()
                )
                userDao.insertUser(user)
            } else {
                recordFailedAttempt()
                return AuthResult.Error("Account not found in local database or offline cache.")
            }
        }

        resetFailedAttempts()
        userDao.deactivateAllUsers()
        userDao.recordLogin(cleanEmail, System.currentTimeMillis())

        saveSession(
            uid = firebaseUid ?: user.id,
            email = user.email,
            name = user.displayName,
            affiliation = user.affiliation,
            field = user.researchField
        )

        return AuthResult.Success(user)
    }

    /**
     * Registers a new account on Firebase Auth and synchronizes with local Room v5 database.
     */
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

        // 1. Attempt Cloud Registration via Firebase Auth
        var assignedUid: String = UUID.randomUUID().toString()
        try {
            val auth = FirebaseAuth.getInstance()
            val authResult = auth.createUserWithEmailAndPassword(cleanEmail, cleanPass).awaitTask()
            val firebaseUser = authResult.user
            if (firebaseUser != null) {
                assignedUid = firebaseUser.uid
                val profileUpdates = UserProfileChangeRequest.Builder()
                    .setDisplayName(cleanName)
                    .build()
                firebaseUser.updateProfile(profileUpdates).awaitTask()
            }
        } catch (e: FirebaseAuthUserCollisionException) {
            return AuthResult.Error("An account with this email address already exists on Firebase. Please log in.")
        } catch (e: Exception) {
            // Offline or fallback mode: proceed with local Room storage
        }

        val existingLocal = userDao.findUserByEmail(cleanEmail)
        if (existingLocal != null) {
            return AuthResult.Error("An account with this email already exists locally. Please log in.")
        }

        val newUser = UserAccount(
            id = assignedUid,
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
            uid = assignedUid,
            email = newUser.email,
            name = newUser.displayName,
            affiliation = newUser.affiliation,
            field = newUser.researchField,
            privacyAccepted = true
        )

        return AuthResult.Success(newUser)
    }

    /**
     * One-tap instant demo login as an Academic Researcher synced with Firebase Auth.
     */
    suspend fun loginAsDemoResearcher(): AuthResult {
        val demoEmail = "demo.researcher@cite.circle"
        val demoPass = "citecircle2026"
        val demoName = "Dr. Morgan Vance"

        var assignedUid: String = "demo-researcher-uid"
        try {
            val auth = FirebaseAuth.getInstance()
            val authResult = try {
                auth.signInWithEmailAndPassword(demoEmail, demoPass).awaitTask()
            } catch (e: Exception) {
                auth.createUserWithEmailAndPassword(demoEmail, demoPass).awaitTask()
            }
            assignedUid = authResult.user?.uid ?: assignedUid
        } catch (e: Exception) {
            // Offline fallback
        }

        var demo = userDao.findUserByEmail(demoEmail)
        if (demo == null) {
            demo = UserAccount(
                id = assignedUid,
                email = demoEmail,
                displayName = demoName,
                passwordHash = hashPassword(demoPass),
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
            uid = assignedUid,
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
        try {
            FirebaseAuth.getInstance().signOut()
        } catch (e: Exception) {
            // Ignore if Firebase uninitialized
        }

        val email = currentUserEmail.first()
        if (email.isNotBlank()) {
            userDao.deactivateUser(email)
        }
        store.edit {
            it[keyIsLoggedIn] = false
            it[keyUserUid] = ""
            it[keyUserEmail] = ""
            it[keyUserName] = ""
            it[keyUserAffiliation] = ""
            it[keyUserField] = ""
        }
    }

    suspend fun deleteAccount() {
        try {
            FirebaseAuth.getInstance().currentUser?.delete()?.awaitTask()
        } catch (e: Exception) {
            // Ignore if offline
        }

        val email = currentUserEmail.first()
        if (email.isNotBlank()) {
            userDao.deleteUser(email)
        }
        signOut()
    }

    private suspend fun saveSession(
        uid: String,
        email: String,
        name: String,
        affiliation: String,
        field: String,
        privacyAccepted: Boolean? = null
    ) {
        store.edit {
            it[keyIsLoggedIn] = true
            it[keyUserUid] = uid
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
