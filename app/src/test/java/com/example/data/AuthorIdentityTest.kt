package com.example.data

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Covers the pure part of [AuthorIdentity]. `current()` reads FirebaseAuth and so is not
 * unit-testable without an Android runtime; the initials derivation is plain Kotlin.
 */
class AuthorIdentityTest {

    @Test
    fun `two-part names use the first and last initial`() {
        assertEquals("JD", AuthorIdentity.initialsOf("Jane Doe"))
        assertEquals("JT", AuthorIdentity.initialsOf("Julian Thorne"))
    }

    @Test
    fun `middle names are ignored in favour of first and last`() {
        assertEquals("AB", AuthorIdentity.initialsOf("Alice Marie Brown"))
    }

    @Test
    fun `honorifics are stripped rather than initialised`() {
        assertEquals("JD", AuthorIdentity.initialsOf("Dr. Jane Doe"))
        assertEquals("JD", AuthorIdentity.initialsOf("Prof Jane Doe"))
        assertEquals("JT", AuthorIdentity.initialsOf("Sir Julian Thorne"))
    }

    @Test
    fun `post-nominals are stripped from the end`() {
        assertEquals("JD", AuthorIdentity.initialsOf("Jane Doe, PhD"))
        assertEquals("JD", AuthorIdentity.initialsOf("Jane Doe Jr."))
    }

    @Test
    fun `a single name falls back to its first two letters`() {
        assertEquals("PL", AuthorIdentity.initialsOf("Plato"))
        assertEquals("JA", AuthorIdentity.initialsOf("Dr. Jane"))
    }

    @Test
    fun `a blank name yields the placeholder`() {
        assertEquals("??", AuthorIdentity.initialsOf("   "))
    }

    @Test
    fun `initials are always upper case`() {
        assertEquals("JD", AuthorIdentity.initialsOf("jane doe"))
    }
}
