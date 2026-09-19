package com.example.data

import android.content.Context
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import java.util.concurrent.TimeUnit

/**
 * Manages research paper PDFs stored locally in the Paper Vault.
 *
 * User-selected PDFs and downloaded open-access preprints are safely stored inside
 * app-private storage `context.filesDir/vault_pdfs/`.
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
     * Copies a user-picked PDF [source] Uri into app storage, returning its absolute path,
     * or null if unreadable.
     */
    suspend fun persist(context: Context, source: Uri): String? = withContext(Dispatchers.IO) {
        try {
            val file = File(dir(context), "${UUID.randomUUID()}.pdf")
            context.contentResolver.openInputStream(source)?.use { input ->
                FileOutputStream(file).use { output ->
                    input.copyTo(output)
                }
            } ?: return@withContext null

            if (file.length() <= 0L) {
                file.delete()
                return@withContext null
            }

            file.absolutePath
        } catch (e: Exception) {
            Log.e(TAG, "Failed to persist PDF from $source", e)
            null
        }
    }

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
     * Deletes a local PDF if it resides inside app-private files directory.
     */
    fun delete(context: Context, path: String) {
        if (path.isBlank()) return
        try {
            val file = File(path)
            if (file.canonicalPath.startsWith(context.filesDir.canonicalPath)) {
                file.delete()
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to delete PDF at $path", e)
        }
    }

    /**
     * Calculates the number of pages in a local PDF without loading page bitmaps into memory.
     */
    fun getPageCount(path: String): Int {
        if (path.isBlank()) return 0
        val file = File(path)
        if (!file.exists() || !file.canRead()) return 0
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
