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
 * Manages profile photo (avatar) and cover banner image processing,
 * memory-safe downsampling, square cropping, and sandboxed persistent storage.
 *
 * Ensures images picked from Android's Photo Picker survive app restarts
 * and process death without transient SecurityException permission denials.
 */
object ProfileImageHelper {

    private const val TAG = "ProfileImageHelper"
    private const val AVATAR_DIR = "avatars"
    private const val COVER_DIR = "covers"

    private const val AVATAR_MAX_EDGE = 512
    private const val COVER_MAX_EDGE = 1600
    private const val COMPRESSION_QUALITY = 85

    private fun avatarDir(context: Context): File =
        File(context.filesDir, AVATAR_DIR).apply { mkdirs() }

    private fun coverDir(context: Context): File =
        File(context.filesDir, COVER_DIR).apply { mkdirs() }

    /**
     * Persists and optimizes a profile photo into sandboxed app storage.
     * Crops image into a square and resizes to [AVATAR_MAX_EDGE]x[AVATAR_MAX_EDGE] max dimensions.
     *
     * @return Absolute file path of the saved JPEG, or null on failure.
     */
    suspend fun persistAvatar(
        context: Context,
        source: Uri,
        userId: String = ""
    ): String? = withContext(Dispatchers.IO) {
        try {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            context.contentResolver.openInputStream(source)?.use {
                BitmapFactory.decodeStream(it, null, bounds)
            }

            val origWidth = bounds.outWidth
            val origHeight = bounds.outHeight
            if (origWidth <= 0 || origHeight <= 0) {
                Log.w(TAG, "Invalid image bounds for avatar: $source")
                return@withContext null
            }

            // Downsample with sampleSize
            val sampleSize = calculateSampleSize(origWidth, origHeight, AVATAR_MAX_EDGE, AVATAR_MAX_EDGE)
            val decodeOptions = BitmapFactory.Options().apply {
                inSampleSize = sampleSize
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }

            val rawBitmap = context.contentResolver.openInputStream(source)?.use {
                BitmapFactory.decodeStream(it, null, decodeOptions)
            } ?: return@withContext null

            // Center-crop to square
            val squareBitmap = cropToSquare(rawBitmap)
            if (squareBitmap != rawBitmap) {
                rawBitmap.recycle()
            }

            // Scale to target avatar size if still larger than AVATAR_MAX_EDGE
            val finalBitmap = if (squareBitmap.width > AVATAR_MAX_EDGE) {
                val scaled = Bitmap.createScaledBitmap(squareBitmap, AVATAR_MAX_EDGE, AVATAR_MAX_EDGE, true)
                if (scaled != squareBitmap) squareBitmap.recycle()
                scaled
            } else {
                squareBitmap
            }

            val filename = if (userId.isNotBlank()) {
                "avatar_${userId.replace(Regex("[^a-zA-Z0-9_-]"), "_")}.jpg"
            } else {
                "avatar_${UUID.randomUUID()}.jpg"
            }

            val targetFile = File(avatarDir(context), filename)
            FileOutputStream(targetFile).use { out ->
                finalBitmap.compress(Bitmap.CompressFormat.JPEG, COMPRESSION_QUALITY, out)
            }
            finalBitmap.recycle()

            Log.d(TAG, "Avatar successfully persisted at ${targetFile.absolutePath} (${targetFile.length()} bytes)")
            targetFile.absolutePath
        } catch (e: Exception) {
            Log.e(TAG, "Failed to persist avatar from $source", e)
            null
        }
    }

    /**
     * Persists and downsamples a cover banner image into sandboxed app storage.
     *
     * @return Absolute file path of the saved JPEG, or null on failure.
     */
    suspend fun persistCover(
        context: Context,
        source: Uri,
        userId: String = ""
    ): String? = withContext(Dispatchers.IO) {
        try {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            context.contentResolver.openInputStream(source)?.use {
                BitmapFactory.decodeStream(it, null, bounds)
            }

            val origWidth = bounds.outWidth
            val origHeight = bounds.outHeight
            if (origWidth <= 0 || origHeight <= 0) {
                Log.w(TAG, "Invalid image bounds for cover: $source")
                return@withContext null
            }

            val sampleSize = calculateSampleSize(origWidth, origHeight, COVER_MAX_EDGE, COVER_MAX_EDGE)
            val decodeOptions = BitmapFactory.Options().apply {
                inSampleSize = sampleSize
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }

            val bitmap = context.contentResolver.openInputStream(source)?.use {
                BitmapFactory.decodeStream(it, null, decodeOptions)
            } ?: return@withContext null

            val filename = if (userId.isNotBlank()) {
                "cover_${userId.replace(Regex("[^a-zA-Z0-9_-]"), "_")}.jpg"
            } else {
                "cover_${UUID.randomUUID()}.jpg"
            }

            val targetFile = File(coverDir(context), filename)
            FileOutputStream(targetFile).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, COMPRESSION_QUALITY, out)
            }
            bitmap.recycle()

            Log.d(TAG, "Cover persisted at ${targetFile.absolutePath} (${targetFile.length()} bytes)")
            targetFile.absolutePath
        } catch (e: Exception) {
            Log.e(TAG, "Failed to persist cover from $source", e)
            null
        }
    }

    /**
     * Safely deletes a stored image if located in app sandboxed storage.
     */
    suspend fun deleteImage(context: Context, path: String?) = withContext(Dispatchers.IO) {
        if (path.isNullOrBlank()) return@withContext
        try {
            val file = File(path)
            val isInsideAvatars = file.parentFile?.absolutePath == avatarDir(context).absolutePath
            val isInsideCovers = file.parentFile?.absolutePath == coverDir(context).absolutePath
            if (isInsideAvatars || isInsideCovers) {
                file.delete()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to delete stored image: $path", e)
        }
    }

    private fun cropToSquare(bitmap: Bitmap): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        if (width == height) return bitmap

        val dimension = minOf(width, height)
        val xOffset = (width - dimension) / 2
        val yOffset = (height - dimension) / 2

        return Bitmap.createBitmap(bitmap, xOffset, yOffset, dimension, dimension)
    }

    private fun calculateSampleSize(width: Int, height: Int, maxW: Int, maxH: Int): Int {
        var inSampleSize = 1
        while ((width / inSampleSize) > maxW || (height / inSampleSize) > maxH) {
            inSampleSize *= 2
        }
        return inSampleSize
    }
}
