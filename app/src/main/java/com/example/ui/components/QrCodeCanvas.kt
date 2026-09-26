package com.example.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.abs

/**
 * Custom Compose canvas component that draws a realistic vector QR Code matrix
 * deterministically based on input data token, complete with 3 finder patterns and an animated scanning beam.
 */
@Composable
fun QrCodeCanvas(
    qrData: String,
    modifier: Modifier = Modifier,
    size: Dp = 200.dp,
    showScanAnimation: Boolean = true
) {
    val matrixSize = 21 // Standard QR version 1 size
    val grid = remember(qrData) {
        generateQrMatrix(qrData, matrixSize)
    }

    val infiniteTransition = rememberInfiniteTransition(label = "scan_beam")
    val beamYPercentage by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "beam_y"
    )

    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White)
            .padding(12.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val canvasWidth = this.size.width
            val cellSize = canvasWidth / matrixSize

            // Draw Modules
            for (row in 0 until matrixSize) {
                for (col in 0 until matrixSize) {
                    if (grid[row][col]) {
                        drawRect(
                            color = Color(0xFF0F172A),
                            topLeft = Offset(col * cellSize, row * cellSize),
                            size = Size(cellSize * 0.95f, cellSize * 0.95f)
                        )
                    }
                }
            }

            // Draw Finder Patterns (Top-Left, Top-Right, Bottom-Left)
            val finderSize = cellSize * 7
            val drawFinder = { x: Float, y: Float ->
                // Outer square
                drawRect(
                    color = Color(0xFF0F172A),
                    topLeft = Offset(x, y),
                    size = Size(finderSize, finderSize)
                )
                // Inner white gap
                drawRect(
                    color = Color.White,
                    topLeft = Offset(x + cellSize, y + cellSize),
                    size = Size(finderSize - 2 * cellSize, finderSize - 2 * cellSize)
                )
                // Center black block
                drawRect(
                    color = Color(0xFF6366F1), // Indigo accent center
                    topLeft = Offset(x + 2 * cellSize, y + 2 * cellSize),
                    size = Size(finderSize - 4 * cellSize, finderSize - 4 * cellSize)
                )
            }

            drawFinder(0f, 0f) // Top-Left
            drawFinder((matrixSize - 7) * cellSize, 0f) // Top-Right
            drawFinder(0f, (matrixSize - 7) * cellSize) // Bottom-Left

            // Draw Scanning Beam if enabled
            if (showScanAnimation) {
                val currentY = canvasWidth * beamYPercentage
                drawLine(
                    color = Color(0xFF10B981), // Emerald green glow
                    start = Offset(0f, currentY),
                    end = Offset(canvasWidth, currentY),
                    strokeWidth = 6f
                )
                drawRect(
                    color = Color(0x3310B981),
                    topLeft = Offset(0f, currentY - 12f),
                    size = Size(canvasWidth, 24f)
                )
            }
        }
    }
}

private fun generateQrMatrix(data: String, size: Int): Array<BooleanArray> {
    val matrix = Array(size) { BooleanArray(size) }
    val hash = abs(data.hashCode())

    // Standard pattern filling using hash seed
    for (r in 0 until size) {
        for (c in 0 until size) {
            // Reserve finder patterns (7x7 corners)
            val isTopLeft = r < 7 && c < 7
            val isTopRight = r < 7 && c >= size - 7
            val isBottomLeft = r >= size - 7 && c < 7

            if (!isTopLeft && !isTopRight && !isBottomLeft) {
                val bitIndex = (r * size + c) % 32
                val isFilled = ((hash shr bitIndex) and 1) == 1 || (r + c) % 3 == 0
                matrix[r][c] = isFilled
            }
        }
    }
    return matrix
}
