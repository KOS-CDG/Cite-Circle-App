package com.example.network

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.net.URLEncoder
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern

/**
 * Discovered academic paper from OpenAlex or arXiv global scholarly catalogs.
 */
data class DiscoveredPaper(
    val id: String,
    val title: String,
    val authors: String,
    val year: String,
    val venue: String,
    val doi: String,
    val url: String,
    val pdfUrl: String = "",
    val abstractText: String = "",
    val citationsCount: Int = 0,
    val isOpenAccess: Boolean = false,
    val source: String = "OpenAlex" // "OpenAlex" or "arXiv"
)

/**
 * High-speed scholarly discovery engine querying 250M+ open-access papers, preprints,
 * and journal articles across OpenAlex and arXiv APIs.
 */
object ScholarlyDiscoveryService {

    private const val TAG = "ScholarlyDiscovery"
    private const val OPENALEX_API = "https://api.openalex.org/works"
    private const val ARXIV_API = "https://export.arxiv.org/api/query"
    private const val CROSSREF_API = "https://api.crossref.org/works"

    private val httpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .addInterceptor { chain ->
                val req = chain.request().newBuilder()
                    .header("User-Agent", "CiteCircleApp/2.4 (https://cite.circle; mailto:support@citecircle.org)")
                    .build()
                chain.proceed(req)
            }
            .build()
    }

    /**
     * Executes parallel search across Crossref, arXiv, and OpenAlex, combining and ranking results.
     */
    suspend fun search(query: String, maxResultsPerSource: Int = 15): List<DiscoveredPaper> = withContext(Dispatchers.IO) {
        val trimmed = query.trim()
        if (trimmed.isBlank()) return@withContext emptyList()

        coroutineScope {
            val crossrefDeferred = async { searchCrossref(trimmed, maxResultsPerSource) }
            val arxivDeferred = async { searchArxiv(trimmed, maxResultsPerSource) }
            val openAlexDeferred = async { searchOpenAlex(trimmed, maxResultsPerSource) }

            val crossrefResults = try {
                crossrefDeferred.await()
            } catch (e: Exception) {
                Log.w(TAG, "Crossref query failed", e)
                emptyList()
            }

            val arxivResults = try {
                arxivDeferred.await()
            } catch (e: Exception) {
                Log.w(TAG, "arXiv query failed", e)
                emptyList()
            }

            val openAlexResults = try {
                openAlexDeferred.await()
            } catch (e: Exception) {
                Log.w(TAG, "OpenAlex query failed", e)
                emptyList()
            }

            // Merge, prioritize open access, and remove duplicates by normalized title
            val seenTitles = mutableSetOf<String>()
            val combined = mutableListOf<DiscoveredPaper>()

            // Interleave results to present diverse sources: Crossref, arXiv, OpenAlex
            val maxLen = maxOf(crossrefResults.size, arxivResults.size, openAlexResults.size)
            for (i in 0 until maxLen) {
                if (i < crossrefResults.size) {
                    val p = crossrefResults[i]
                    val norm = normalizeTitle(p.title)
                    if (norm.isNotBlank() && seenTitles.add(norm)) {
                        combined.add(p)
                    }
                }
                if (i < arxivResults.size) {
                    val p = arxivResults[i]
                    val norm = normalizeTitle(p.title)
                    if (norm.isNotBlank() && seenTitles.add(norm)) {
                        combined.add(p)
                    }
                }
                if (i < openAlexResults.size) {
                    val p = openAlexResults[i]
                    val norm = normalizeTitle(p.title)
                    if (norm.isNotBlank() && seenTitles.add(norm)) {
                        combined.add(p)
                    }
                }
            }

            combined
        }
    }

    /**
     * Queries OpenAlex Works endpoint with relevance sorting.
     */
    fun searchOpenAlex(query: String, limit: Int = 15): List<DiscoveredPaper> {
        try {
            val encodedQuery = URLEncoder.encode(query, "UTF-8")
            val url = "$OPENALEX_API?search=$encodedQuery&per-page=$limit&sort=relevance_score:desc"

            val request = Request.Builder().url(url).get().build()
            val response = httpClient.newCall(request).execute()
            if (!response.isSuccessful) return emptyList()

            val body = response.body?.string().orEmpty()
            if (body.isBlank()) return emptyList()

            val root = JSONObject(body)
            val results = root.optJSONArray("results") ?: return emptyList()
            val papers = mutableListOf<DiscoveredPaper>()

            for (i in 0 until results.length()) {
                val item = results.optJSONObject(i) ?: continue
                val rawTitle = item.optString("title", "").trim()
                if (rawTitle.isBlank()) continue

                val id = item.optString("id", "openalex_${System.currentTimeMillis()}_$i")
                val year = item.optInt("publication_year", 0).let { if (it > 0) it.toString() else "" }
                val citations = item.optInt("cited_by_count", 0)

                // Authors
                val authorships = item.optJSONArray("authorships")
                val authorList = mutableListOf<String>()
                if (authorships != null) {
                    for (j in 0 until minOf(authorships.length(), 6)) {
                        val authObj = authorships.optJSONObject(j)
                        val author = authObj?.optJSONObject("author")
                        val name = author?.optString("display_name", "")?.trim().orEmpty()
                        if (name.isNotBlank()) authorList.add(name)
                    }
                }
                val authors = if (authorList.isNotEmpty()) {
                    if (authorships != null && authorships.length() > 6) "${authorList.joinToString(", ")} et al."
                    else authorList.joinToString(", ")
                } else "Unknown Authors"

                // Venue
                val primaryLoc = item.optJSONObject("primary_location")
                val source = primaryLoc?.optJSONObject("source")
                val venue = source?.optString("display_name", "")?.trim().orEmpty()

                // Open Access & PDF
                val openAccess = item.optJSONObject("open_access")
                val isOa = openAccess?.optBoolean("is_oa", false) ?: false
                val bestOa = item.optJSONObject("best_oa_location")
                val bestPdf = bestOa?.optString("pdf_url", "").orEmpty()
                val primaryPdf = primaryLoc?.optString("pdf_url", "").orEmpty()
                val oaUrl = openAccess?.optString("oa_url", "").orEmpty()

                val pdfUrl = when {
                    bestPdf.isNotBlank() -> bestPdf
                    primaryPdf.isNotBlank() -> primaryPdf
                    oaUrl.endsWith(".pdf", ignoreCase = true) -> oaUrl
                    else -> ""
                }

                // DOI & Landing URL
                val rawDoi = item.optString("doi", "")
                val bareDoi = AcademicPaperResolver.cleanDoi(rawDoi)
                val landingUrl = if (rawDoi.isNotBlank()) rawDoi else "https://openalex.org/$id"

                // Abstract
                val invertedIndex = item.optJSONObject("abstract_inverted_index")
                val abstractText = if (invertedIndex != null) {
                    AcademicPaperResolver.reconstructOpenAlexAbstract(invertedIndex)
                } else ""

                papers.add(
                    DiscoveredPaper(
                        id = "oa_${bareDoi.ifBlank { id.substringAfterLast("/") }}",
                        title = rawTitle,
                        authors = authors,
                        year = year,
                        venue = venue.ifBlank { "Scholarly Publication" },
                        doi = bareDoi,
                        url = landingUrl,
                        pdfUrl = pdfUrl,
                        abstractText = abstractText,
                        citationsCount = citations,
                        isOpenAccess = isOa || pdfUrl.isNotBlank(),
                        source = "OpenAlex"
                    )
                )
            }
            return papers
        } catch (e: Exception) {
            Log.w(TAG, "searchOpenAlex failed for '$query'", e)
            return emptyList()
        }
    }

    /**
     * Queries Crossref Works endpoint for DOI-indexed academic literature.
     */
    fun searchCrossref(query: String, limit: Int = 15): List<DiscoveredPaper> {
        try {
            val encodedQuery = URLEncoder.encode(query, "UTF-8")
            val url = "$CROSSREF_API?query=$encodedQuery&rows=$limit&sort=relevance"

            val request = Request.Builder().url(url).get().build()
            val response = httpClient.newCall(request).execute()
            if (!response.isSuccessful) return emptyList()

            val body = response.body?.string().orEmpty()
            if (body.isBlank()) return emptyList()

            val root = JSONObject(body)
            val message = root.optJSONObject("message") ?: return emptyList()
            val items = message.optJSONArray("items") ?: return emptyList()
            val papers = mutableListOf<DiscoveredPaper>()

            for (i in 0 until items.length()) {
                val item = items.optJSONObject(i) ?: continue
                val titleArray = item.optJSONArray("title")
                val rawTitle = if (titleArray != null && titleArray.length() > 0) {
                    titleArray.optString(0, "").trim()
                } else ""
                if (rawTitle.isBlank()) continue

                val doi = item.optString("DOI", "").trim()
                val id = if (doi.isNotBlank()) "crossref_$doi" else "crossref_${System.currentTimeMillis()}_$i"
                val landingUrl = item.optString("URL", if (doi.isNotBlank()) "https://doi.org/$doi" else "")

                // Year
                var year = ""
                val issued = item.optJSONObject("issued") ?: item.optJSONObject("published-print") ?: item.optJSONObject("published-online")
                val dateParts = issued?.optJSONArray("date-parts")
                if (dateParts != null && dateParts.length() > 0) {
                    val firstDate = dateParts.optJSONArray(0)
                    if (firstDate != null && firstDate.length() > 0) {
                        val y = firstDate.optInt(0, 0)
                        if (y > 0) year = y.toString()
                    }
                }

                // Citations
                val citations = item.optInt("is-referenced-by-count", 0)

                // Authors
                val authorsArr = item.optJSONArray("author")
                val authorList = mutableListOf<String>()
                if (authorsArr != null) {
                    for (j in 0 until minOf(authorsArr.length(), 6)) {
                        val authObj = authorsArr.optJSONObject(j) ?: continue
                        val given = authObj.optString("given", "").trim()
                        val family = authObj.optString("family", "").trim()
                        val fullName = when {
                            given.isNotBlank() && family.isNotBlank() -> "$given $family"
                            family.isNotBlank() -> family
                            given.isNotBlank() -> given
                            else -> authObj.optString("name", "").trim()
                        }
                        if (fullName.isNotBlank()) authorList.add(fullName)
                    }
                }
                val authors = if (authorList.isNotEmpty()) {
                    if (authorsArr != null && authorsArr.length() > 6) "${authorList.joinToString(", ")} et al."
                    else authorList.joinToString(", ")
                } else "Scholarly Authors"

                // Venue
                val containerArr = item.optJSONArray("container-title")
                val venue = if (containerArr != null && containerArr.length() > 0) {
                    containerArr.optString(0, "").trim()
                } else item.optString("publisher", "").trim()

                // PDF URL from link array
                var pdfUrl = ""
                val linkArr = item.optJSONArray("link")
                if (linkArr != null) {
                    for (k in 0 until linkArr.length()) {
                        val linkObj = linkArr.optJSONObject(k) ?: continue
                        val contentType = linkObj.optString("content-type", "")
                        if (contentType.contains("pdf", ignoreCase = true)) {
                            pdfUrl = linkObj.optString("URL", "")
                            if (pdfUrl.isNotBlank()) break
                        }
                    }
                }

                // Abstract
                val rawAbstract = item.optString("abstract", "")
                val cleanAbstract = if (rawAbstract.isNotBlank()) {
                    rawAbstract.replace(Regex("<[^>]+>"), " ").replace(Regex("\\s+"), " ").trim()
                } else ""

                val isOpenAccess = pdfUrl.isNotBlank()

                papers.add(
                    DiscoveredPaper(
                        id = id,
                        title = rawTitle,
                        authors = authors,
                        year = year,
                        venue = venue,
                        doi = doi,
                        url = landingUrl,
                        pdfUrl = pdfUrl,
                        abstractText = cleanAbstract,
                        citationsCount = citations,
                        isOpenAccess = isOpenAccess,
                        source = "CrossRef"
                    )
                )
            }
            return papers
        } catch (e: Exception) {
            Log.w(TAG, "searchCrossref failed for '$query'", e)
            return emptyList()
        }
    }

    /**
     * Queries arXiv Atom API for relevant preprints.
     */
    fun searchArxiv(query: String, limit: Int = 15): List<DiscoveredPaper> {
        try {
            val encodedQuery = URLEncoder.encode(query, "UTF-8")
            val url = "$ARXIV_API?search_query=all:$encodedQuery&start=0&max_results=$limit&sortBy=relevance&sortOrder=descending"

            val request = Request.Builder().url(url).get().build()
            val response = httpClient.newCall(request).execute()
            if (!response.isSuccessful) return emptyList()

            val xml = response.body?.string().orEmpty()
            if (xml.isBlank() || !xml.contains("<entry>")) return emptyList()

            val papers = mutableListOf<DiscoveredPaper>()
            val entryPattern = Pattern.compile("<entry>(.*?)</entry>", Pattern.DOTALL)
            val matcher = entryPattern.matcher(xml)

            while (matcher.find()) {
                val entryXml = matcher.group(1) ?: continue

                // Title
                val titleMatcher = Pattern.compile("<title>(.*?)</title>", Pattern.DOTALL).matcher(entryXml)
                val rawTitle = if (titleMatcher.find()) {
                    titleMatcher.group(1)?.replace(Regex("\\s+"), " ")?.trim().orEmpty()
                } else ""
                if (rawTitle.isBlank()) continue

                // ID / arXiv ID
                val idMatcher = Pattern.compile("<id>http://arxiv.org/abs/(.*?)</id>").matcher(entryXml)
                val arxivId = if (idMatcher.find()) idMatcher.group(1)?.trim().orEmpty() else ""

                // Summary / Abstract
                val summaryMatcher = Pattern.compile("<summary>(.*?)</summary>", Pattern.DOTALL).matcher(entryXml)
                val summary = if (summaryMatcher.find()) {
                    summaryMatcher.group(1)?.replace(Regex("\\s+"), " ")?.trim().orEmpty()
                } else ""

                // Year
                val publishedMatcher = Pattern.compile("<published>(\\d{4})").matcher(entryXml)
                val year = if (publishedMatcher.find()) publishedMatcher.group(1).orEmpty() else ""

                // Authors
                val authorMatcher = Pattern.compile("<author>\\s*<name>(.*?)</name>\\s*</author>", Pattern.DOTALL).matcher(entryXml)
                val authorList = mutableListOf<String>()
                while (authorMatcher.find()) {
                    val aName = authorMatcher.group(1)?.trim().orEmpty()
                    if (aName.isNotBlank()) authorList.add(aName)
                }
                val authors = if (authorList.isNotEmpty()) {
                    if (authorList.size > 5) "${authorList.take(5).joinToString(", ")} et al."
                    else authorList.joinToString(", ")
                } else "arXiv Contributors"

                val pdfUrl = if (arxivId.isNotBlank()) "https://arxiv.org/pdf/$arxivId.pdf" else ""
                val landingUrl = if (arxivId.isNotBlank()) "https://arxiv.org/abs/$arxivId" else ""

                papers.add(
                    DiscoveredPaper(
                        id = "arxiv_${arxivId.ifBlank { System.currentTimeMillis().toString() }}",
                        title = rawTitle,
                        authors = authors,
                        year = year,
                        venue = "arXiv Preprint (${if (arxivId.isNotBlank()) "arXiv:$arxivId" else "Preprint"})",
                        doi = if (arxivId.isNotBlank()) "10.48550/arXiv.$arxivId" else "",
                        url = landingUrl,
                        pdfUrl = pdfUrl,
                        abstractText = summary,
                        citationsCount = 0,
                        isOpenAccess = true,
                        source = "arXiv"
                    )
                )
            }
            return papers
        } catch (e: Exception) {
            Log.w(TAG, "searchArxiv failed for '$query'", e)
            return emptyList()
        }
    }

    private fun normalizeTitle(title: String): String {
        return title.lowercase().replace(Regex("[^a-z0-9]"), "")
    }
}
