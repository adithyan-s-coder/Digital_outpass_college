package com.example.ui.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.util.Base64
import android.util.Log
import androidx.exifinterface.media.ExifInterface
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

object ImageCropUtil {
    private const val TAG = "ImageCropUtil"
    private const val TARGET_AVATAR_SIZE = 160 // compact, high quality for avatars & fast network sync

    fun getAvatarsDir(context: Context): File {
        val dir = File(context.filesDir, "avatars")
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    fun savePermanently(context: Context, sourceUri: Uri): String? {
        return try {
            val bitmap = loadAndOrientBitmap(context, sourceUri) ?: return null
            val squareBitmap = cropToSquare(bitmap, 1.0f, 0.5f, 0.5f, TARGET_AVATAR_SIZE)
            encodeAndSave(context, squareBitmap)
        } catch (e: Exception) {
            Log.e(TAG, "savePermanently error: ${e.message}", e)
            sourceUri.toString()
        }
    }

    fun cropAndSaveSquare(
        context: Context,
        sourceUri: Uri,
        zoom: Float,
        panXPercent: Float,
        panYPercent: Float,
        targetSize: Int = TARGET_AVATAR_SIZE
    ): String? {
        return try {
            val bitmap = loadAndOrientBitmap(context, sourceUri) ?: return null
            val cropped = cropToSquare(bitmap, zoom, panXPercent, panYPercent, targetSize)
            encodeAndSave(context, cropped)
        } catch (e: Exception) {
            Log.e(TAG, "cropAndSaveSquare error: ${e.message}", e)
            null
        }
    }

    fun decodeBase64ToBitmap(base64Str: String): Bitmap? {
        return try {
            val clean = if (base64Str.contains(",")) {
                base64Str.substringAfter(",")
            } else {
                base64Str
            }.trim()
            val bytes = Base64.decode(clean, Base64.DEFAULT)
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        } catch (e: Exception) {
            Log.w(TAG, "decodeBase64ToBitmap error: ${e.message}")
            null
        }
    }

    fun saveBase64ToDisk(context: Context, base64Str: String, prefix: String): String? {
        return try {
            val bitmap = decodeBase64ToBitmap(base64Str) ?: return null
            val avatarsDir = getAvatarsDir(context)
            val file = File(avatarsDir, "${prefix}_${System.currentTimeMillis()}.jpg")
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 80, out)
            }
            Uri.fromFile(file).toString()
        } catch (e: Exception) {
            Log.e(TAG, "saveBase64ToDisk error: ${e.message}")
            null
        }
    }

    private fun encodeAndSave(context: Context, bitmap: Bitmap): String {
        val stream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 75, stream)
        val byteArray = stream.toByteArray()
        val base64 = Base64.encodeToString(byteArray, Base64.NO_WRAP)
        val dataUri = "data:image/jpeg;base64,$base64"

        // Also save to disk locally
        try {
            val avatarsDir = getAvatarsDir(context)
            val file = File(avatarsDir, "avatar_${System.currentTimeMillis()}.jpg")
            FileOutputStream(file).use { out ->
                out.write(byteArray)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Could not save local copy: ${e.message}")
        }

        return dataUri
    }

    private fun loadAndOrientBitmap(context: Context, uri: Uri): Bitmap? {
        var input: InputStream? = null
        return try {
            input = context.contentResolver.openInputStream(uri) ?: return null
            val bitmap = BitmapFactory.decodeStream(input) ?: return null
            input.close()

            // Check orientation
            val exifStream = context.contentResolver.openInputStream(uri)
            val exif = exifStream?.let { ExifInterface(it) }
            val orientation = exif?.getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL
            ) ?: ExifInterface.ORIENTATION_NORMAL
            exifStream?.close()

            fixOrientation(bitmap, orientation)
        } catch (e: Exception) {
            Log.e(TAG, "loadAndOrientBitmap failed: ${e.message}", e)
            null
        } finally {
            input?.close()
        }
    }

    private fun fixOrientation(bitmap: Bitmap, orientation: Int): Bitmap {
        val matrix = Matrix()
        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.postScale(-1f, 1f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.postScale(1f, -1f)
            else -> return bitmap
        }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    private fun cropToSquare(
        bitmap: Bitmap,
        zoom: Float,
        panXPercent: Float,
        panYPercent: Float,
        targetSize: Int
    ): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        val minDim = Math.min(width, height)
        val cropSize = (minDim / Math.max(1.0f, zoom)).toInt().coerceIn(1, minDim)

        val maxOffsetX = width - cropSize
        val maxOffsetY = height - cropSize

        val startX = (maxOffsetX * panXPercent.coerceIn(0f, 1f)).toInt().coerceIn(0, maxOffsetX)
        val startY = (maxOffsetY * panYPercent.coerceIn(0f, 1f)).toInt().coerceIn(0, maxOffsetY)

        val cropped = Bitmap.createBitmap(bitmap, startX, startY, cropSize, cropSize)
        return Bitmap.createScaledBitmap(cropped, targetSize, targetSize, true)
    }
}
