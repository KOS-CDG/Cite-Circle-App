package com.example.data

import android.content.Context
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.util.Log
import com.example.data.security.DocumentFormat
import com.example.data.security.DocumentUploadValidator
import com.example.data.security.ValidationResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import java.util.concurrent.TimeUnit

/**
 * Manages research paper documents (PDF, Word DOCX/DOC, RTF, and text manuscripts)
 * stored securely in the Paper Vault.
 *
 * Enforces anti-malware verification and security restrictions via [DocumentUploadValidator].
 */
object PdfStore {

    private const val TAG = "PdfStore"
    private const val DIR = "vault_pdfs"

    private val httpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .followRedirects(true)
            .addInterceptor { chain ->
                val req = chain.request().newBuilder()
                    .header("User-Agent", "CiteCircle/1.0 (PaperVault; support@cite.circle)")
                    .build()
                chain.proceed(req)
            }
            .build()
    }

    private fun dir(context: Context): File =
        File(context.filesDir, DIR).apply { mkdirs() }

    /**
     * Inspects, validates against malware/virus/zip rules, and persists a user-selected
     * academic document (PDF, Word, RTF, or text manuscript) into app storage.
     * Returns Pair of (savedFilePath, ValidationResult).
     */
    suspend fun persistDocument(context: Context, source: Uri): Pair<String?, ValidationResult> = withContext(Dispatchers.IO) {
        val validation = DocumentUploadValidator.validate(context, source)
        if (validation is ValidationResult.Error) {
            return@withContext Pair(null, validation)
        }

        val success = validation as ValidationResult.Success
        try {
            val extension = success.format.extension
            val file = File(dir(context), "${UUID.randomUUID()}.$extension")

            context.contentResolver.openInputStream(source)?.use { input ->
                FileOutputStream(file).use { output ->
                    input.copyTo(output)
                }
            } ?: return@withContext Pair(null, ValidationResult.Error("Unable to read document stream."))

            if (file.length() <= 0L) {
                file.delete()
                return@withContext Pair(null, ValidationResult.Error("Document file is empty."))
            }

            Pair(file.absolutePath, validation)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to persist document from $source", e)
            Pair(null, ValidationResult.Error("Failed to save document: ${e.localizedMessage ?: "I/O error"}"))
        }
    }

    /**
     * Backward-compatible helper that validates and saves a document, returning the path or null.
     */
    suspend fun persist(context: Context, source: Uri): String? =
        persistDocument(context, source).first

    /**
     * Downloads an open-access PDF from [pdfUrl] into the Paper Vault, returning its local path.
     */
    suspend fun downloadPdf(context: Context, pdfUrl: String): String? = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder().url(pdfUrl).get().build()
            val response = httpClient.newCall(request).execute()

            if (!response.isSuccessful) {
                Log.w(TAG, "Failed to download PDF, code: ${response.code} for $pdfUrl")
                return@withContext null
            }

            val body = response.body ?: return@withContext null
            val file = File(dir(context), "${UUID.randomUUID()}.pdf")

            body.byteStream().use { input ->
                FileOutputStream(file).use { output ->
                    input.copyTo(output)
                }
            }

            if (file.length() <= 0L) {
                file.delete()
                return@withContext null
            }

            file.absolutePath
        } catch (e: Exception) {
            Log.e(TAG, "Error downloading PDF from $pdfUrl", e)
            null
        }
    }

    /**
     * Deletes a local document if it resides inside app-private files directory.
     */
    fun delete(context: Context, path: String) {
        if (path.isBlank()) return
        try {
            val file = File(path)
            if (file.canonicalPath.startsWith(context.filesDir.canonicalPath)) {
                file.delete()
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to delete document at $path", e)
        }
    }

    /**
     * Returns the detected format for a local file path (PDF, DOCX, DOC, RTF, TXT, etc.).
     */
    fun getDocumentFormat(path: String): DocumentFormat {
        val ext = path.substringAfterLast('.', "pdf").lowercase()
        return DocumentFormat.fromExtension(ext) ?: DocumentFormat.PDF
    }

    /**
     * Returns a human-friendly format badge label (e.g. "PDF", "Word (DOCX)", "Text").
     */
    fun getFormatLabel(path: String): String = getDocumentFormat(path).label

    /**
     * Calculates the number of pages in a local PDF without loading page bitmaps into memory.
     */
    fun getPageCount(path: String): Int {
        if (path.isBlank()) return 0
        val file = File(path)
        if (!file.exists() || !file.canRead()) return 0
        if (!path.endsWith(".pdf", ignoreCase = true)) return 1
        return try {
            ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY).use { pfd ->
                PdfRenderer(pfd).use { renderer ->
                    renderer.pageCount
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to inspect page count for $path", e)
            0
        }
    }

    /**
     * Formats bytes into a human-readable size string (e.g. "2.4 MB").
     */
    fun formatFileSize(bytes: Long): String {
        if (bytes <= 0) return "0 B"
        val kb = bytes / 1024.0
        val mb = kb / 1024.0
        return if (mb >= 1.0) {
            String.format("%.1f MB", mb)
        } else {
            String.format("%.0f KB", kb)
        }
    }

    /**
     * Returns formatted size for a given local file path.
     */
    fun getFormattedSize(path: String): String {
        if (path.isBlank()) return ""
        val file = File(path)
        return if (file.exists()) formatFileSize(file.length()) else ""
    }
}
