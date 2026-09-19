package com.example.network

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern

/**
 * Resolved metadata from public academic catalog APIs (Crossref, OpenAlex, and arXiv).
 */
data class ResolvedPaperMetadata(
    val title: String,
    val authors: String,
    val year: String,
    val venue: String,
    val doi: String,
    val url: String,
    val pdfUrl: String = "",
    val abstractText: String = "",
    val isOpenAccess: Boolean = false
)

/**
 * Autonomously resolves academic paper metadata, citations, abstracts, and open-access PDFs
 * from DOI and arXiv identifiers using Crossref, OpenAlex, and arXiv APIs.
 */
object AcademicPaperResolver {

    private const val TAG = "AcademicPaperResolver"
    private const val CROSSREF_BASE = "https://api.crossref.org/works/"
    private const val OPENALEX_BASE = "https://api.openalex.org/works/"

    private val httpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .addInterceptor { chain ->
                val req = chain.request().newBuilder()
                    .header("User-Agent", "CiteCircle/1.0 (https://cite.circle; mailto:support@cite.circle)")
                    .build()
                chain.proceed(req)
            }
            .build()
    }

    /**
     * Cleans an input DOI or URL into a bare "10.xxxx/yyyy".
     */
    fun cleanDoi(raw: String): String {
        var doi = raw.trim()
        val prefixes = listOf("https://doi.org/", "http://doi.org/", "https://dx.doi.org/", "doi:", "DOI:")
        for (prefix in prefixes) {
            if (doi.startsWith(prefix, ignoreCase = true)) {
                doi = doi.substring(prefix.length).trim()
            }
        }
        return doi
    }

    /**
     * Extracts arXiv ID if the string represents an arXiv paper.
     * e.g., "arXiv:2301.07041", "2301.07041", "https://arxiv.org/abs/2301.07041"
     */
    fun extractArxivId(raw: String): String? {
        val trimmed = raw.trim()
        val pattern = Pattern.compile("(?:arxiv\\.org/(?:abs|pdf)/|arxiv:)?(\\d{4}\\.\\d{4,5}(?:v\\d+)?)", Pattern.CASE_INSENSITIVE)
        val matcher = pattern.matcher(trimmed)
        return if (matcher.find()) matcher.group(1) else null
    }

    /**
     * Strips XML/HTML markup (such as JATS XML tags in Crossref abstracts)
     * and decodes common HTML entities.
     */
    fun stripHtmlAndJats(raw: String): String {
        if (raw.isBlank()) return ""
        val withoutTags = raw.replace(Regex("<[^>]+>"), " ")
        return withoutTags
            .replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&quot;", "\"")
            .replace("&#39;", "'")
            .replace("&apos;", "'")
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    /**
     * Reconstructs human-readable text from OpenAlex's abstract_inverted_index.
     */
    fun reconstructOpenAlexAbstract(invertedIndex: JSONObject): String {
        val positions = mutableMapOf<Int, String>()
        val keys = invertedIndex.keys()
        while (keys.hasNext()) {
            val word = keys.next()
            val indices = invertedIndex.optJSONArray(word) ?: continue
            for (i in 0 until indices.length()) {
                positions[indices.getInt(i)] = word
            }
        }
        if (positions.isEmpty()) return ""
        val maxPos = positions.keys.maxOrNull() ?: return ""
        val words = (0..maxPos).mapNotNull { positions[it] }
        return words.joinToString(" ")
    }

    /**
     * Resolves metadata for [rawIdentifier] (DOI or arXiv ID) across Crossref and OpenAlex.
     */
    suspend fun resolveDoi(rawIdentifier: String): ResolvedPaperMetadata? = withContext(Dispatchers.IO) {
        val bareDoi = cleanDoi(rawIdentifier)
        val arxivId = extractArxivId(rawIdentifier)

        if (arxivId != null && (bareDoi.isBlank() || !bareDoi.startsWith("10."))) {
            return@withContext resolveArxiv(arxivId)
        }

        if (bareDoi.isBlank() || !bareDoi.startsWith("10.")) return@withContext null

        try {
            var metadata = resolveFromCrossref(bareDoi)

            // If Crossref lacks PDF or abstract, enrich via OpenAlex
            if (metadata == null || metadata.pdfUrl.isBlank() || metadata.abstractText.isBlank()) {
                val openAlexMeta = resolveFromOpenAlex(bareDoi)
                if (openAlexMeta != null) {
                    metadata = if (metadata == null) {
                        openAlexMeta
                    } else {
                        metadata.copy(
                            pdfUrl = if (metadata.pdfUrl.isNotBlank()) metadata.pdfUrl else openAlexMeta.pdfUrl,
                            abstractText = if (metadata.abstractText.isNotBlank()) metadata.abstractText else openAlexMeta.abstractText,
                            isOpenAccess = metadata.isOpenAccess || openAlexMeta.isOpenAccess,
                            venue = if (metadata.venue.isNotBlank()) metadata.venue else openAlexMeta.venue
                        )
                    }
                }
            }

            metadata
        } catch (e: Exception) {
            Log.e(TAG, "Error resolving DOI: $bareDoi", e)
            null
        }
    }

    private fun resolveFromCrossref(bareDoi: String): ResolvedPaperMetadata? {
        try {
            val url = "$CROSSREF_BASE$bareDoi"
            val request = Request.Builder().url(url).get().build()
            val response = httpClient.newCall(request).execute()

            if (!response.isSuccessful) {
                Log.w(TAG, "Crossref lookup failed with code ${response.code} for $bareDoi")
                return null
            }

            val body = response.body?.string().orEmpty()
            if (body.isBlank()) return null

            val root = JSONObject(body)
            val message = root.optJSONObject("message") ?: return null

            // Title
            val titles = message.optJSONArray("title")
            val title = if (titles != null && titles.length() > 0) titles.getString(0).trim() else ""

            // Authors
            val authorsArray = message.optJSONArray("author")
            val authorList = mutableListOf<String>()
            if (authorsArray != null) {
                for (i in 0 until authorsArray.length()) {
                    val a = authorsArray.getJSONObject(i)
                    val family = a.optString("family", "").trim()
                    val given = a.optString("given", "").trim()
                    if (family.isNotEmpty()) {
                        authorList.add(if (given.isNotEmpty()) "$family, $given" else family)
                    }
                }
            }
            val authors = authorList.joinToString("; ")

            // Publication Year
            var year = ""
            val published = message.optJSONObject("published") ?: message.optJSONObject("created")
            val dateParts = published?.optJSONArray("date-parts")
            if (dateParts != null && dateParts.length() > 0) {
                val firstDate = dateParts.optJSONArray(0)
                if (firstDate != null && firstDate.length() > 0) {
                    year = firstDate.optInt(0, 0).let { if (it > 0) it.toString() else "" }
                }
            }

            // Journal / Venue
            val containers = message.optJSONArray("container-title")
            val venue = if (containers != null && containers.length() > 0) containers.getString(0).trim() else ""

            // URL
            val resolvedUrl = message.optString("URL", "https://doi.org/$bareDoi").trim()

            // Abstract
            val rawAbstract = message.optString("abstract", "")
            val abstractText = stripHtmlAndJats(rawAbstract)

            // Direct PDF Link if available
            var pdfUrl = ""
            val links = message.optJSONArray("link")
            if (links != null) {
                for (i in 0 until links.length()) {
                    val linkObj = links.getJSONObject(i)
                    val contentType = linkObj.optString("content-type", "")
                    val linkUrl = linkObj.optString("URL", "")
                    if (contentType.contains("pdf", ignoreCase = true) && linkUrl.isNotBlank()) {
                        pdfUrl = linkUrl
                        break
                    }
                }
            }

            return ResolvedPaperMetadata(
                title = title,
                authors = authors,
                year = year,
                venue = venue,
                doi = bareDoi,
                url = resolvedUrl,
                pdfUrl = pdfUrl,
                abstractText = abstractText,
                isOpenAccess = pdfUrl.isNotBlank()
            )
        } catch (e: Exception) {
            Log.w(TAG, "Crossref parse error for $bareDoi", e)
            return null
        }
    }

    private fun resolveFromOpenAlex(bareDoi: String): ResolvedPaperMetadata? {
        try {
            val url = "${OPENALEX_BASE}https://doi.org/$bareDoi"
            val request = Request.Builder().url(url).get().build()
            val response = httpClient.newCall(request).execute()

            if (!response.isSuccessful) return null

            val body = response.body?.string().orEmpty()
            if (body.isBlank()) return null

            val root = JSONObject(body)
            val title = root.optString("title", "").trim()
            val publicationYear = root.optInt("publication_year", 0).let { if (it > 0) it.toString() else "" }

            // Authorships
            val authorships = root.optJSONArray("authorships")
            val authorList = mutableListOf<String>()
            if (authorships != null) {
                for (i in 0 until authorships.length()) {
                    val authObj = authorships.getJSONObject(i)
                    val author = authObj.optJSONObject("author")
                    val name = author?.optString("display_name", "")?.trim().orEmpty()
                    if (name.isNotBlank()) {
                        authorList.add(name)
                    }
                }
            }
            val authors = authorList.joinToString("; ")

            // Venue
            val primaryLoc = root.optJSONObject("primary_location")
            val source = primaryLoc?.optJSONObject("source")
            val venue = source?.optString("display_name", "")?.trim().orEmpty()

            // Open access and PDF
            val openAccessObj = root.optJSONObject("open_access")
            val isOa = openAccessObj?.optBoolean("is_oa", false) ?: false
            val oaUrl = openAccessObj?.optString("oa_url", "").orEmpty()

            val bestOa = root.optJSONObject("best_oa_location")
            val bestPdf = bestOa?.optString("pdf_url", "").orEmpty()
            val primaryPdf = primaryLoc?.optString("pdf_url", "").orEmpty()
            val pdfUrl = when {
                bestPdf.isNotBlank() -> bestPdf
                primaryPdf.isNotBlank() -> primaryPdf
                oaUrl.endsWith(".pdf", ignoreCase = true) -> oaUrl
                else -> oaUrl
            }

            // Abstract
            val invertedIndex = root.optJSONObject("abstract_inverted_index")
            val abstractText = if (invertedIndex != null) reconstructOpenAlexAbstract(invertedIndex) else ""

            val landingUrl = root.optString("doi", "https://doi.org/$bareDoi")

            return ResolvedPaperMetadata(
                title = title,
                authors = authors,
                year = publicationYear,
                venue = venue,
                doi = bareDoi,
                url = landingUrl,
                pdfUrl = pdfUrl,
                abstractText = abstractText,
                isOpenAccess = isOa || pdfUrl.isNotBlank()
            )
        } catch (e: Exception) {
            Log.w(TAG, "OpenAlex resolution failed for $bareDoi", e)
            return null
        }
    }

    private fun resolveArxiv(arxivId: String): ResolvedPaperMetadata {
        val pdfUrl = "https://arxiv.org/pdf/$arxivId.pdf"
        val landingUrl = "https://arxiv.org/abs/$arxivId"

        // Attempt to fetch title from OpenAlex for arXiv ID
        val openAlexMeta = resolveFromOpenAlex("10.48550/arXiv.$arxivId")
        if (openAlexMeta != null && openAlexMeta.title.isNotBlank()) {
            return openAlexMeta.copy(
                doi = "arXiv:$arxivId",
                url = landingUrl,
                pdfUrl = pdfUrl,
                isOpenAccess = true,
                venue = openAlexMeta.venue.ifBlank { "arXiv" }
            )
        }

        return ResolvedPaperMetadata(
            title = "arXiv:$arxivId",
            authors = "",
            year = "",
            venue = "arXiv",
            doi = "arXiv:$arxivId",
            url = landingUrl,
            pdfUrl = pdfUrl,
            abstractText = "",
            isOpenAccess = true
        )
    }
}
