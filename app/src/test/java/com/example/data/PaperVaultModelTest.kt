package com.example.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PaperVaultModelTest {

    @Test
    fun `SavedPaper default values for Phase 3 fields are correct`() {
        val paper = SavedPaper(
            id = "test-1",
            authorInitials = "AB",
            authorName = "Alice Brown",
            affiliation = "MIT",
            content = "Exciting discovery."
        )

        assertEquals("", paper.pdfUrl)
        assertEquals("", paper.pdfLocalPath)
        assertEquals("", paper.abstractText)
        assertFalse(paper.openAccess)
    }

    @Test
    fun `SavedPaper holds PDF and open-access metadata`() {
        val paper = SavedPaper(
            id = "test-2",
            authorInitials = "CD",
            authorName = "Charles Darwin",
            affiliation = "Cambridge",
            content = "Origin of species commentary.",
            title = "On the Origin of Species",
            pdfUrl = "https://example.org/darwin.pdf",
            pdfLocalPath = "/data/user/0/com.example/files/vault_pdfs/test.pdf",
            abstractText = "A study concerning natural selection and adaptation.",
            openAccess = true
        )

        assertEquals("https://example.org/darwin.pdf", paper.pdfUrl)
        assertEquals("/data/user/0/com.example/files/vault_pdfs/test.pdf", paper.pdfLocalPath)
        assertEquals("A study concerning natural selection and adaptation.", paper.abstractText)
        assertTrue(paper.openAccess)
    }

    @Test
    fun `PdfStore formats file sizes accurately`() {
        assertEquals("0 B", PdfStore.formatFileSize(0))
        assertEquals("500 KB", PdfStore.formatFileSize(500 * 1024))
        assertEquals("1.5 MB", PdfStore.formatFileSize((1.5 * 1024 * 1024).toLong()))
        assertEquals("12.0 MB", PdfStore.formatFileSize(12 * 1024 * 1024))
    }
}
