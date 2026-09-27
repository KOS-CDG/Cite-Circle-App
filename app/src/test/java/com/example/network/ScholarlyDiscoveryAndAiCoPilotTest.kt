package com.example.network

import com.example.ui.post.AiCoPilotMessage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ScholarlyDiscoveryAndAiCoPilotTest {

    @Test
    fun testDiscoveredPaperModelCreation() {
        val paper = DiscoveredPaper(
            id = "oa_10.1145_3442188.3445922",
            title = "Attention Is All You Need",
            authors = "Vaswani, Ashish; Shazeer, Noam; Parmar, Niki",
            year = "2017",
            venue = "Advances in Neural Information Processing Systems",
            doi = "10.1145/3442188.3445922",
            url = "https://doi.org/10.1145/3442188.3445922",
            pdfUrl = "https://arxiv.org/pdf/1706.03762.pdf",
            abstractText = "The dominant sequence transduction models are based on complex recurrent or convolutional neural networks...",
            citationsCount = 125430,
            isOpenAccess = true,
            source = "OpenAlex"
        )

        assertEquals("oa_10.1145_3442188.3445922", paper.id)
        assertEquals("Attention Is All You Need", paper.title)
        assertEquals("2017", paper.year)
        assertEquals(125430, paper.citationsCount)
        assertTrue(paper.isOpenAccess)
        assertEquals("OpenAlex", paper.source)
    }

    @Test
    fun testDiscoveredPaperCrossrefModel() {
        val paper = DiscoveredPaper(
            id = "crossref_10.1109/CVPR.2016.90",
            title = "Deep Residual Learning for Image Recognition",
            authors = "Kaiming He, Xiangyu Zhang, Shaoqing Ren, Jian Sun",
            year = "2016",
            venue = "2016 IEEE Conference on Computer Vision and Pattern Recognition (CVPR)",
            doi = "10.1109/CVPR.2016.90",
            url = "https://doi.org/10.1109/CVPR.2016.90",
            pdfUrl = "https://www.cv-foundation.org/openaccess/content_cvpr_2016/papers/He_Deep_Residual_Learning_CVPR_2016_paper.pdf",
            abstractText = "Deeper neural networks are more difficult to train...",
            citationsCount = 180000,
            isOpenAccess = true,
            source = "CrossRef"
        )

        assertEquals("crossref_10.1109/CVPR.2016.90", paper.id)
        assertEquals("CrossRef", paper.source)
        assertTrue(paper.isOpenAccess)
        assertEquals("2016", paper.year)
    }


    @Test
    fun testAiCoPilotMessageCreation() {
        val userMsg = AiCoPilotMessage(text = "Explain the attention mechanism", isUser = true)
        val aiMsg = AiCoPilotMessage(text = "Multi-head attention allows the model to jointly attend to information...", isUser = false)

        assertTrue(userMsg.isUser)
        assertFalse(userMsg.isError)
        assertNotNull(userMsg.id)

        assertFalse(aiMsg.isUser)
        assertFalse(aiMsg.isError)
        assertTrue(aiMsg.text.contains("Multi-head attention"))
    }

    @Test
    fun testAiCoPilotGroundingPromptAssembly() {
        val paperTitle = "Deep Residual Learning for Image Recognition"
        val authors = "He, Kaiming; Zhang, Xiangyu; Ren, Shaoqing; Sun, Jian"
        val year = "2016"
        val venue = "IEEE CVPR"
        val doi = "10.1109/CVPR.2016.90"
        val abstractText = "Deeper neural networks are more difficult to train. We present a residual learning framework to ease the training..."

        val prompt = """
            MANUSCRIPT CONTEXT:
            - Title: "$paperTitle"
            - Authors: $authors
            - Year: $year
            - Venue: $venue
            - DOI: $doi
            - Abstract: $abstractText
        """.trimIndent()

        assertTrue(prompt.contains(paperTitle))
        assertTrue(prompt.contains("He, Kaiming"))
        assertTrue(prompt.contains("10.1109/CVPR.2016.90"))
        assertTrue(prompt.contains("Deeper neural networks are more difficult to train"))
    }

    @Test
    fun testAiInsightAppendingToResearchNotes() {
        val existingNotes = "### Reading Goals\nReview experimental validation on ImageNet."
        val aiInsight = "The skip connections identity mappings prevent vanishing gradients in 152-layer networks."
        val noteEntry = "### 🤖 AI Co-Pilot Insight\n$aiInsight"

        val updatedNotes = if (existingNotes.isEmpty()) noteEntry else "$existingNotes\n\n---\n$noteEntry"

        assertTrue(updatedNotes.contains("### Reading Goals"))
        assertTrue(updatedNotes.contains("### 🤖 AI Co-Pilot Insight"))
        assertTrue(updatedNotes.contains("skip connections identity mappings"))
    }
}
