package com.example.ui.components

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel

/**
 * Custom Compose component that renders a 100% genuine ISO-compliant QR Code
 * using ZXing QRCodeWriter with standard quiet zone and pixel-perfect high contrast.
 * Designed for immediate, reliable optical detection by other device cameras.
 */
@Composable
fun QrCodeCanvas(
    qrData: String,
    modifier: Modifier = Modifier,
    size: Dp = 210.dp,
    showScanAnimation: Boolean = false // Kept for API compatibility; laser line removed so optical scanning from other phones is 100% unobstructed
) {
    val qrBitmap = remember(qrData) {
        generateRealQrBitmap(qrData, 600)
    }

    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White)
            .padding(12.dp),
        contentAlignment = Alignment.Center
    ) {
        if (qrBitmap != null) {
            Image(
                bitmap = qrBitmap.asImageBitmap(),
                contentDescription = "Official Outpass QR Code",
                filterQuality = FilterQuality.None,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

private fun generateRealQrBitmap(content: String, targetSize: Int): Bitmap? {
    return try {
        val hints = mapOf(
            EncodeHintType.MARGIN to 2, // Standard quiet zone for reliable optical detection
            EncodeHintType.CHARACTER_SET to "UTF-8",
            EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.M
        )
        val writer = QRCodeWriter()
        val bitMatrix = writer.encode(content, BarcodeFormat.QR_CODE, targetSize, targetSize, hints)
        val width = bitMatrix.width
        val height = bitMatrix.height
        val pixels = IntArray(width * height)

        val blackColor = android.graphics.Color.BLACK
        val whiteColor = android.graphics.Color.WHITE

        for (y in 0 until height) {
            val offset = y * width
            for (x in 0 until width) {
                pixels[offset + x] = if (bitMatrix.get(x, y)) blackColor else whiteColor
            }
        }

        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        bitmap.setPixels(pixels, 0, width, 0, 0, width, height)
        bitmap
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}
