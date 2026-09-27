package com.example.data.nativebridge

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class NativeCitationEngineTest {

    @Test
    fun testDetectDocumentFormatPdf() {
        val pdfHeader = "%PDF-1.7\n%abc".toByteArray(Charsets.ISO_8859_1)
        val format = NativeCitationEngine.detectDocumentFormat(pdfHeader)
        assertEquals(1, format)
        assertEquals("PDF Document", NativeCitationEngine.getFormatLabel(format))
    }

    @Test
    fun testDetectDocumentFormatPostScript() {
        val psHeader = "%!PS-Adobe-3.0".toByteArray(Charsets.ISO_8859_1)
        val format = NativeCitationEngine.detectDocumentFormat(psHeader)
        assertEquals(2, format)
        assertEquals("PostScript Document", NativeCitationEngine.getFormatLabel(format))
    }

    @Test
    fun testDetectDocumentFormatRtf() {
        val rtfHeader = "{\\rtf1\\ansi\\deff0".toByteArray(Charsets.ISO_8859_1)
        val format = NativeCitationEngine.detectDocumentFormat(rtfHeader)
        assertEquals(3, format)
        assertEquals("Rich Text Format (RTF)", NativeCitationEngine.getFormatLabel(format))
    }

    @Test
    fun testDetectDocumentFormatZip() {
        val zipHeader = byteArrayOf(0x50, 0x4B, 0x03, 0x04, 0x14, 0x00)
        val format = NativeCitationEngine.detectDocumentFormat(zipHeader)
        assertEquals(4, format)
        assertEquals("EPUB / ZIP Container", NativeCitationEngine.getFormatLabel(format))
    }

    @Test
    fun testDetectDocumentFormatUnknown() {
        val textBytes = "This is a standard text file".toByteArray()
        val format = NativeCitationEngine.detectDocumentFormat(textBytes)
        assertEquals(0, format)
        assertEquals("Plain Text / Unknown", NativeCitationEngine.getFormatLabel(format))
    }

    @Test
    fun testParseBibTeX() {
        val bibtex = """
            @article{vaswani2017attention,
                title = {Attention Is All You Need},
                author = {Vaswani, Ashish and Shazeer, Noam},
                year = {2017},
                journal = {NeurIPS},
                doi = {10.5555/3295222.3295349}
            }
        """.trimIndent()

        val entries = NativeCitationEngine.parseBibTeX(bibtex)
        assertEquals(1, entries.size)
        val entry = entries[0]
        assertEquals("article", entry.entryType)
        assertEquals("vaswani2017attention", entry.citeKey)
        assertEquals("Attention Is All You Need", entry.title)
        assertEquals("Vaswani, Ashish and Shazeer, Noam", entry.author)
        assertEquals("2017", entry.year)
        assertEquals("NeurIPS", entry.venue)
        assertEquals("10.5555/3295222.3295349", entry.doi)
    }

    @Test
    fun testEngineBanner() {
        val banner = NativeCitationEngine.getEngineBanner()
        assertNotNull(banner)
        assertTrue(banner.isNotBlank())
    }
}
