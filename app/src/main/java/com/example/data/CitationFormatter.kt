package com.example.data

import java.util.Locale
import java.util.concurrent.TimeUnit

/** The citation styles offered in the UI. */
enum class CitationStyle(val label: String) {
    APA("APA"),
    MLA("MLA"),
    CHICAGO("Chicago");

    companion object {
        val DEFAULT = APA
    }
}

/** Machine-readable export formats. */
enum class ExportFormat(val label: String, val extension: String, val mimeType: String) {
    BIBTEX("BibTeX", "bib", "application/x-bibtex"),
    RIS("RIS", "ris", "application/x-research-info-systems")
}

/**
 * One parsed author name.
 *
 * Input is expected as "Family, Given" but a plain "Given Family" is accepted too —
 * researchers paste both, and silently mangling the fallback is worse than guessing.
 */
data class Author(val family: String, val given: String) {

    /** "Doe, Jane" — the inverted form used for the first author in every style. */
    fun inverted(): String = if (given.isBlank()) family else "$family, $given"

    /** "Jane Doe" — the natural form used for non-first authors in MLA and Chicago. */
    fun natural(): String = if (given.isBlank()) family else "$given $family"

    /** "Doe, J. M." — APA reduces given names to initials. */
    fun withInitials(): String {
        val initials = given.split(' ', '.', '-')
            .filter { it.isNotBlank() }
            .joinToString(" ") { "${it.first().uppercaseChar()}." }
        return if (initials.isBlank()) family else "$family, $initials"
    }

    companion object {
        fun parse(raw: String): Author? {
            val entry = raw.trim().trim(',')
            if (entry.isBlank()) return null
            val comma = entry.indexOf(',')
            if (comma >= 0) {
                val family = entry.substring(0, comma).trim()
                val given = entry.substring(comma + 1).trim()
                if (family.isBlank()) return null
                return Author(family, given)
            }
            // No comma: assume "Given Family" and take the final token as the family name.
            val tokens = entry.split(' ').filter { it.isNotBlank() }
            if (tokens.isEmpty()) return null
            if (tokens.size == 1) return Author(tokens.first(), "")
            return Author(tokens.last(), tokens.dropLast(1).joinToString(" "))
        }

        /** Splits the stored semicolon-separated author string into parsed authors. */
        fun parseList(raw: String): List<Author> =
            raw.split(';').mapNotNull { parse(it) }
    }
}

/**
 * Renders [SavedPaper] metadata into citation styles and export formats.
 *
 * Everything is derived from the structured fields. When a paper has no [SavedPaper.title]
 * — which is true of rows migrated from schema v1 — the verbatim
 * [SavedPaper.citationOverride] is returned unchanged for every style, because a stored
 * string cannot be honestly restyled.
 */
object CitationFormatter {

    fun format(paper: SavedPaper, style: CitationStyle): String {
        if (paper.title.isBlank()) {
            return paper.citationOverride.ifBlank { "No citation metadata recorded." }
        }
        return when (style) {
            CitationStyle.APA -> apa(paper)
            CitationStyle.MLA -> mla(paper)
            CitationStyle.CHICAGO -> chicago(paper)
        }
    }

    /** True when [format] can actually restyle this paper rather than echoing a stored string. */
    fun isStyleable(paper: SavedPaper): Boolean = paper.title.isNotBlank()

    // ---------------------------------------------------------------- styles

    /** APA 7: Doe, J., & Smith, J. (2026). Title. Venue. https://doi.org/10.1000/x */
    private fun apa(paper: SavedPaper): String {
        val authors = Author.parseList(paper.authors)
        val sb = StringBuilder()
        var titleUsed = false

        if (authors.isEmpty()) {
            // APA moves the title into the author slot when a work has no credited author.
            sb.appendSentence(paper.title)
            titleUsed = true
        } else {
            sb.appendSentence(apaAuthors(authors))
        }
        sb.appendSentence("(${paper.year.ifBlank { "n.d." }})")
        if (!titleUsed) sb.appendSentence(paper.title)
        sb.appendSentence(paper.venue)

        val link = link(paper)
        if (link.isNotBlank()) {
            if (sb.isNotEmpty()) sb.append(' ')
            sb.append(link) // APA takes no terminal period after a DOI or URL.
        }
        return sb.toString()
    }

    private fun apaAuthors(authors: List<Author>): String = when {
        authors.size == 1 -> authors[0].withInitials()
        authors.size == 2 -> "${authors[0].withInitials()}, & ${authors[1].withInitials()}"
        authors.size <= 20 ->
            authors.dropLast(1).joinToString(", ") { it.withInitials() } +
                ", & ${authors.last().withInitials()}"
        // 21+ authors: first 19, an ellipsis, then the final author.
        else ->
            authors.take(19).joinToString(", ") { it.withInitials() } +
                ", ... ${authors.last().withInitials()}"
    }

    /** MLA 9: Doe, Jane, and John Smith. "Title." Venue, 2026, https://... */
    private fun mla(paper: SavedPaper): String {
        val authors = Author.parseList(paper.authors)
        val parts = mutableListOf<String>()

        if (authors.isNotEmpty()) parts += ensurePeriod(mlaAuthors(authors))
        parts += "“${stripTerminalPeriod(paper.title)}.”"

        val tail = listOf(paper.venue, paper.year, link(paper))
            .filter { it.isNotBlank() }
            .joinToString(", ")
        if (tail.isNotBlank()) parts += ensurePeriod(tail)

        return parts.joinToString(" ")
    }

    private fun mlaAuthors(authors: List<Author>): String = when {
        authors.size == 1 -> authors[0].inverted()
        authors.size == 2 -> "${authors[0].inverted()}, and ${authors[1].natural()}"
        // MLA 9 abbreviates three or more authors to the first plus "et al."
        else -> "${authors[0].inverted()}, et al"
    }

    /** Chicago: Doe, Jane, and John Smith. "Title." Venue (2026). https://... */
    private fun chicago(paper: SavedPaper): String {
        val authors = Author.parseList(paper.authors)
        val parts = mutableListOf<String>()

        if (authors.isNotEmpty()) parts += ensurePeriod(chicagoAuthors(authors))
        parts += "“${stripTerminalPeriod(paper.title)}.”"

        var tail = paper.venue.trim()
        if (paper.year.isNotBlank()) {
            tail = if (tail.isBlank()) "(${paper.year})" else "$tail (${paper.year})"
        }
        if (tail.isNotBlank()) parts += ensurePeriod(tail)

        val link = link(paper)
        if (link.isNotBlank()) parts += ensurePeriod(link)

        return parts.joinToString(" ")
    }

    private fun chicagoAuthors(authors: List<Author>): String = when {
        authors.size == 1 -> authors[0].inverted()
        authors.size == 2 -> "${authors[0].inverted()}, and ${authors[1].natural()}"
        authors.size <= 10 ->
            authors[0].inverted() + ", " +
                authors.drop(1).dropLast(1).joinToString("") { "${it.natural()}, " } +
                "and ${authors.last().natural()}"
        // 11+ authors: Chicago lists the first seven, then "et al."
        else ->
            authors[0].inverted() + ", " +
                authors.subList(1, 7).joinToString(", ") { it.natural() } +
                ", et al"
    }

    // ---------------------------------------------------------------- exports

    fun export(paper: SavedPaper, format: ExportFormat): String = when (format) {
        ExportFormat.BIBTEX -> bibtex(paper)
        ExportFormat.RIS -> ris(paper)
    }

    private fun bibtex(paper: SavedPaper): String {
        val authors = Author.parseList(paper.authors)
        val fields = buildList {
            if (authors.isNotEmpty()) {
                add("author" to authors.joinToString(" and ") { it.inverted() })
            }
            if (paper.title.isNotBlank()) add("title" to paper.title)
            if (paper.venue.isNotBlank()) add("journal" to paper.venue)
            if (paper.year.isNotBlank()) add("year" to paper.year)
            if (paper.doi.isNotBlank()) add("doi" to bareDoi(paper.doi))
            if (paper.url.isNotBlank()) add("url" to paper.url)
        }
        val body = fields.joinToString(",\n") { (key, value) ->
            "  ${key.padEnd(7)} = {${escapeBibtex(value)}}"
        }
        return "@article{${bibtexKey(paper, authors)},\n$body\n}\n"
    }

    private fun bibtexKey(paper: SavedPaper, authors: List<Author>): String {
        val name = authors.firstOrNull()?.family.orEmpty()
            .lowercase(Locale.US).filter { it.isLetterOrDigit() }
        val word = paper.title.split(' ')
            .map { it.lowercase(Locale.US).filter { c -> c.isLetterOrDigit() } }
            .firstOrNull { it.length > 3 }
            .orEmpty()
        val key = "$name${paper.year.filter { it.isDigit() }}$word"
        return key.ifBlank { "citecircle" }
    }

    /** Braces and backslashes are BibTeX's own syntax, so they cannot survive verbatim. */
    private fun escapeBibtex(value: String): String =
        value.replace("\\", "\\textbackslash{}")
            .replace("{", "\\{")
            .replace("}", "\\}")

    private fun ris(paper: SavedPaper): String {
        val lines = buildList {
            add("TY  - JOUR")
            Author.parseList(paper.authors).forEach { add("AU  - ${it.inverted()}") }
            if (paper.title.isNotBlank()) add("TI  - ${paper.title}")
            if (paper.venue.isNotBlank()) add("JO  - ${paper.venue}")
            if (paper.year.isNotBlank()) add("PY  - ${paper.year}")
            if (paper.doi.isNotBlank()) add("DO  - ${bareDoi(paper.doi)}")
            if (paper.url.isNotBlank()) add("UR  - ${paper.url}")
            add("ER  - ")
        }
        // RIS is a CRLF format; readers such as EndNote reject bare LF.
        return lines.joinToString("\r\n") + "\r\n"
    }

    // ---------------------------------------------------------------- helpers

    /** Prefers a resolvable DOI link, falling back to whatever URL was recorded. */
    private fun link(paper: SavedPaper): String {
        val doi = bareDoi(paper.doi)
        return if (doi.isNotBlank()) "https://doi.org/$doi" else paper.url.trim()
    }

    /** Strips the prefixes people paste along with a DOI, leaving the bare "10.x/y". */
    private fun bareDoi(raw: String): String {
        var doi = raw.trim()
        listOf("https://doi.org/", "http://doi.org/", "https://dx.doi.org/", "doi:", "DOI:")
            .forEach { prefix ->
                if (doi.startsWith(prefix, ignoreCase = true)) {
                    doi = doi.substring(prefix.length).trim()
                }
            }
        return doi
    }

    private fun ensurePeriod(text: String): String {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return trimmed
        return if (trimmed.endsWith('.')) trimmed else "$trimmed."
    }

    private fun stripTerminalPeriod(text: String): String = text.trim().removeSuffix(".")

    /** Appends a sentence, supplying the terminal period only when one is missing. */
    private fun StringBuilder.appendSentence(text: String) {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return
        if (isNotEmpty()) append(' ')
        append(trimmed)
        if (!endsWith(".") && !endsWith("?") && !endsWith("!")) append('.')
    }
}

/**
 * Renders a relative timestamp for the feed.
 *
 * v1 froze strings like "2h ago" into the database, so every post aged into a lie. This
 * derives the label from [publishedAt] at render time instead.
 */
fun formatTimeAgo(publishedAt: Long, now: Long = System.currentTimeMillis()): String {
    if (publishedAt <= 0L) return "—"
    val delta = now - publishedAt
    if (delta < 0L) return "just now"
    val minutes = TimeUnit.MILLISECONDS.toMinutes(delta)
    val hours = TimeUnit.MILLISECONDS.toHours(delta)
    val days = TimeUnit.MILLISECONDS.toDays(delta)
    return when {
        minutes < 1L -> "just now"
        minutes < 60L -> "${minutes}m ago"
        hours < 24L -> "${hours}h ago"
        days < 7L -> "${days}d ago"
        days < 365L -> "${days / 7L}w ago"
        else -> "${days / 365L}y ago"
    }
}
