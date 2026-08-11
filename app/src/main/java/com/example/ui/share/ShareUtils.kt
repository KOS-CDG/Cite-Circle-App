package com.example.ui.share

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import androidx.core.content.FileProvider
import com.example.R
import com.example.data.CitationFormatter
import com.example.data.CitationStyle
import com.example.data.ExportFormat
import com.example.data.SavedPaper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.Locale

/**
 * Everything that leaves the app.
 *
 * Files are written into `cacheDir/shared`, which is the only directory exposed by
 * `res/xml/file_paths.xml`, and handed out as content:// URIs through [FileProvider] —
 * a raw file:// URI would trip FileUriExposedException on API 24+.
 */
object ShareUtils {

    private const val SHARED_DIR = "shared"

    private fun authority(context: Context) = "${context.packageName}.fileprovider"

    private fun sharedDir(context: Context): File =
        File(context.cacheDir, SHARED_DIR).apply { mkdirs() }

    /** Strips characters that are illegal in filenames on the platforms files may land on. */
    private fun safeFileName(raw: String, fallback: String): String {
        val cleaned = raw.trim()
            .replace(Regex("[^A-Za-z0-9 _-]"), "")
            .replace(Regex("\\s+"), "-")
            .take(48)
            .trim('-')
        return cleaned.ifBlank { fallback }.lowercase(Locale.US)
    }

    private fun launchChooser(context: Context, intent: Intent, title: String) {
        val chooser = Intent.createChooser(intent, title)
        // The caller may be a non-Activity context (e.g. application context in tests).
        chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(chooser)
    }

    /**
     * Shares a post as plain text: the author's commentary followed by the citation in the
     * chosen style. This is what lands in a tweet, a Slack message, or an email.
     */
    fun sharePostText(context: Context, paper: SavedPaper, style: CitationStyle) {
        val citation = CitationFormatter.format(paper, style)
        val body = buildString {
            if (paper.content.isNotBlank()) {
                append(paper.content.trim())
                append("\n\n")
            }
            append(citation)
            append("\n\n")
            append(context.getString(R.string.share_signature))
        }
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(
                Intent.EXTRA_SUBJECT,
                paper.title.ifBlank { context.getString(R.string.share_subject_post) }
            )
            putExtra(Intent.EXTRA_TEXT, body)
        }
        launchChooser(context, intent, context.getString(R.string.share_chooser_post))
    }

    /** Shares just the formatted citation, with no surrounding commentary. */
    fun shareCitationText(context: Context, paper: SavedPaper, style: CitationStyle) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(
                Intent.EXTRA_SUBJECT,
                paper.title.ifBlank { context.getString(R.string.share_subject_citation) }
            )
            putExtra(Intent.EXTRA_TEXT, CitationFormatter.format(paper, style))
        }
        launchChooser(context, intent, context.getString(R.string.share_chooser_citation))
    }

    /**
     * Writes [bitmap] to the shared cache as a PNG and offers it to any app that accepts
     * images. [caption] rides along as EXTRA_TEXT so composers that support both prefill it.
     */
    suspend fun shareCardImage(
        context: Context,
        bitmap: Bitmap,
        paper: SavedPaper,
        caption: String
    ) {
        val uri = withContext(Dispatchers.IO) {
            val name = safeFileName(paper.title, "citation")
            val file = File(sharedDir(context), "$name-card.png")
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }
            fileUri(context, file)
        }
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_TEXT, caption)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        launchChooser(context, intent, context.getString(R.string.share_chooser_card))
    }

    /**
     * Writes a real .bib or .ris file and shares it — replacing the placeholder toast that
     * previously claimed an export had happened.
     */
    suspend fun shareExport(context: Context, paper: SavedPaper, format: ExportFormat) {
        val uri = withContext(Dispatchers.IO) {
            val name = safeFileName(paper.title, "citation")
            val file = File(sharedDir(context), "$name.${format.extension}")
            file.writeText(CitationFormatter.export(paper, format))
            fileUri(context, file)
        }
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = format.mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(
                Intent.EXTRA_SUBJECT,
                paper.title.ifBlank { context.getString(R.string.share_subject_export) }
            )
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        launchChooser(
            context,
            intent,
            context.getString(R.string.share_chooser_export, format.label)
        )
    }

    private fun fileUri(context: Context, file: File): Uri =
        FileProvider.getUriForFile(context, authority(context), file)
}
