package com.example.data.security

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.util.zip.ZipInputStream

/**
 * Supported academic manuscript and paper document formats.
 */
enum class DocumentFormat(val extension: String, val label: String, val mimeType: String) {
    PDF("pdf", "PDF", "application/pdf"),
    DOCX("docx", "Word (DOCX)", "application/vnd.openxmlformats-officedocument.wordprocessingml.document"),
    DOC("doc", "Word (DOC)", "application/msword"),
    RTF("rtf", "RTF", "application/rtf"),
    TXT("txt", "Text", "text/plain"),
    MD("md", "Markdown", "text/markdown"),
    TEX("tex", "LaTeX", "application/x-tex");

    companion object {
        fun fromExtension(ext: String): DocumentFormat? =
            entries.firstOrNull { it.extension.equals(ext, ignoreCase = true) }
    }
}

/**
 * Result of document upload security validation.
 */
sealed class ValidationResult {
    data class Success(
        val format: DocumentFormat,
        val sanitizedFileName: String,
        val sizeBytes: Long
    ) : ValidationResult()

    data class Error(val message: String) : ValidationResult()
}

/**
 * Enforces rigorous security, file integrity, anti-malware safeguards, and rate limiting
 * on uploaded academic research documents.
 *
 * Requirements:
 * 1. Allows Word (DOCX, DOC), PDF, RTF, Text, Markdown, and LaTeX.
 * 2. Strictly prohibits ZIP archives, RAR, 7Z, and executables to prevent malware/virus payloads.
 * 3. Inspects magic bytes to detect disguised ZIP archives or PE/ELF binary executables.
 * 4. Enforces 50 MB maximum size limit to prevent memory freezing and DoS.
 * 5. Applies rate limiting to prevent server/app exhaustion.
 */
object DocumentUploadValidator {

    private const val TAG = "DocumentValidator"

    /** Maximum allowed paper upload size: 50 MB */
    const val MAX_FILE_SIZE_BYTES = 50 * 1024 * 1024L // 50 MB

    /** Rate limit: Maximum 5 document uploads per 60-second window */
    private const val MAX_UPLOADS_PER_WINDOW = 5
    private const val WINDOW_DURATION_MS = 60_000L

    private val uploadTimestamps = mutableListOf<Long>()

    private val FORBIDDEN_EXTENSIONS = setOf(
        "zip", "rar", "7z", "tar", "gz", "bz2", "xz", "iso", "tgz",
        "exe", "dll", "bat", "cmd", "sh", "vbs", "js", "jar", "apk",
        "com", "scr", "bin", "msi", "ps1", "elf", "so", "dmg"
    )

    /**
     * Checks client-side rate limiting to prevent rapid-fire spam or app freezes.
     */
    @Synchronized
    fun checkRateLimit(): Boolean {
        val now = System.currentTimeMillis()
        uploadTimestamps.removeAll { now - it > WINDOW_DURATION_MS }
        return if (uploadTimestamps.size < MAX_UPLOADS_PER_WINDOW) {
            uploadTimestamps.add(now)
            true
        } else {
            false
        }
    }

    /**
     * Resets rate limiting state (useful for unit tests).
     */
    @Synchronized
    fun resetRateLimit() {
        uploadTimestamps.clear()
    }

    /**
     * Inspects and validates a user-selected [uri] against strict security policies.
     */
    suspend fun validate(context: Context, uri: Uri): ValidationResult = withContext(Dispatchers.IO) {
        if (!checkRateLimit()) {
            return@withContext ValidationResult.Error(
                "Upload rate limit reached: Please wait 30 seconds before uploading another paper to avoid server overlimiting."
            )
        }

        try {
            val fileName = queryFileName(context, uri)
            val extension = fileName.substringAfterLast('.', "").lowercase()

            // 1. Strict forbidden extensions check (explicitly blocking ZIPs and executables)
            if (FORBIDDEN_EXTENSIONS.contains(extension)) {
                return@withContext ValidationResult.Error(
                    "Security rejection: ZIP archives and executable files are strictly forbidden to prevent viruses or malicious payloads."
                )
            }

            // 2. Whitelist check: Must be a recognized academic format
            val format = DocumentFormat.fromExtension(extension)
                ?: return@withContext ValidationResult.Error(
                    "Unsupported file format (.$extension). Allowed formats: PDF, Word (DOCX/DOC), RTF, TXT, MD, and LaTeX."
                )

            // 3. Inspect stream size and magic bytes
            context.contentResolver.openInputStream(uri)?.use { stream ->
                validateStream(stream, format, fileName)
            } ?: ValidationResult.Error("Unable to open selected document file.")
        } catch (e: Exception) {
            Log.e(TAG, "Validation failed", e)
            ValidationResult.Error("Error inspecting document: ${e.localizedMessage ?: "Unknown error"}")
        }
    }

    /**
     * Validates an InputStream against magic bytes and size constraints.
     */
    fun validateStream(
        stream: InputStream,
        expectedFormat: DocumentFormat,
        fileName: String
    ): ValidationResult {
        val header = ByteArray(16)
        var bytesRead = 0
        while (bytesRead < header.size) {
            val read = stream.read(header, bytesRead, header.size - bytesRead)
            if (read == -1) break
            bytesRead += read
        }

        if (bytesRead == 0) {
            return ValidationResult.Error("The selected file is empty (0 bytes).")
        }

        // Check for Executable / Binary Malware Magic Bytes
        if (isWindowsExecutable(header)) {
            return ValidationResult.Error("Security rejection: Executable binary detected (MZ header). Malicious files are forbidden.")
        }
        if (isElfBinary(header)) {
            return ValidationResult.Error("Security rejection: Linux/Android binary executable detected.")
        }
        if (isShellScript(header)) {
            return ValidationResult.Error("Security rejection: Shell script detected.")
        }

        // Check for ZIP magic bytes (50 4B 03 04, 50 4B 05 06, 50 4B 07 08)
        val isZipHeader = isZipArchive(header)
        if (isZipHeader) {
            if (expectedFormat != DocumentFormat.DOCX) {
                return ValidationResult.Error(
                    "Security rejection: Disguised ZIP archive detected in a non-DOCX file. Upload blocked."
                )
            }
        }

        // If claimed to be PDF, verify PDF header (%PDF-)
        if (expectedFormat == DocumentFormat.PDF) {
            if (!isPdfHeader(header)) {
                return ValidationResult.Error(
                    "Invalid PDF document: The file does not start with a valid PDF header."
                )
            }
        }

        // Count total stream size safely without loading entire payload into RAM
        var totalBytes = bytesRead.toLong()
        val buffer = ByteArray(8192)
        var count: Int
        while (stream.read(buffer).also { count = it } != -1) {
            totalBytes += count
            if (totalBytes > MAX_FILE_SIZE_BYTES) {
                return ValidationResult.Error(
                    "Document exceeds the maximum limit of 50 MB (read over ${(totalBytes / (1024 * 1024))} MB). Please compress or trim the manuscript."
                )
            }
        }

        val sanitized = sanitizeFileName(fileName)
        return ValidationResult.Success(
            format = expectedFormat,
            sanitizedFileName = sanitized,
            sizeBytes = totalBytes
        )
    }

    private fun isWindowsExecutable(header: ByteArray): Boolean =
        header.size >= 2 && header[0] == 0x4D.toByte() && header[1] == 0x5A.toByte() // MZ

    private fun isElfBinary(header: ByteArray): Boolean =
        header.size >= 4 && header[0] == 0x7F.toByte() && header[1] == 'E'.code.toByte() &&
            header[2] == 'L'.code.toByte() && header[3] == 'F'.code.toByte()

    private fun isShellScript(header: ByteArray): Boolean =
        header.size >= 2 && header[0] == '#'.code.toByte() && header[1] == '!'.code.toByte()

    fun isZipArchive(header: ByteArray): Boolean =
        header.size >= 4 && header[0] == 0x50.toByte() && header[1] == 0x4B.toByte() &&
            (header[2] == 0x03.toByte() || header[2] == 0x05.toByte() || header[2] == 0x07.toByte())

    fun isPdfHeader(header: ByteArray): Boolean =
        header.size >= 4 && header[0] == '%'.code.toByte() && header[1] == 'P'.code.toByte() &&
            header[2] == 'D'.code.toByte() && header[3] == 'F'.code.toByte()

    private fun sanitizeFileName(name: String): String {
        val base = name.trim().ifBlank { "research_paper.pdf" }
        return base.replace(Regex("[^a-zA-Z0-9._-]"), "_")
    }

    private fun queryFileName(context: Context, uri: Uri): String {
        var name = uri.lastPathSegment.orEmpty()
        try {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (nameIndex != -1 && cursor.moveToFirst()) {
                    val displayName = cursor.getString(nameIndex)
                    if (!displayName.isNullOrBlank()) name = displayName
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to resolve display name for $uri", e)
        }
        return name
    }
}
