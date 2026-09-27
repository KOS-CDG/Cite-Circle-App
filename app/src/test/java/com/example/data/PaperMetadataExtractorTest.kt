package com.example.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class PaperMetadataExtractorTest {

    @Test
    fun `extractArxivIdentifier recognizes arXiv IDs in filenames and text`() {
        assertEquals("2301.07041", PaperMetadataExtractor.extractArxivIdentifier("2301.07041.pdf"))
        assertEquals("2106.09685v2", PaperMetadataExtractor.extractArxivIdentifier("arXiv:2106.09685v2"))
        assertEquals("1706.03762", PaperMetadataExtractor.extractArxivIdentifier("arxiv.org/abs/1706.03762"))
        assertEquals("2301.07041", PaperMetadataExtractor.extractArxivIdentifier("Preprint_2301.07041_v1.pdf"))
        assertNull(PaperMetadataExtractor.extractArxivIdentifier("regular_document_no_arxiv.pdf"))
    }

    @Test
    fun `extractDoiIdentifier detects and sanitizes DOIs`() {
        assertEquals("10.1038/s41586-020-2649-2", PaperMetadataExtractor.extractDoiIdentifier("doi: 10.1038/s41586-020-2649-2"))
        assertEquals("10.1109/CVPR.2016.90", PaperMetadataExtractor.extractDoiIdentifier("https://doi.org/10.1109/CVPR.2016.90."))
        assertEquals("10.1145/3372278.3390670", PaperMetadataExtractor.extractDoiIdentifier("DOI: 10.1145/3372278.3390670;"))
        assertNull(PaperMetadataExtractor.extractDoiIdentifier("not_a_doi_number_12345"))
    }

    @Test
    fun `cleanPdfString unescapes PDF parentheses and whitespace`() {
        val raw = """Deep Learning \(with PyTorch\) and Neural Networks\\Models\r\nand Architectures"""
        val expected = """Deep Learning (with PyTorch) and Neural Networks\Models and Architectures"""
        assertEquals(expected, PaperMetadataExtractor.cleanPdfString(raw))
    }

    @Test
    fun `decodePdfHexString decodes UTF-16BE hex strings`() {
        // "Attention" in UTF-16BE hex with FEFF BOM:
        // A (0041), t (0074), t (0074), e (0065), n (006E), t (0074), i (0069), o (006F), n (006E)
        val hex = "FEFF0041007400740065006E00740069006F006E"
        val decoded = PaperMetadataExtractor.decodePdfHexString(hex)
        assertEquals("Attention", decoded)
    }

    @Test
    fun `cleanFilenameToTitle converts filenames to readable titles`() {
        assertEquals(
            "Attention Is All You Need",
            PaperMetadataExtractor.cleanFilenameToTitle("Attention_Is_All_You_Need.pdf")
        )
        assertEquals(
            "quantum computing advances 2024",
            PaperMetadataExtractor.cleanFilenameToTitle("quantum-computing-advances-2024.docx")
        )
    }

    @Test
    fun `extractFromFile derives title from filename when file has no metadata`() {
        val tempFile = File.createTempFile("Graph_Neural_Networks_Survey", ".pdf")
        try {
            tempFile.writeText("Non-pdf dummy text")
            val extracted = kotlinx.coroutines.runBlocking {
                PaperMetadataExtractor.extractFromFile(tempFile, "Graph_Neural_Networks_Survey.pdf")
            }
            assertNotNull(extracted)
            assertEquals("Graph Neural Networks Survey", extracted.title)
        } finally {
            tempFile.delete()
        }
    }
}
