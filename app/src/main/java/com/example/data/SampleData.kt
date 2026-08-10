package com.example.data

/**
 * Mock content for the screens that are not yet backed by a real data source.
 *
 * Sample data used to live as top-level vals next to the screen that rendered it. Detail
 * screens need to resolve an id coming from a navigation argument, so the fixtures and their
 * lookups are consolidated here instead. [SavedPaper] is deliberately absent: it is a real
 * Room entity and is served by [PaperRepository].
 */

data class AcademicField(
    val id: String,
    val name: String,
    val researcherCount: Int,
    val description: String,
    val activeTopics: List<String>,
)

data class ReadingListFolder(
    val id: String,
    val title: String,
    val description: String,
    val paperCount: Int,
    val isPrivate: Boolean,
    val entries: List<ReadingListEntry> = emptyList(),
)

data class ReadingListEntry(
    val id: String,
    val title: String,
    val authors: String,
    val year: String,
    val note: String,
)

data class Opportunity(
    val id: String,
    val type: String, // Grant, Academic Job, Call for Papers
    val title: String,
    val institution: String,
    val deadline: String,
    val description: String,
    val eligibility: List<String> = emptyList(),
    val contact: String = "",
)

data class CitationNotification(
    val id: String,
    val summary: String,
    val paperTitle: String,
    val paperMeta: String,
    val quote: String,
    val citingAuthorName: String,
    val citingAuthorInitials: String,
    val citingAffiliation: String,
    val citingField: String,
    val referencedSection: String,
    val isRead: Boolean,
)

object SampleData {

    val fields = listOf(
        AcademicField(
            id = "theoretical-physics",
            name = "Theoretical Physics",
            researcherCount = 120,
            description = "Foundational work on the mathematical structures underlying physical law, " +
                "from thermodynamics to quantum information.",
            activeTopics = listOf("Entropy", "Quantum Information", "Statistical Mechanics"),
        ),
        AcademicField(
            id = "molecular-biology",
            name = "Molecular Biology",
            researcherCount = 240,
            description = "The molecular basis of biological activity, spanning gene expression, " +
                "protein folding and cellular signalling.",
            activeTopics = listOf("Gene Expression", "Protein Folding", "CRISPR"),
        ),
        AcademicField(
            id = "ancient-history",
            name = "Ancient History",
            researcherCount = 360,
            description = "Archival and archaeological scholarship on the ancient world and the " +
                "transmission of its written record.",
            activeTopics = listOf("Epigraphy", "Manuscript Transmission", "Numismatics"),
        ),
        AcademicField(
            id = "computational-linguistics",
            name = "Computational Linguistics",
            researcherCount = 480,
            description = "Formal and statistical models of language, increasingly shaped by the " +
                "semantics of large language models.",
            activeTopics = listOf("LLM Semantics", "Parsing", "Low-Resource Languages"),
        ),
        AcademicField(
            id = "cognitive-science",
            name = "Cognitive Science",
            researcherCount = 600,
            description = "Interdisciplinary study of mind and intelligence across psychology, " +
                "neuroscience and philosophy.",
            activeTopics = listOf("Embodied Cognition", "Memory", "Decision Making"),
        ),
        AcademicField(
            id = "macroeconomics",
            name = "Macroeconomics",
            researcherCount = 720,
            description = "Aggregate economic behaviour, monetary policy and the modelling of " +
                "long-run growth.",
            activeTopics = listOf("Monetary Policy", "Growth Models", "Labour Markets"),
        ),
    )

    val readingLists = listOf(
        ReadingListFolder(
            id = "1",
            title = "LLM Epistemology",
            description = "Papers covering the semantic shifts in large models.",
            paperCount = 12,
            isPrivate = true,
            entries = listOf(
                ReadingListEntry(
                    id = "1-1",
                    title = "Semantic Structures in LLMs",
                    authors = "Doe, J.",
                    year = "2026",
                    note = "Core reference for the latent representation argument.",
                ),
                ReadingListEntry(
                    id = "1-2",
                    title = "Latent Knowledge and Its Discontents",
                    authors = "Okafor, A.; Lindqvist, M.",
                    year = "2025",
                    note = "Useful counterpoint on probing methodology.",
                ),
                ReadingListEntry(
                    id = "1-3",
                    title = "On the Stability of Learned Abstractions",
                    authors = "Reyes, C.",
                    year = "2025",
                    note = "Section 3 has the clearest formalisation.",
                ),
            ),
        ),
        ReadingListFolder(
            id = "2",
            title = "Thermodynamics in Archival Tech",
            description = "Foundational texts for my upcoming grant proposal.",
            paperCount = 4,
            isPrivate = false,
            entries = listOf(
                ReadingListEntry(
                    id = "2-1",
                    title = "Entropy and the Architecture of Distributed Knowledge Systems",
                    authors = "Thorne, J.",
                    year = "2023",
                    note = "The paper the whole proposal responds to.",
                ),
                ReadingListEntry(
                    id = "2-2",
                    title = "Decay Models for Long-Lived Archives",
                    authors = "Basu, R.",
                    year = "2022",
                    note = "Empirical grounding for section 4.",
                ),
            ),
        ),
        ReadingListFolder(
            id = "3",
            title = "Decentralized Science",
            description = "DAO structures and reputation mechanics.",
            paperCount = 28,
            isPrivate = false,
            entries = listOf(
                ReadingListEntry(
                    id = "3-1",
                    title = "Reputation Without Institutions",
                    authors = "Nakamura, S.",
                    year = "2024",
                    note = "Survey; good bibliography.",
                ),
                ReadingListEntry(
                    id = "3-2",
                    title = "Governance Models for Research Collectives",
                    authors = "Adeyemi, T.; Park, H.",
                    year = "2024",
                    note = "Compare with the funding-body critique.",
                ),
            ),
        ),
    )

    val opportunities = listOf(
        Opportunity(
            id = "1",
            type = "Grant",
            title = "Early Career Researcher Fellowship",
            institution = "NSF",
            deadline = "Deadline: Nov 15, 2026",
            description = "Funding for innovative theoretical physics research.",
            eligibility = listOf(
                "Within 5 years of doctoral award",
                "Affiliated with a recognised research institution",
                "No prior NSF fellowship in the same category",
            ),
            contact = "fellowships@nsf.example",
        ),
        Opportunity(
            id = "2",
            type = "Academic Job",
            title = "Assistant Professor in Computational Linguistics",
            institution = "Stanford University",
            deadline = "Review begins: Oct 1, 2026",
            description = "Seeking candidates with strong publication records in LLM semantics.",
            eligibility = listOf(
                "PhD in linguistics, computer science or a related field",
                "Demonstrated teaching experience",
                "Active publication record in the last three years",
            ),
            contact = "linguistics-search@stanford.example",
        ),
        Opportunity(
            id = "3",
            type = "Call for Papers",
            title = "Special Issue: Entropy in Distributed Systems",
            institution = "Journal of Archival Science",
            deadline = "Submission: Dec 10, 2026",
            description = "We invite papers exploring localized data cluster architectures.",
            eligibility = listOf(
                "Original unpublished work",
                "Maximum 8000 words excluding references",
                "Double-blind review",
            ),
            contact = "specialissue@archivalscience.example",
        ),
    )

    val opportunityFilters = listOf("All", "Grant", "Academic Job", "Call for Papers")

    val notifications = listOf(
        CitationNotification(
            id = "n1",
            summary = "Your publication has been formally referenced by Dr. Julian Thorne in a new " +
                "preprint released to the Theoretical Physics circle.",
            paperTitle = "Entropy and the Architecture of Distributed Knowledge Systems",
            paperMeta = "Published Oct 2023 • ID: CC-882-XJ",
            quote = "\"...as proposed in Thorne's recent synthesis, the friction within localized data " +
                "clusters mirrors the thermodynamic decay observed in early archival structures " +
                "(Thorne, 2023).\"",
            citingAuthorName = "Dr. Julian Thorne",
            citingAuthorInitials = "JT",
            citingAffiliation = "CERN",
            citingField = "Theoretical Physics",
            referencedSection = "Section 4.2: Thermodynamic Decay in Archival Structures",
            isRead = false,
        ),
        CitationNotification(
            id = "n2",
            summary = "Your publication was cited in a review article circulated to the " +
                "Computational Linguistics circle.",
            paperTitle = "Semantic Structures in LLMs",
            paperMeta = "Published Jan 2026 • ID: CC-901-QR",
            quote = "\"Doe's account of latent representation drift remains the clearest statement " +
                "of the problem we take up here (Doe, 2026).\"",
            citingAuthorName = "Dr. Amara Okafor",
            citingAuthorInitials = "AO",
            citingAffiliation = "MIT",
            citingField = "Computational Linguistics",
            referencedSection = "Section 2.1: Representation Drift",
            isRead = true,
        ),
    )

    fun fieldById(id: String?): AcademicField? = fields.firstOrNull { it.id == id }

    fun readingListById(id: String?): ReadingListFolder? = readingLists.firstOrNull { it.id == id }

    fun opportunityById(id: String?): Opportunity? = opportunities.firstOrNull { it.id == id }

    fun notificationById(id: String?): CitationNotification? = notifications.firstOrNull { it.id == id }
}
