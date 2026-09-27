package com.example.data

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import com.example.network.AcademicPaperResolver
import com.example.network.ResolvedPaperMetadata
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.RandomAccessFile
import java.util.regex.Pattern

/**
 * Encapsulates bibliographic metadata extracted from an academic document (PDF/Word/Text)
 * or resolved from embedded DOIs / arXiv identifiers.
 */
data class ExtractedMetadata(
    val title: String = "",
    val authors: String = "",
    val year: String = "",
    val venue: String = "",
    val doi: String = "",
    val url: String = "",
    val pdfUrl: String = "",
    val abstractText: String = "",
    val isOpenAccess: Boolean = false,
    val source: String = ""
)

/**
 * Intelligent repository ingestion engine:
 * 1. Inspects uploaded PDF binary streams for embedded DOIs, arXiv IDs, and XMP/Info dictionaries.
 * 2. Cross-references detected identifiers against academic registry APIs (Crossref, arXiv, OpenAlex)
 *    to retrieve verified peer-reviewed metadata.
 * 3. Falls back gracefully to local PDF Info metadata or structured filename extraction when offline.
 */
object PaperMetadataExtractor {

    private const val TAG = "PaperMetadataExtractor"

    // Patterns for academic identifiers
    private val DOI_PATTERN = Pattern.compile(
        """(?:doi(?:\.org)?/|doi:\s*|DOI:\s*)?(10\.\d{4,9}/[a-zA-Z0-9.\-_;()/:]+)""",
        Pattern.CASE_INSENSITIVE
    )

    private val ARXIV_ID_PATTERN = Pattern.compile(
        """(?:arxiv(?:\.org/(?:abs|pdf)/|:|-|\s))?(\d{4}\.\d{4,5}(?:v\d+)?)""",
        Pattern.CASE_INSENSITIVE
    )

    // PDF Info Dictionary standard entries
    private val PDF_TITLE_LITERAL = Pattern.compile(
        """/Title\s*\(([^)\\]*(?:\\.[^)\\]*)*)\)""",
        Pattern.CASE_INSENSITIVE
    )
    private val PDF_TITLE_HEX = Pattern.compile(
        """/Title\s*<([0-9a-fA-F]+)>""",
        Pattern.CASE_INSENSITIVE
    )
    private val PDF_AUTHOR_LITERAL = Pattern.compile(
        """/Author\s*\(([^)\\]*(?:\\.[^)\\]*)*)\)""",
        Pattern.CASE_INSENSITIVE
    )
    private val PDF_DATE_PATTERN = Pattern.compile(
        """/CreationDate\s*\([D:]?(\d{4})""",
        Pattern.CASE_INSENSITIVE
    )

    // XMP Metadata entries
    private val XMP_PRISM_DOI = Pattern.compile(
        """<prism:doi>(10\.\d{4,9}/[^<]+)</prism:doi>""",
        Pattern.CASE_INSENSITIVE
    )
    private val XMP_DC_TITLE = Pattern.compile(
        """<dc:title>[\s\S]*?<rdf:li[^>]*>([^<]+)</rdf:li>""",
        Pattern.CASE_INSENSITIVE
    )
    private val XMP_DC_CREATOR = Pattern.compile(
        """<dc:creator>[\s\S]*?<rdf:li[^>]*>([^<]+)</rdf:li>""",
        Pattern.CASE_INSENSITIVE
    )
    private val XMP_DATE_PATTERN = Pattern.compile(
        """<(?:prism:publicationDate|dc:date)>(\d{4})""",
        Pattern.CASE_INSENSITIVE
    )

    /**
     * Resolves the user-visible display name of a chosen Uri from ContentResolver.
     */
    fun resolveDisplayName(context: Context, uri: Uri): String? {
        try {
            if (uri.scheme == "content") {
                context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        if (nameIndex != -1) return cursor.getString(nameIndex)
                    }
                }
            }
            return uri.lastPathSegment
        } catch (_: Exception) {
            return uri.lastPathSegment
        }
    }

    /**
     * Inspects a local paper file and extracts metadata or triggers online resolution.
     */
    suspend fun extractFromFile(
        file: File,
        originalName: String? = null
    ): ExtractedMetadata = withContext(Dispatchers.IO) {
        val fileName = originalName ?: file.name

        // 1. Check filename for obvious arXiv ID (e.g. "2301.07041.pdf")
        val arxivFromFilename = extractArxivIdentifier(fileName)
        if (arxivFromFilename != null) {
            try {
                val resolved = AcademicPaperResolver.resolveDoi(arxivFromFilename)
                if (resolved != null && resolved.title.isNotBlank()) {
                    return@withContext mapResolvedToExtracted(resolved, "arXiv ID from filename")
                }
            } catch (e: Exception) {
                Log.w(TAG, "Network resolution failed for filename arXiv $arxivFromFilename", e)
            }
        }

        // 2. Check filename for embedded DOI (e.g. "10.1038_s41586-020-2649-2.pdf")
        val doiFromFilename = extractDoiIdentifier(fileName)
        if (doiFromFilename != null) {
            try {
                val resolved = AcademicPaperResolver.resolveDoi(doiFromFilename)
                if (resolved != null && resolved.title.isNotBlank()) {
                    return@withContext mapResolvedToExtracted(resolved, "DOI from filename")
                }
            } catch (e: Exception) {
                Log.w(TAG, "Network resolution failed for filename DOI $doiFromFilename", e)
            }
        }

        if (!file.exists() || file.length() <= 0L) {
            return@withContext deriveFromFilenameOnly(fileName)
        }

        // 3. Scan PDF binary sample (first 128 KB + last 32 KB)
        val sampleText = readSampleText(file)
        if (sampleText.isBlank()) {
            return@withContext deriveFromFilenameOnly(fileName)
        }

        // Check for DOI in XMP packet first
        val xmpDoiMatcher = XMP_PRISM_DOI.matcher(sampleText)
        if (xmpDoiMatcher.find()) {
            val doi = sanitizeDoi(xmpDoiMatcher.group(1))
            if (doi.isNotBlank()) {
                val resolved = runCatching { AcademicPaperResolver.resolveDoi(doi) }.getOrNull()
                if (resolved != null && resolved.title.isNotBlank()) {
                    return@withContext mapResolvedToExtracted(resolved, "XMP Metadata DOI ($doi)")
                }
            }
        }

        // Check for arXiv ID in text sample
        val arxivMatcher = ARXIV_ID_PATTERN.matcher(sampleText)
        if (arxivMatcher.find()) {
            val id = arxivMatcher.group(1)
            if (!id.isNullOrBlank()) {
                val resolved = runCatching { AcademicPaperResolver.resolveDoi(id) }.getOrNull()
                if (resolved != null && resolved.title.isNotBlank()) {
                    return@withContext mapResolvedToExtracted(resolved, "Embedded arXiv ID (arXiv:$id)")
                }
            }
        }

        // Check for DOI anywhere in sample text
        val doiMatcher = DOI_PATTERN.matcher(sampleText)
        while (doiMatcher.find()) {
            val candidateDoi = sanitizeDoi(doiMatcher.group(1))
            if (candidateDoi.isNotBlank() && isValidDoiPrefix(candidateDoi)) {
                val resolved = runCatching { AcademicPaperResolver.resolveDoi(candidateDoi) }.getOrNull()
                if (resolved != null && resolved.title.isNotBlank()) {
                    return@withContext mapResolvedToExtracted(resolved, "Detected DOI ($candidateDoi)")
                }
            }
        }

        // 4. Fallback to local PDF Info / XMP dictionary metadata (offline-friendly)
        var localTitle = ""
        var localAuthor = ""
        var localYear = ""

        // XMP Title
        val xmpTitleMatcher = XMP_DC_TITLE.matcher(sampleText)
        if (xmpTitleMatcher.find()) {
            localTitle = cleanPdfString(xmpTitleMatcher.group(1).orEmpty())
        }

        // PDF Info Title
        if (localTitle.isBlank()) {
            val hexMatcher = PDF_TITLE_HEX.matcher(sampleText)
            if (hexMatcher.find()) {
                localTitle = decodePdfHexString(hexMatcher.group(1).orEmpty())
            }
        }
        if (localTitle.isBlank()) {
            val litMatcher = PDF_TITLE_LITERAL.matcher(sampleText)
            if (litMatcher.find()) {
                localTitle = cleanPdfString(litMatcher.group(1).orEmpty())
            }
        }

        // XMP / PDF Author
        val xmpAuthorMatcher = XMP_DC_CREATOR.matcher(sampleText)
        if (xmpAuthorMatcher.find()) {
            localAuthor = cleanPdfString(xmpAuthorMatcher.group(1).orEmpty())
        } else {
            val litAuthor = PDF_AUTHOR_LITERAL.matcher(sampleText)
            if (litAuthor.find()) {
                localAuthor = cleanPdfString(litAuthor.group(1).orEmpty())
            }
        }

        // Date
        val xmpDateMatcher = XMP_DATE_PATTERN.matcher(sampleText)
        if (xmpDateMatcher.find()) {
            localYear = xmpDateMatcher.group(1).orEmpty()
        } else {
            val litDate = PDF_DATE_PATTERN.matcher(sampleText)
            if (litDate.find()) {
                localYear = litDate.group(1).orEmpty()
            }
        }

        // Discard generic or placeholder titles
        if (isGenericTitle(localTitle)) {
            localTitle = ""
        }

        // Final title fallback: cleaned filename
        if (localTitle.isBlank()) {
            localTitle = cleanFilenameToTitle(fileName)
        }

        ExtractedMetadata(
            title = localTitle,
            authors = localAuthor,
            year = localYear,
            source = "PDF Document Properties"
        )
    }

    private fun mapResolvedToExtracted(meta: ResolvedPaperMetadata, source: String): ExtractedMetadata {
        return ExtractedMetadata(
            title = meta.title,
            authors = meta.authors,
            year = meta.year,
            venue = meta.venue,
            doi = meta.doi,
            url = meta.url,
            pdfUrl = meta.pdfUrl,
            abstractText = meta.abstractText,
            isOpenAccess = meta.isOpenAccess,
            source = source
        )
    }

    private fun deriveFromFilenameOnly(fileName: String): ExtractedMetadata {
        return ExtractedMetadata(
            title = cleanFilenameToTitle(fileName),
            source = "File Name Analysis"
        )
    }

    /**
     * Reads the start (128KB) and end (32KB) of a file as ISO-8859-1 string.
     */
    private fun readSampleText(file: File): String {
        return try {
            RandomAccessFile(file, "r").use { raf ->
                val len = raf.length()
                val headSize = minOf(len, 131072L).toInt()
                val headBytes = ByteArray(headSize)
                raf.readFully(headBytes)

                val headStr = String(headBytes, Charsets.ISO_8859_1)

                if (len > 131072L) {
                    val tailSize = minOf(len - headSize, 32768L).toInt()
                    raf.seek(len - tailSize)
                    val tailBytes = ByteArray(tailSize)
                    raf.readFully(tailBytes)
                    headStr + "\n" + String(tailBytes, Charsets.ISO_8859_1)
                } else {
                    headStr
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to read binary sample from ${file.name}", e)
            ""
        }
    }

    fun extractArxivIdentifier(raw: String): String? {
        val matcher = ARXIV_ID_PATTERN.matcher(raw)
        return if (matcher.find()) matcher.group(1) else null
    }

    fun extractDoiIdentifier(raw: String): String? {
        val matcher = DOI_PATTERN.matcher(raw)
        if (matcher.find()) {
            val candidate = sanitizeDoi(matcher.group(1))
            if (isValidDoiPrefix(candidate)) return candidate
        }
        return null
    }

    private fun sanitizeDoi(raw: String?): String {
        if (raw == null) return ""
        var doi = raw.trim()
        // Trim trailing punctuation common in text citations (.,;)
        while (doi.endsWith(".") || doi.endsWith(",") || doi.endsWith(";") || doi.endsWith(")") || doi.endsWith(">")) {
            doi = doi.dropLast(1).trim()
        }
        return doi
    }

    private fun isValidDoiPrefix(doi: String): Boolean {
        return doi.startsWith("10.") && doi.contains("/") && doi.length > 7
    }

    fun cleanPdfString(raw: String): String {
        return raw.replace("\\(", "(")
            .replace("\\)", ")")
            .replace("\\\\", "\\")
            .replace("\\r", " ")
            .replace("\\n", " ")
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    fun decodePdfHexString(hex: String): String {
        return try {
            val clean = hex.replace("\\s".toRegex(), "")
            if (clean.length % 2 != 0) return ""
            val bytes = ByteArray(clean.length / 2)
            for (i in bytes.indices) {
                bytes[i] = clean.substring(i * 2, i * 2 + 2).toInt(16).toByte()
            }
            if (bytes.size >= 2 && bytes[0] == 0xFE.toByte() && bytes[1] == 0xFF.toByte()) {
                String(bytes, 2, bytes.size - 2, Charsets.UTF_16BE).trim()
            } else {
                String(bytes, Charsets.ISO_8859_1).trim()
            }
        } catch (_: Exception) {
            ""
        }
    }

    fun cleanFilenameToTitle(fileName: String): String {
        var base = fileName
        val extIndex = base.lastIndexOf('.')
        if (extIndex > 0) base = base.substring(0, extIndex)
        return base
            .replace("_", " ")
            .replace("-", " ")
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    private fun isGenericTitle(title: String): Boolean {
        val lower = title.lowercase().trim()
        return lower.isBlank() ||
                lower == "untitled" ||
                lower.startsWith("microsoft word") ||
                lower.startsWith("untitled document") ||
                lower.startsWith("latex document") ||
                lower == "paper" ||
                lower == "manuscript"
    }
}
