package com.example.network

import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class AcademicPaperResolverTest {

    @Test
    fun `cleanDoi strips URL and doi prefixes`() {
        assertEquals("10.1038/s41586-020-2649-2", AcademicPaperResolver.cleanDoi("https://doi.org/10.1038/s41586-020-2649-2"))
        assertEquals("10.1038/s41586-020-2649-2", AcademicPaperResolver.cleanDoi("http://doi.org/10.1038/s41586-020-2649-2"))
        assertEquals("10.1038/s41586-020-2649-2", AcademicPaperResolver.cleanDoi("doi:10.1038/s41586-020-2649-2"))
        assertEquals("10.1038/s41586-020-2649-2", AcademicPaperResolver.cleanDoi("DOI: 10.1038/s41586-020-2649-2"))
        assertEquals("10.1038/s41586-020-2649-2", AcademicPaperResolver.cleanDoi(" 10.1038/s41586-020-2649-2 "))
    }

    @Test
    fun `extractArxivId correctly identifies arXiv identifiers`() {
        assertEquals("2301.07041", AcademicPaperResolver.extractArxivId("2301.07041"))
        assertEquals("2301.07041", AcademicPaperResolver.extractArxivId("arXiv:2301.07041"))
        assertEquals("2301.07041", AcademicPaperResolver.extractArxivId("https://arxiv.org/abs/2301.07041"))
        assertEquals("2301.07041", AcademicPaperResolver.extractArxivId("https://arxiv.org/pdf/2301.07041.pdf"))
        assertEquals("2301.07041v2", AcademicPaperResolver.extractArxivId("arXiv:2301.07041v2"))
        assertNull(AcademicPaperResolver.extractArxivId("10.1038/nature12345"))
    }

    @Test
    fun `stripHtmlAndJats strips markup and decodes entities`() {
        val raw = "<jats:p>This study demonstrates <b>significant</b> improvements &amp; discoveries in latent structures.</jats:p>"
        val expected = "This study demonstrates significant improvements & discoveries in latent structures."
        assertEquals(expected, AcademicPaperResolver.stripHtmlAndJats(raw))
    }

    @Test
    fun `reconstructOpenAlexAbstract reconstructs words in correct sequence`() {
        val index = JSONObject()
        index.put("The", JSONArray(listOf(0)))
        index.put("quick", JSONArray(listOf(1)))
        index.put("brown", JSONArray(listOf(2)))
        index.put("fox", JSONArray(listOf(3)))

        val text = AcademicPaperResolver.reconstructOpenAlexAbstract(index)
        assertEquals("The quick brown fox", text)
    }
}
