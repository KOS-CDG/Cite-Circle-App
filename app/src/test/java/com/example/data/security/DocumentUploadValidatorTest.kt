package com.example.data.security

import com.example.data.PdfStore
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.ByteArrayInputStream

class DocumentUploadValidatorTest {

    @Before
    fun setUp() {
        DocumentUploadValidator.resetRateLimit()
    }

    @Test
    fun `valid PDF stream succeeds`() {
        val pdfBytes = "%PDF-1.7\nSample content for research paper".toByteArray(Charsets.UTF_8)
        val result = DocumentUploadValidator.validateStream(
            stream = ByteArrayInputStream(pdfBytes),
            expectedFormat = DocumentFormat.PDF,
            fileName = "paper.pdf"
        )
        assertTrue(result is ValidationResult.Success)
        val success = result as ValidationResult.Success
        assertEquals(DocumentFormat.PDF, success.format)
        assertEquals("paper.pdf", success.sanitizedFileName)
        assertEquals(pdfBytes.size.toLong(), success.sizeBytes)
    }

    @Test
    fun `Windows executable MZ header is blocked`() {
        val mzBytes = byteArrayOf(0x4D, 0x5A, 0x90.toByte(), 0x00, 0x03, 0x00, 0x00, 0x00)
        val result = DocumentUploadValidator.validateStream(
            stream = ByteArrayInputStream(mzBytes),
            expectedFormat = DocumentFormat.PDF,
            fileName = "trojan.pdf"
        )
        assertTrue(result is ValidationResult.Error)
        val error = result as ValidationResult.Error
        assertTrue(error.message.contains("Executable binary detected (MZ header)"))
    }

    @Test
    fun `Linux ELF binary header is blocked`() {
        val elfBytes = byteArrayOf(0x7F, 'E'.code.toByte(), 'L'.code.toByte(), 'F'.code.toByte(), 0x02, 0x01)
        val result = DocumentUploadValidator.validateStream(
            stream = ByteArrayInputStream(elfBytes),
            expectedFormat = DocumentFormat.PDF,
            fileName = "virus.pdf"
        )
        assertTrue(result is ValidationResult.Error)
        val error = result as ValidationResult.Error
        assertTrue(error.message.contains("Linux/Android binary executable detected"))
    }

    @Test
    fun `Shell script header is blocked`() {
        val shBytes = "#!/bin/bash\nrm -rf /".toByteArray(Charsets.UTF_8)
        val result = DocumentUploadValidator.validateStream(
            stream = ByteArrayInputStream(shBytes),
            expectedFormat = DocumentFormat.TXT,
            fileName = "script.txt"
        )
        assertTrue(result is ValidationResult.Error)
        val error = result as ValidationResult.Error
        assertTrue(error.message.contains("Shell script detected"))
    }

    @Test
    fun `Disguised ZIP archive in non-DOCX file is rejected`() {
        val zipHeaderBytes = byteArrayOf(0x50, 0x4B, 0x03, 0x04, 0x14, 0x00, 0x00, 0x00)
        val result = DocumentUploadValidator.validateStream(
            stream = ByteArrayInputStream(zipHeaderBytes),
            expectedFormat = DocumentFormat.PDF,
            fileName = "disguised_archive.pdf"
        )
        assertTrue(result is ValidationResult.Error)
        val error = result as ValidationResult.Error
        assertTrue(error.message.contains("Disguised ZIP archive detected"))
    }

    @Test
    fun `DOCX format allows valid OpenXML ZIP header`() {
        val docxZipBytes = byteArrayOf(0x50, 0x4B, 0x03, 0x04, 0x14, 0x00, 0x06, 0x00, 0x08, 0x00)
        val result = DocumentUploadValidator.validateStream(
            stream = ByteArrayInputStream(docxZipBytes),
            expectedFormat = DocumentFormat.DOCX,
            fileName = "manuscript.docx"
        )
        assertTrue(result is ValidationResult.Success)
        val success = result as ValidationResult.Success
        assertEquals(DocumentFormat.DOCX, success.format)
    }

    @Test
    fun `PDF format requires percent-PDF header`() {
        val nonPdfBytes = "Random text that does not have PDF header".toByteArray(Charsets.UTF_8)
        val result = DocumentUploadValidator.validateStream(
            stream = ByteArrayInputStream(nonPdfBytes),
            expectedFormat = DocumentFormat.PDF,
            fileName = "invalid.pdf"
        )
        assertTrue(result is ValidationResult.Error)
        val error = result as ValidationResult.Error
        assertTrue(error.message.contains("valid PDF header"))
    }

    @Test
    fun `Empty file stream is rejected`() {
        val emptyBytes = ByteArray(0)
        val result = DocumentUploadValidator.validateStream(
            stream = ByteArrayInputStream(emptyBytes),
            expectedFormat = DocumentFormat.TXT,
            fileName = "empty.txt"
        )
        assertTrue(result is ValidationResult.Error)
        val error = result as ValidationResult.Error
        assertTrue(error.message.contains("empty"))
    }

    @Test
    fun `Rate limiter enforces upload throttling`() {
        DocumentUploadValidator.resetRateLimit()
        for (i in 1..5) {
            assertTrue("Upload $i should be permitted", DocumentUploadValidator.checkRateLimit())
        }
        // 6th upload within the same minute should be rejected
        assertFalse("6th upload should be blocked by rate limit", DocumentUploadValidator.checkRateLimit())
    }

    @Test
    fun `PdfStore getDocumentFormat resolves extensions accurately`() {
        assertEquals(DocumentFormat.PDF, PdfStore.getDocumentFormat("/path/paper.pdf"))
        assertEquals(DocumentFormat.DOCX, PdfStore.getDocumentFormat("/path/paper.docx"))
        assertEquals(DocumentFormat.DOC, PdfStore.getDocumentFormat("/path/paper.doc"))
        assertEquals(DocumentFormat.RTF, PdfStore.getDocumentFormat("/path/paper.rtf"))
        assertEquals(DocumentFormat.TXT, PdfStore.getDocumentFormat("/path/paper.txt"))
        assertEquals(DocumentFormat.MD, PdfStore.getDocumentFormat("/path/paper.md"))
        assertEquals(DocumentFormat.TEX, PdfStore.getDocumentFormat("/path/paper.tex"))
    }
}
