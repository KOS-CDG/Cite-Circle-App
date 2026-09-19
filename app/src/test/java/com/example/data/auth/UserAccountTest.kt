package com.example.data.auth

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.UUID

class UserAccountTest {

    @Before
    fun setUp() {
        UserSessionManager.resetFailedAttempts()
    }

    @Test
    fun `password hashing produces consistent non-plaintext hash`() {
        val password = "academicSecretPassword123"
        val hash1 = UserSessionManager.hashPassword(password)
        val hash2 = UserSessionManager.hashPassword(password)

        assertEquals(hash1, hash2)
        assertNotEquals(password, hash1)
        assertEquals(64, hash1.length) // SHA-256 hex string length
    }

    @Test
    fun `different passwords produce distinct hashes`() {
        val hashA = UserSessionManager.hashPassword("PasswordAlpha")
        val hashB = UserSessionManager.hashPassword("PasswordBeta")

        assertNotEquals(hashA, hashB)
    }

    @Test
    fun `login rate limiter allows up to 5 attempts then blocks`() {
        UserSessionManager.resetFailedAttempts()

        // First 5 attempts should be allowed
        for (i in 1..5) {
            assertTrue("Attempt $i should be permitted", UserSessionManager.checkLoginRateLimit())
            UserSessionManager.recordFailedAttempt()
        }

        // 6th attempt should be blocked
        assertFalse("6th attempt should be blocked by rate limit", UserSessionManager.checkLoginRateLimit())
        assertEquals(0, UserSessionManager.getRemainingAttempts())
        assertTrue("Cooldown should be > 0", UserSessionManager.getCooldownRemainingSeconds() > 0)

        // Reset clears lock
        UserSessionManager.resetFailedAttempts()
        assertTrue("After reset, attempts should be permitted again", UserSessionManager.checkLoginRateLimit())
        assertEquals(5, UserSessionManager.getRemainingAttempts())
    }

    @Test
    fun `UserAccount model defaults and properties are valid`() {
        val id = UUID.randomUUID().toString()
        val account = UserAccount(
            id = id,
            email = "dr.curie@radium.org",
            displayName = "Dr. Marie Curie",
            passwordHash = UserSessionManager.hashPassword("nobelPrize1903"),
            affiliation = "Sorbonne University",
            researchField = "Physics & Chemistry"
        )

        assertEquals(id, account.id)
        assertEquals("dr.curie@radium.org", account.email)
        assertEquals("Dr. Marie Curie", account.displayName)
        assertEquals("Sorbonne University", account.affiliation)
        assertEquals("Physics & Chemistry", account.researchField)
        assertTrue(account.isActive)
        assertTrue(account.createdAt > 0L)
        assertTrue(account.lastLoginAt > 0L)
    }
}
