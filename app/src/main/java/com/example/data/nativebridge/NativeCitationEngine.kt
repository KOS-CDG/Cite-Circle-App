package com.example.data.nativebridge

import android.util.Log

/**
 * Data transfer object mapped to C++ struct [ParsedBibTeXEntry].
 */
data class NativeBibEntry(
    val entryType: String,
    val citeKey: String,
    val title: String,
    val author: String,
    val year: String,
    val venue: String,
    val doi: String
)

/**
 * High-performance Native Citation & Document Engine.
 *
 * Bridges Kotlin to native C++20 routines compiled via Android NDK for:
 * 1. Zero-copy BibTeX citation tokenization and AST parsing.
 * 2. Microsecond-latency magic byte document validation.
 *
 * Features graceful degradation: If the native shared library (`.so`) is unavailable
 * (e.g., during host JVM unit testing without NDK), it falls back safely to pure Kotlin logic.
 */
object NativeCitationEngine {

    private const val TAG = "NativeCitationEngine"
    private const val LIB_NAME = "citecircle_native"

    private fun logInfo(msg: String) {
        try {
            Log.i(TAG, msg)
        } catch (_: Throwable) {
            println("[$TAG] $msg")
        }
    }

    private fun logWarn(msg: String) {
        try {
            Log.w(TAG, msg)
        } catch (_: Throwable) {
            println("[$TAG] $msg")
        }
    }

    private fun logError(msg: String) {
        try {
            Log.e(TAG, msg)
        } catch (_: Throwable) {
            System.err.println("[$TAG] $msg")
        }
    }

    val isNativeAvailable: Boolean by lazy {
        try {
            System.loadLibrary(LIB_NAME)
            logInfo("Native C++ engine successfully loaded: ${nativeGetVersion()}")
            true
        } catch (e: Throwable) {
            logWarn("Native library '$LIB_NAME' not loaded; running with Kotlin fallback engine. (${e.message})")
            false
        }
    }

    // --- Native JNI declarations ----------------------------------------------
    @JvmStatic
    private external fun nativeGetVersion(): String

    @JvmStatic
    private external fun nativeValidateDocumentBytes(bytes: ByteArray): Int

    @JvmStatic
    private external fun nativeParseBibTeX(rawBibtex: String): Array<NativeBibEntry>?

    // --- Public High-Level APIs -----------------------------------------------

    /**
     * Returns the active engine banner (C++ native with ARM-NEON if loaded, or Kotlin Fallback).
     */
    fun getEngineBanner(): String {
        return if (isNativeAvailable) {
            try {
                nativeGetVersion()
            } catch (e: Throwable) {
                "Kotlin Fallback Engine (Native error: ${e.message})"
            }
        } else {
            "Kotlin Pure Fallback Engine"
        }
    }

    /**
     * Detects document format via binary header inspection.
     * Returns format code:
     * 1 = PDF (%PDF-)
     * 2 = PostScript (%!PS-)
     * 3 = RTF ({\rtf)
     * 4 = EPUB/Zip (PK\x03\x04)
     * 0 = Unknown / Plain text
     */
    fun detectDocumentFormat(bytes: ByteArray): Int {
        if (bytes.size < 4) return 0
        if (isNativeAvailable) {
            try {
                return nativeValidateDocumentBytes(bytes)
            } catch (e: Throwable) {
                logError("Native validate failed, falling back to Kotlin: ${e.message}")
            }
        }

        // Pure Kotlin Fallback
        return when {
            bytes.size >= 5 && bytes[0] == 0x25.toByte() && bytes[1] == 0x50.toByte() &&
                bytes[2] == 0x44.toByte() && bytes[3] == 0x46.toByte() && bytes[4] == 0x2D.toByte() -> 1
            bytes.size >= 4 && bytes[0] == 0x25.toByte() && bytes[1] == 0x21.toByte() &&
                bytes[2] == 0x50.toByte() && bytes[3] == 0x53.toByte() -> 2
            bytes.size >= 5 && bytes[0] == '{'.code.toByte() && bytes[1] == '\\'.code.toByte() &&
                bytes[2] == 'r'.code.toByte() && bytes[3] == 't'.code.toByte() && bytes[4] == 'f'.code.toByte() -> 3
            bytes.size >= 4 && bytes[0] == 0x50.toByte() && bytes[1] == 0x4B.toByte() &&
                bytes[2] == 0x03.toByte() && bytes[3] == 0x04.toByte() -> 4
            else -> 0
        }
    }

    fun getFormatLabel(formatCode: Int): String {
        return when (formatCode) {
            1 -> "PDF Document"
            2 -> "PostScript Document"
            3 -> "Rich Text Format (RTF)"
            4 -> "EPUB / ZIP Container"
            else -> "Plain Text / Unknown"
        }
    }

    /**
     * Parses raw BibTeX text into structured entries.
     */
    fun parseBibTeX(rawBibtex: String): List<NativeBibEntry> {
        if (rawBibtex.isBlank()) return emptyList()

        if (isNativeAvailable) {
            try {
                val nativeResults = nativeParseBibTeX(rawBibtex)
                if (nativeResults != null) {
                    return nativeResults.toList()
                }
            } catch (e: Throwable) {
                logError("Native BibTeX parsing failed, falling back to Kotlin: ${e.message}")
            }
        }

        // Pure Kotlin Fallback Parser
        return parseBibTeXFallback(rawBibtex)
    }

    private fun parseBibTeXFallback(text: String): List<NativeBibEntry> {
        val entries = mutableListOf<NativeBibEntry>()
        var pos = 0
        val len = text.length

        while (pos < len) {
            val atPos = text.indexOf('@', pos)
            if (atPos == -1) break

            val braceStart = text.indexOf('{', atPos)
            if (braceStart == -1) break

            val entryType = text.substring(atPos + 1, braceStart).trim().lowercase()
            if (entryType.isEmpty()) {
                pos = atPos + 1
                continue
            }

            var cursor = braceStart + 1
            var depth = 1
            var inQuotes = false

            while (cursor < len && depth > 0) {
                val ch = text[cursor]
                if (ch == '\"' && (cursor == 0 || text[cursor - 1] != '\\')) {
                    inQuotes = !inQuotes
                } else if (!inQuotes) {
                    if (ch == '{') depth++
                    else if (ch == '}') depth--
                }
                cursor++
            }

            if (depth != 0) break

            val body = text.substring(braceStart + 1, cursor - 1)
            val commaPos = body.indexOf(',')
            if (commaPos != -1) {
                val citeKey = body.substring(0, commaPos).trim()
                val fieldsBlock = body.substring(commaPos + 1)

                val fieldMap = mutableMapOf<String, String>()
                val fieldRegex = Regex("""(\w+)\s*=\s*(?:\{([^{}]*)\}|"([^"]*)"|([^,\n\r]+))""")
                for (match in fieldRegex.findAll(fieldsBlock)) {
                    val fieldName = match.groupValues[1].lowercase()
                    val value = match.groupValues[2].ifEmpty {
                        match.groupValues[3].ifEmpty {
                            match.groupValues[4]
                        }
                    }.trim()
                    fieldMap[fieldName] = value
                }

                entries.add(
                    NativeBibEntry(
                        entryType = entryType,
                        citeKey = citeKey,
                        title = fieldMap["title"].orEmpty(),
                        author = fieldMap["author"].orEmpty(),
                        year = fieldMap["year"].orEmpty(),
                        venue = fieldMap["journal"] ?: fieldMap["booktitle"].orEmpty(),
                        doi = fieldMap["doi"].orEmpty()
                    )
                )
            }

            pos = cursor
        }

        return entries
    }
}
