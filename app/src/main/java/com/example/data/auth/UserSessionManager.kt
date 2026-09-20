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
import com.example.network.SupabaseClient
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
 * Manages user authentication with Supabase Cloud synchronization,
 * Room local offline database persistence, security rate limiting,
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
    private val keyAccessToken = stringPreferencesKey("supabase_access_token")
    private val keyRefreshToken = stringPreferencesKey("supabase_refresh_token")
    private val keyPrivacyAccepted = booleanPreferencesKey("privacy_policy_accepted")
    private val keyRememberLogin = booleanPreferencesKey("remember_login_info")
    private val keyUserHeadline = stringPreferencesKey("user_headline")
    private val keyUserBio = stringPreferencesKey("user_bio")
    private val keyUserLocation = stringPreferencesKey("user_location")
    private val keyUserAvatarUri = stringPreferencesKey("user_avatar_uri")
    private val keyUserCoverUri = stringPreferencesKey("user_cover_uri")
    private val keyUserOrcid = stringPreferencesKey("user_orcid")
    private val keyUserWebsite = stringPreferencesKey("user_website")
    private val keyUserOpenTo = stringPreferencesKey("user_open_to")

    private val preferences: Flow<Preferences> = store.data.catch { cause ->
        if (cause is IOException) emit(emptyPreferences()) else throw cause
    }

    val isLoggedIn: Flow<Boolean> = preferences.map { it[keyIsLoggedIn] ?: false }
    val currentUserUid: Flow<String> = preferences.map { it[keyUserUid].orEmpty() }
    val currentUserEmail: Flow<String> = preferences.map { it[keyUserEmail].orEmpty() }
    val currentUserName: Flow<String> = preferences.map { it[keyUserName].orEmpty() }
    val currentUserAffiliation: Flow<String> = preferences.map { it[keyUserAffiliation].orEmpty() }
    val currentUserField: Flow<String> = preferences.map { it[keyUserField].orEmpty() }
    val currentUserHeadline: Flow<String> = preferences.map { it[keyUserHeadline].orEmpty() }
    val currentUserBio: Flow<String> = preferences.map { it[keyUserBio].orEmpty() }
    val currentUserLocation: Flow<String> = preferences.map { it[keyUserLocation].orEmpty() }
    val currentUserAvatarUri: Flow<String> = preferences.map { it[keyUserAvatarUri].orEmpty() }
    val currentUserCoverUri: Flow<String> = preferences.map { it[keyUserCoverUri].orEmpty() }
    val currentUserOrcid: Flow<String> = preferences.map { it[keyUserOrcid].orEmpty() }
    val currentUserWebsite: Flow<String> = preferences.map { it[keyUserWebsite].orEmpty() }
    val currentUserOpenTo: Flow<String> = preferences.map { it[keyUserOpenTo].orEmpty() }
    val currentAccessToken: Flow<String> = preferences.map { it[keyAccessToken].orEmpty() }
    val isPrivacyAccepted: Flow<Boolean> = preferences.map { it[keyPrivacyAccepted] ?: false }
    val rememberLoginInfo: Flow<Boolean> = preferences.map { it[keyRememberLogin] ?: true }

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
     * Signs in using Supabase Authentication with automatic offline fallback to local Room SQLite.
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

        // 1. Attempt Supabase Cloud Sign-In
        var remoteUid: String? = null
        var remoteDisplayName: String? = null
        var accessToken = ""
        var refreshToken = ""

        val remoteResult = SupabaseClient.signIn(cleanEmail, cleanPass)
        if (remoteResult.isSuccess) {
            val authResp = remoteResult.getOrThrow()
            remoteUid = authResp.user.id
            remoteDisplayName = authResp.user.fullName.ifBlank {
                cleanEmail.substringBefore('@').replace('.', ' ')
                    .replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
            }
            accessToken = authResp.accessToken
            refreshToken = authResp.refreshToken
        }

        // 2. Synchronize with Local Room Database
        var user = userDao.findUserByEmail(cleanEmail)
        val inputHash = hashPassword(cleanPass)

        if (user != null) {
            // If offline and cloud failed, verify password hash locally
            if (remoteUid == null) {
                if (user.passwordHash != inputHash) {
                    recordFailedAttempt()
                    val remaining = getRemainingAttempts()
                    return AuthResult.Error("Incorrect password for offline account. ($remaining attempts remaining)")
                }
            } else {
                // Update local record with latest verified info
                user = user.copy(
                    id = remoteUid,
                    displayName = remoteDisplayName ?: user.displayName,
                    lastLoginAt = System.currentTimeMillis()
                )
                userDao.insertUser(user)
            }
        } else {
            if (remoteUid != null) {
                user = UserAccount(
                    id = remoteUid,
                    email = cleanEmail,
                    displayName = remoteDisplayName ?: cleanEmail.substringBefore('@'),
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
                val remaining = getRemainingAttempts()
                val errorMsg = remoteResult.exceptionOrNull()?.message ?: "Invalid academic credentials"
                return AuthResult.Error("$errorMsg ($remaining attempts remaining)")
            }
        }

        resetFailedAttempts()
        userDao.deactivateAllUsers()
        userDao.recordLogin(cleanEmail, System.currentTimeMillis())

        saveSession(
            uid = remoteUid ?: user.id,
            email = user.email,
            name = user.displayName,
            affiliation = user.affiliation,
            field = user.researchField,
            accessToken = accessToken,
            refreshToken = refreshToken
        )

        return AuthResult.Success(user)
    }

    /**
     * Registers a new account on Supabase Auth and synchronizes with local Room database.
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

        // 1. Attempt Cloud Registration via Supabase Auth
        val username = cleanEmail.substringBefore('@').replace(Regex("[^a-zA-Z0-9_]"), "_")
        var assignedUid: String = UUID.randomUUID().toString()
        var accessToken = ""
        var refreshToken = ""

        val signupResult = SupabaseClient.signUp(
            email = cleanEmail,
            password = cleanPass,
            fullName = cleanName,
            username = username
        )

        if (signupResult.isSuccess) {
            val authResp = signupResult.getOrThrow()
            assignedUid = authResp.user.id
            accessToken = authResp.accessToken
            refreshToken = authResp.refreshToken
        } else {
            val error = signupResult.exceptionOrNull()?.message ?: "Sign up failed"
            if (error.contains("already registered", ignoreCase = true) || error.contains("user already exists", ignoreCase = true)) {
                return AuthResult.Error("An account with this email address already exists. Please log in.")
            }
            // Fall back gracefully if offline
        }

        val existingLocal = userDao.findUserByEmail(cleanEmail)
        if (existingLocal != null && signupResult.isFailure) {
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
            accessToken = accessToken,
            refreshToken = refreshToken,
            privacyAccepted = true
        )

        return AuthResult.Success(newUser)
    }

    /**
     * Instant clean Guest Researcher access with neutral profile.
     */
    suspend fun loginAsGuest(): AuthResult {
        val guestEmail = "guest.researcher@citecircle.app"
        val guestName = "Guest Researcher"
        val guestUid = "guest-" + UUID.randomUUID().toString().take(8)

        val guestUser = UserAccount(
            id = guestUid,
            email = guestEmail,
            displayName = guestName,
            passwordHash = "",
            affiliation = "Visiting Researcher",
            researchField = "Academic Research",
            isActive = true,
            createdAt = System.currentTimeMillis(),
            lastLoginAt = System.currentTimeMillis()
        )

        userDao.deactivateAllUsers()
        userDao.insertUser(guestUser)

        saveSession(
            uid = guestUid,
            email = guestUser.email,
            name = guestUser.displayName,
            affiliation = guestUser.affiliation,
            field = guestUser.researchField,
            privacyAccepted = true
        )

        return AuthResult.Success(guestUser)
    }

    /** Backward compatibility alias for UI */
    suspend fun loginAsDemoResearcher(): AuthResult = loginAsGuest()

    suspend fun acceptPrivacyPolicy() {
        store.edit { it[keyPrivacyAccepted] = true }
    }

    suspend fun setRememberLoginInfo(remember: Boolean) {
        store.edit { it[keyRememberLogin] = remember }
    }

    fun getAllSavedAccounts(): Flow<List<UserAccount>> = userDao.getAllUsers()

    /**
     * Updates personal and academic details in Room and local state.
     */
    suspend fun updateProfile(
        displayName: String,
        affiliation: String,
        researchField: String
    ): AuthResult {
        val cleanName = displayName.trim()
        val cleanAffiliation = affiliation.trim()
        val cleanField = researchField.trim()

        if (cleanName.isBlank()) {
            return AuthResult.Error("Name cannot be blank.")
        }

        val email = currentUserEmail.first()
        if (email.isBlank()) {
            return AuthResult.Error("No active user session found.")
        }

        // 1. Update Room local database
        userDao.updateProfileInfo(email, cleanName, cleanAffiliation, cleanField)

        // 2. Update DataStore session
        store.edit {
            it[keyUserName] = cleanName
            it[keyUserAffiliation] = cleanAffiliation
            it[keyUserField] = cleanField
        }

        val updated = userDao.findUserByEmail(email)
        return if (updated != null) AuthResult.Success(updated)
        else AuthResult.Error("Failed to retrieve updated profile.")
    }

    /**
     * Comprehensive profile updater for LinkedIn-style profile attributes.
     */
    suspend fun updateFullProfile(
        displayName: String,
        headline: String,
        affiliation: String,
        researchField: String,
        location: String,
        bio: String,
        orcid: String,
        website: String
    ): AuthResult {
        val cleanName = displayName.trim()
        val cleanHeadline = headline.trim()
        val cleanAffiliation = affiliation.trim()
        val cleanField = researchField.trim()
        val cleanLocation = location.trim()
        val cleanBio = bio.trim()
        val cleanOrcid = orcid.trim()
        val cleanWebsite = website.trim()

        if (cleanName.isBlank()) {
            return AuthResult.Error("Name cannot be blank.")
        }

        val email = currentUserEmail.first()
        if (email.isNotBlank()) {
            userDao.updateProfileInfo(email, cleanName, cleanAffiliation, cleanField)
        }

        store.edit {
            it[keyUserName] = cleanName
            it[keyUserHeadline] = cleanHeadline
            it[keyUserAffiliation] = cleanAffiliation
            it[keyUserField] = cleanField
            it[keyUserLocation] = cleanLocation
            it[keyUserBio] = cleanBio
            it[keyUserOrcid] = cleanOrcid
            it[keyUserWebsite] = cleanWebsite
        }

        val updated = if (email.isNotBlank()) userDao.findUserByEmail(email) else null
        return if (updated != null) AuthResult.Success(updated)
        else AuthResult.Success(UserAccount(
            id = currentUserUid.first().ifBlank { java.util.UUID.randomUUID().toString() },
            email = email,
            displayName = cleanName,
            passwordHash = "",
            affiliation = cleanAffiliation,
            researchField = cleanField
        ))
    }

    suspend fun updateAvatarUri(uriString: String) {
        store.edit { it[keyUserAvatarUri] = uriString }
    }

    suspend fun updateCoverUri(uriString: String) {
        store.edit { it[keyUserCoverUri] = uriString }
    }

    suspend fun updateOpenTo(options: String) {
        store.edit { it[keyUserOpenTo] = options }
    }

    /**
     * Changes account password with validation and local database synchronization.
     */
    suspend fun changePassword(oldPassword: String, newPassword: String): AuthResult {
        val cleanOld = oldPassword.trim()
        val cleanNew = newPassword.trim()

        if (cleanOld.isBlank() || cleanNew.isBlank()) {
            return AuthResult.Error("Please provide both current and new passwords.")
        }
        if (cleanNew.length < 6) {
            return AuthResult.Error("New password must be at least 6 characters.")
        }
        if (cleanOld == cleanNew) {
            return AuthResult.Error("New password must be different from current password.")
        }

        val email = currentUserEmail.first()
        if (email.isBlank()) {
            return AuthResult.Error("No active user session found.")
        }

        val localUser = userDao.findUserByEmail(email)
            ?: return AuthResult.Error("User record not found.")

        // Verify old password
        val oldHash = hashPassword(cleanOld)
        if (localUser.passwordHash != oldHash) {
            return AuthResult.Error("Current password is incorrect.")
        }

        val newHash = hashPassword(cleanNew)
        userDao.updatePasswordHash(email, newHash)

        return AuthResult.Success(localUser.copy(passwordHash = newHash))
    }

    /**
     * Switches session to another saved local account.
     */
    suspend fun switchAccount(targetEmail: String): AuthResult {
        val cleanEmail = targetEmail.trim().lowercase()
        val targetUser = userDao.findUserByEmail(cleanEmail)
            ?: return AuthResult.Error("Account $cleanEmail not found.")

        userDao.deactivateAllUsers()
        userDao.recordLogin(cleanEmail, System.currentTimeMillis())

        saveSession(
            uid = targetUser.id,
            email = targetUser.email,
            name = targetUser.displayName,
            affiliation = targetUser.affiliation,
            field = targetUser.researchField,
            privacyAccepted = true
        )

        return AuthResult.Success(targetUser)
    }

    /**
     * Logout with optional credential retention on device.
     */
    suspend fun signOut(keepSavedOnDevice: Boolean = true) {
        val email = currentUserEmail.first()
        if (email.isNotBlank()) {
            if (keepSavedOnDevice) {
                userDao.deactivateUser(email)
            } else {
                userDao.deleteUser(email)
            }
        }

        store.edit {
            it[keyIsLoggedIn] = false
            it[keyUserUid] = ""
            it[keyUserEmail] = ""
            it[keyUserName] = ""
            it[keyUserAffiliation] = ""
            it[keyUserField] = ""
            it[keyAccessToken] = ""
            it[keyRefreshToken] = ""
        }
    }

    suspend fun deleteAccount() {
        val email = currentUserEmail.first()
        if (email.isNotBlank()) {
            userDao.deleteUser(email)
        }
        signOut(keepSavedOnDevice = false)
    }

    private suspend fun saveSession(
        uid: String,
        email: String,
        name: String,
        affiliation: String,
        field: String,
        accessToken: String = "",
        refreshToken: String = "",
        privacyAccepted: Boolean? = null
    ) {
        store.edit {
            it[keyIsLoggedIn] = true
            it[keyUserUid] = uid
            it[keyUserEmail] = email
            it[keyUserName] = name
            it[keyUserAffiliation] = affiliation
            it[keyUserField] = field
            if (accessToken.isNotBlank()) {
                it[keyAccessToken] = accessToken
            }
            if (refreshToken.isNotBlank()) {
                it[keyRefreshToken] = refreshToken
            }
            if (privacyAccepted != null) {
                it[keyPrivacyAccepted] = privacyAccepted
            }
        }
    }
}
