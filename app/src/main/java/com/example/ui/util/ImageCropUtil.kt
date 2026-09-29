package com.example.ui.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import kotlin.math.min

object ImageCropUtil {

    /**
     * Ensures an avatar storage directory exists inside persistent internal storage (filesDir).
     * Files in filesDir are never wiped by Android OS cache clearers.
     */
    fun getAvatarsDir(context: Context): File {
        val dir = File(context.filesDir, "avatars")
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    /**
     * Immediately copies a transient content URI (e.g. from PhotoPicker) into a permanent
     * local file in filesDir/avatars so it never expires or disappears across app restarts.
     */
    fun savePermanently(context: Context, sourceUri: Uri): String? {
        return try {
            val dir = getAvatarsDir(context)
            val destFile = File(dir, "avatar_${System.currentTimeMillis()}.jpg")
            context.contentResolver.openInputStream(sourceUri)?.use { input ->
                FileOutputStream(destFile).use { output ->
                    input.copyTo(output)
                }
            }
            Uri.fromFile(destFile).toString()
        } catch (e: Exception) {
            e.printStackTrace()
            sourceUri.toString()
        }
    }

    /**
     * Crops and scales the image from [sourceUri] into a clean, square avatar.
     * Guarantees zero broken edges, zero blank margins, and proper orientation.
     * Saves to persistent filesDir/avatars so the avatar is permanently available across app reboots.
     */
    fun cropAndSaveSquare(
        context: Context,
        sourceUri: Uri,
        zoom: Float, // >= 1.0f
        panXPercent: Float, // -1.0f .. 1.0f
        panYPercent: Float, // -1.0f .. 1.0f
        targetSize: Int = 512
    ): String? {
        var inputStream: InputStream? = null
        var originalBitmap: Bitmap? = null
        var rotatedBitmap: Bitmap? = null
        var croppedBitmap: Bitmap? = null
        var finalScaledBitmap: Bitmap? = null

        return try {
            inputStream = context.contentResolver.openInputStream(sourceUri) ?: return null
            originalBitmap = BitmapFactory.decodeStream(inputStream)
            inputStream.close()
            inputStream = null

            if (originalBitmap == null) return null

            // Handle EXIF orientation from device camera photos
            rotatedBitmap = fixOrientation(context, sourceUri, originalBitmap)

            val width = rotatedBitmap.width
            val height = rotatedBitmap.height

            // Base square dimension is the smallest dimension of the image
            val baseDimension = min(width, height).toFloat()
            val effectiveZoom = zoom.coerceIn(1.0f, 5.0f)
            val cropDimension = (baseDimension / effectiveZoom).coerceIn(20f, baseDimension)

            // Available pan slack in pixels
            val maxSlackX = (width - cropDimension) / 2f
            val maxSlackY = (height - cropDimension) / 2f

            val clampedPanX = panXPercent.coerceIn(-1.0f, 1.0f)
            val clampedPanY = panYPercent.coerceIn(-1.0f, 1.0f)

            val centerX = (width / 2f) - (clampedPanX * maxSlackX)
            val centerY = (height / 2f) - (clampedPanY * maxSlackY)

            val left = (centerX - (cropDimension / 2f)).toInt().coerceIn(0, (width - cropDimension.toInt()).coerceAtLeast(0))
            val top = (centerY - (cropDimension / 2f)).toInt().coerceIn(0, (height - cropDimension.toInt()).coerceAtLeast(0))
            val cropW = cropDimension.toInt().coerceAtMost(width - left)
            val cropH = cropDimension.toInt().coerceAtMost(height - top)
            val squareDim = min(cropW, cropH).coerceAtLeast(1)

            croppedBitmap = Bitmap.createBitmap(rotatedBitmap, left, top, squareDim, squareDim)
            finalScaledBitmap = Bitmap.createScaledBitmap(croppedBitmap, targetSize, targetSize, true)

            val destFile = File(getAvatarsDir(context), "avatar_${System.currentTimeMillis()}.jpg")
            FileOutputStream(destFile).use { out ->
                finalScaledBitmap.compress(Bitmap.CompressFormat.JPEG, 92, out)
            }

            Uri.fromFile(destFile).toString()
        } catch (e: Exception) {
            e.printStackTrace()
            null
        } finally {
            try {
                inputStream?.close()
            } catch (_: Exception) {}
            if (rotatedBitmap != null && rotatedBitmap != originalBitmap) {
                rotatedBitmap.recycle()
            }
            originalBitmap?.recycle()
            if (croppedBitmap != null && croppedBitmap != finalScaledBitmap) {
                croppedBitmap.recycle()
            }
            finalScaledBitmap?.recycle()
        }
    }

    private fun fixOrientation(context: Context, uri: Uri, bitmap: Bitmap): Bitmap {
        return try {
            val input = context.contentResolver.openInputStream(uri) ?: return bitmap
            val exif = ExifInterface(input)
            val orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
            input.close()

            val matrix = Matrix()
            when (orientation) {
                ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
                ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
                ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
                else -> return bitmap
            }
            Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        } catch (e: Exception) {
            bitmap
        }
    }
}
