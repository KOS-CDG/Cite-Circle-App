package com.example.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

/**
 * Owns the images attached to posts.
 *
 * The photo picker hands back a URI whose read permission is transient, so the bytes have to
 * be copied into app storage before the post is saved — otherwise the image would render
 * once and then fail forever. Copies are downsampled on the way in: a modern camera shot is
 * tens of megabytes, and a feed card never needs more than [MAX_EDGE] pixels of it.
 */
object ImageStore {

    private const val DIR = "post-images"
    private const val MAX_EDGE = 1600
    private const val QUALITY = 85
    private const val TAG = "ImageStore"

    private fun dir(context: Context): File =
        File(context.filesDir, DIR).apply { mkdirs() }

    /**
     * Copies [source] into app storage, returning the absolute path of the stored JPEG, or
     * null if the image could not be read or decoded.
     */
    suspend fun persist(context: Context, source: Uri): String? = withContext(Dispatchers.IO) {
        try {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            context.contentResolver.openInputStream(source)?.use {
                BitmapFactory.decodeStream(it, null, bounds)
            }
            if (bounds.outWidth <= 0 || bounds.outHeight <= 0) {
                Log.w(TAG, "Could not read image bounds for $source")
                return@withContext null
            }

            val options = BitmapFactory.Options().apply {
                inSampleSize = sampleSize(bounds.outWidth, bounds.outHeight)
            }
            val bitmap = context.contentResolver.openInputStream(source)?.use {
                BitmapFactory.decodeStream(it, null, options)
            } ?: return@withContext null

            val file = File(dir(context), "${UUID.randomUUID()}.jpg")
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, QUALITY, out)
            }
            bitmap.recycle()
            file.absolutePath
        } catch (e: Exception) {
            Log.e(TAG, "Failed to persist image", e)
            null
        }
    }

    /** Deletes a stored image; safe to call with a blank path or a path already gone. */
    suspend fun delete(context: Context, path: String) {
        if (path.isBlank()) return
        withContext(Dispatchers.IO) {
            try {
                val file = File(path)
                // Only ever delete inside our own directory, never an arbitrary path.
                if (file.parentFile?.absolutePath == dir(context).absolutePath) file.delete()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to delete image at $path", e)
            }
        }
    }

    /** Largest power-of-two subsample that keeps both edges within [MAX_EDGE]. */
    private fun sampleSize(width: Int, height: Int): Int {
        var sample = 1
        while (width / sample > MAX_EDGE || height / sample > MAX_EDGE) sample *= 2
        return sample
    }
}
