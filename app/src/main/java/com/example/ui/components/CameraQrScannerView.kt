package com.example.ui.components

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.view.ViewGroup
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.PhotoLibrary
import android.graphics.BitmapFactory
import android.net.Uri
import android.widget.Toast
import androidx.activity.result.PickVisualMediaRequest
import com.google.zxing.RGBLuminanceSource
import com.google.zxing.InvertedLuminanceSource
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.ui.theme.CardBorderColor
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.GreenContainer
import com.example.ui.theme.GreenDark
import com.example.ui.theme.GreenPrimary
import com.example.ui.theme.TextDark
import com.example.ui.theme.TextMuted
import com.example.ui.theme.WhiteCard
import com.google.zxing.BarcodeFormat
import com.google.zxing.BinaryBitmap
import com.google.zxing.DecodeHintType
import com.google.zxing.MultiFormatReader
import com.google.zxing.PlanarYUVLuminanceSource
import com.google.zxing.common.HybridBinarizer
import java.util.concurrent.Executors
import com.example.data.models.Outpass

@Composable
fun CameraQrScannerView(
    onScanSuccess: (String) -> Unit,
    onCloseScanner: () -> Unit,
    modifier: Modifier = Modifier,
    availablePasses: List<Outpass> = emptyList()
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val haptic = LocalHapticFeedback.current

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
    }

    var isDecodingScreenshot by remember { mutableStateOf(false) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            isDecodingScreenshot = true
            val decoded = decodeQrFromImageUri(context, uri)
            isDecodingScreenshot = false
            if (!decoded.isNullOrBlank()) {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                Toast.makeText(context, "Screenshot scanned: ${extractPassIdentifier(decoded)}", Toast.LENGTH_SHORT).show()
                onScanSuccess(decoded)
            } else {
                Toast.makeText(context, "Could not find a valid Outpass QR code in selected screenshot.", Toast.LENGTH_LONG).show()
            }
        }
    }

    var isTorchEnabled by remember { mutableStateOf(false) }
    var useFrontCamera by remember { mutableStateOf(false) }
    var activeCamera by remember { mutableStateOf<Camera?>(null) }
    var lastScannedCode by remember { mutableStateOf<String?>(null) }

    var previewViewInstance by remember { mutableStateOf<PreviewView?>(null) }
    var cameraProviderInstance by remember { mutableStateOf<ProcessCameraProvider?>(null) }

    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }

    DisposableEffect(Unit) {
        onDispose {
            cameraExecutor.shutdown()
        }
    }

    // Initialize Camera Provider
    LaunchedEffect(hasCameraPermission) {
        if (hasCameraPermission) {
            val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
            cameraProviderFuture.addListener({
                try {
                    cameraProviderInstance = cameraProviderFuture.get()
                } catch (_: Exception) {}
            }, ContextCompat.getMainExecutor(context))
        }
    }

    // Dynamic Camera Rebinding: Re-binds cleanly whenever front/back toggle, previewView, or provider changes
    LaunchedEffect(useFrontCamera, previewViewInstance, cameraProviderInstance) {
        val provider = cameraProviderInstance ?: return@LaunchedEffect
        val pv = previewViewInstance ?: return@LaunchedEffect

        val targetSelector = if (useFrontCamera) {
            if (provider.hasCamera(CameraSelector.DEFAULT_FRONT_CAMERA)) {
                CameraSelector.DEFAULT_FRONT_CAMERA
            } else {
                CameraSelector.DEFAULT_BACK_CAMERA
            }
        } else {
            if (provider.hasCamera(CameraSelector.DEFAULT_BACK_CAMERA)) {
                CameraSelector.DEFAULT_BACK_CAMERA
            } else {
                CameraSelector.DEFAULT_FRONT_CAMERA
            }
        }

        val preview = Preview.Builder().build().also {
            it.surfaceProvider = pv.surfaceProvider
        }

        val qrReader = MultiFormatReader().apply {
            val hints = mapOf(
                DecodeHintType.POSSIBLE_FORMATS to listOf(BarcodeFormat.QR_CODE),
                DecodeHintType.TRY_HARDER to true
            )
            setHints(hints)
        }

        var lastScanTimeMs = 0L

        val imageAnalysis = ImageAnalysis.Builder()
            .setTargetResolution(android.util.Size(960, 540))
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()
            .also { analysis ->
                analysis.setAnalyzer(cameraExecutor) { imageProxy ->
                    processImageProxy(imageProxy, qrReader) { rawResult ->
                        val cleanRaw = rawResult.trim()
                        val now = System.currentTimeMillis()
                        if (cleanRaw.isNotBlank() && (now - lastScanTimeMs > 1200L)) {
                            lastScanTimeMs = now
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onScanSuccess(cleanRaw)
                        }
                    }
                }
            }

        try {
            provider.unbindAll()
            activeCamera = provider.bindToLifecycle(
                lifecycleOwner,
                targetSelector,
                preview,
                imageAnalysis
            )
            if (useFrontCamera) {
                isTorchEnabled = false
            } else {
                try {
                    activeCamera?.cameraControl?.enableTorch(isTorchEnabled)
                } catch (_: Exception) {}
            }
        } catch (_: Exception) {
            try {
                provider.unbindAll()
                activeCamera = provider.bindToLifecycle(
                    lifecycleOwner,
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    preview,
                    imageAnalysis
                )
            } catch (_: Exception) {}
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "scanner_laser")
    val laserProgress by infiniteTransition.animateFloat(
        initialValue = 0.05f,
        targetValue = 0.95f,
        animationSpec = infiniteRepeatable(
            animation = tween(1600, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "laser"
    )

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (!hasCameraPermission) {
            // Permission request prompt card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(240.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = WhiteCard),
                border = BorderStroke(1.dp, CardBorderColor)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Videocam,
                        contentDescription = "Camera Permission",
                        tint = GreenPrimary,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Camera Permission Required",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextDark
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "To scan student digital QR passes at the security gate, please allow camera access.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted,
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                        colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Grant Camera Access", fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }
        } else {
            // Live Camera View with Scanner Overlay
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(260.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.Black)
                    .border(2.dp, EmeraldSuccess, RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center
            ) {
                AndroidView(
                    factory = { ctx ->
                        PreviewView(ctx).apply {
                            layoutParams = ViewGroup.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT
                            )
                            scaleType = PreviewView.ScaleType.FILL_CENTER
                            previewViewInstance = this
                        }
                    },
                    modifier = Modifier.fillMaxSize(),
                    update = { pv ->
                        previewViewInstance = pv
                        if (!useFrontCamera) {
                            try {
                                activeCamera?.cameraControl?.enableTorch(isTorchEnabled)
                            } catch (_: Exception) {}
                        }
                    }
                )

                // Laser scan line overlay
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val lineY = size.height * laserProgress
                    drawLine(
                        color = Color(0xFF10B981),
                        start = Offset(size.width * 0.15f, lineY),
                        end = Offset(size.width * 0.85f, lineY),
                        strokeWidth = 5f
                    )
                    drawRect(
                        color = Color(0x3310B981),
                        topLeft = Offset(size.width * 0.15f, lineY - 8f),
                        size = Size(size.width * 0.70f, 16f)
                    )
                }

                // Visual Reticle overlay & controls
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(12.dp),
                    verticalArrangement = Arrangement.SpaceBetween,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Top controls bar (Torch, Live Indicator, & Front/Back Camera Switch)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = {
                                if (!useFrontCamera) {
                                    isTorchEnabled = !isTorchEnabled
                                    try {
                                        activeCamera?.cameraControl?.enableTorch(isTorchEnabled)
                                    } catch (_: Exception) {}
                                }
                            },
                            enabled = !useFrontCamera,
                            modifier = Modifier
                                .size(36.dp)
                                .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                        ) {
                            Icon(
                                imageVector = if (isTorchEnabled) Icons.Default.FlashOn else Icons.Default.FlashOff,
                                contentDescription = "Toggle Torch",
                                tint = if (isTorchEnabled) Color(0xFFFCD34D) else Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color.Black.copy(alpha = 0.65f))
                                .padding(horizontal = 10.dp, vertical = 5.dp)
                        ) {
                            Text(
                                text = if (useFrontCamera) "FRONT CAMERA (SELFIE SCAN)" else "BACK CAMERA (MAIN GATE SCAN)",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (useFrontCamera) Color(0xFF38BDF8) else EmeraldSuccess
                            )
                        }

                        IconButton(
                            onClick = { useFrontCamera = !useFrontCamera },
                            modifier = Modifier
                                .size(36.dp)
                                .background(if (useFrontCamera) GreenPrimary else Color.Black.copy(alpha = 0.5f), CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Cameraswitch,
                                contentDescription = "Switch Camera Front/Back",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    // Center Reticle
                    Box(
                        modifier = Modifier
                            .size(160.dp)
                            .border(2.dp, Color(0xFF10B981).copy(alpha = 0.8f), RoundedCornerShape(16.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.QrCodeScanner,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.4f),
                            modifier = Modifier.size(48.dp)
                        )
                    }

                    // Bottom instruction chip
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.Black.copy(alpha = 0.7f))
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = if (useFrontCamera) "Hold QR code facing the front camera" else "Align student outpass QR code in frame",
                            style = MaterialTheme.typography.bodySmall,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color.White
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Direct Screenshot / Gallery Photo Scanning Button
        Button(
            onClick = {
                photoPickerLauncher.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(46.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF0F172A),
                contentColor = Color.White
            )
        ) {
            Icon(
                imageVector = Icons.Default.PhotoLibrary,
                contentDescription = "Scan Screenshot",
                modifier = Modifier.size(20.dp),
                tint = Color.White
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = if (isDecodingScreenshot) "Decoding Screenshot..." else "Scan QR from Screenshot / Gallery Image",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // 1-Tap Quick Scan Test (helps instantly test QR flow in streaming browser emulator)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
        ) {
            Text(
                text = "Quick Test Scan (Simulator / Emulator):",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(4.dp))
            androidx.compose.foundation.lazy.LazyRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                val passesToTest = if (availablePasses.isNotEmpty()) availablePasses.take(5) else emptyList()
                if (passesToTest.isEmpty()) {
                    item {
                        Button(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onScanSuccess("PASS-1001")
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldSuccess),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Icon(Icons.Default.QrCodeScanner, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color.White)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Scan PASS-1001", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                } else {
                    items(passesToTest.size) { idx ->
                        val pass = passesToTest[idx]
                        Button(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onScanSuccess(pass.id)
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (pass.status == com.example.data.models.OutpassStatus.CHECKED_OUT) Color(0xFF38BDF8) else EmeraldSuccess
                            ),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Icon(Icons.Default.QrCodeScanner, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color.White)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Scan ${pass.id}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // QR Code ID manual entry: if camera scanning is not working, security can use the generated pass code to leave
        var manualQrIdInput by remember { mutableStateOf("") }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = WhiteCard),
            border = BorderStroke(1.dp, CardBorderColor)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp)
            ) {
                Text(
                    text = "QR Code ID / Pass Number (Manual Entry):",
                    style = MaterialTheme.typography.labelSmall,
                    color = GreenDark,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = manualQrIdInput,
                        onValueChange = { manualQrIdInput = it },
                        placeholder = { Text("Enter Pass ID (e.g. PASS-1001)", fontSize = 11.sp, color = TextMuted) },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = GreenPrimary,
                            unfocusedBorderColor = CardBorderColor,
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White,
                            focusedTextColor = Color.Black,
                            unfocusedTextColor = Color.Black
                        ),
                        textStyle = MaterialTheme.typography.bodySmall.copy(color = Color.Black)
                    )

                    Button(
                        onClick = {
                            if (manualQrIdInput.isNotBlank()) {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onScanSuccess(manualQrIdInput.trim())
                            }
                        },
                        enabled = manualQrIdInput.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Verify", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }

                    OutlinedButton(
                        onClick = onCloseScanner,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = TextDark),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, CardBorderColor)
                    ) {
                        Text("Close", fontSize = 11.sp, color = TextDark)
                    }
                }
            }
        }
    }
}

private fun processImageProxy(
    imageProxy: ImageProxy,
    reader: MultiFormatReader,
    onResult: (String) -> Unit
) {
    try {
        val plane = imageProxy.planes[0]
        val buffer = plane.buffer
        val rowStride = plane.rowStride
        val width = imageProxy.width
        val height = imageProxy.height

        val yBytes = ByteArray(buffer.remaining())
        buffer.get(yBytes)

        val rotationDegrees = imageProxy.imageInfo.rotationDegrees

        var source: com.google.zxing.LuminanceSource = PlanarYUVLuminanceSource(
            yBytes,
            rowStride,
            height,
            0,
            0,
            width,
            height,
            false
        )

        // Apply device orientation rotation to make the camera frame upright
        val numRotations = when (rotationDegrees) {
            90 -> 3  // 3 * 270° = 90° clockwise
            180 -> 2
            270 -> 1 // 1 * 270° = 90° counter-clockwise
            else -> 0
        }
        for (i in 0 until numRotations) {
            if (source.isRotateSupported) {
                source = source.rotateCounterClockwise()
            }
        }

        // 1. HybridBinarizer on upright image
        var binaryBitmap = BinaryBitmap(HybridBinarizer(source))
        var result = try {
            reader.decodeWithState(binaryBitmap)
        } catch (_: Exception) {
            null
        }

        // 2. GlobalHistogramBinarizer on upright image (specialized for illuminated phone screens)
        if (result == null) {
            try {
                binaryBitmap = BinaryBitmap(com.google.zxing.common.GlobalHistogramBinarizer(source))
                result = reader.decodeWithState(binaryBitmap)
            } catch (_: Exception) {}
        }

        // 3. Fallback: try unrotated original if sensor was already upright
        if (result == null && numRotations > 0) {
            try {
                val origSource = PlanarYUVLuminanceSource(yBytes, rowStride, height, 0, 0, width, height, false)
                binaryBitmap = BinaryBitmap(HybridBinarizer(origSource))
                result = reader.decodeWithState(binaryBitmap)
            } catch (_: Exception) {}

            if (result == null) {
                try {
                    val origSource = PlanarYUVLuminanceSource(yBytes, rowStride, height, 0, 0, width, height, false)
                    binaryBitmap = BinaryBitmap(com.google.zxing.common.GlobalHistogramBinarizer(origSource))
                    result = reader.decodeWithState(binaryBitmap)
                } catch (_: Exception) {}
            }
        }

        // 4. Inverted colors (for dark mode or high-contrast screens)
        if (result == null) {
            try {
                val inverted = com.google.zxing.InvertedLuminanceSource(source)
                binaryBitmap = BinaryBitmap(HybridBinarizer(inverted))
                result = reader.decodeWithState(binaryBitmap)
            } catch (_: Exception) {}
        }

        // 5. Center 65% Viewfinder crop (removes surrounding phone bezel/room background when scanning screenshots)
        if (result == null && width > 120 && height > 120) {
            try {
                val cropW = (width * 0.65).toInt()
                val cropH = (height * 0.65).toInt()
                val cropX = (width - cropW) / 2
                val cropY = (height - cropH) / 2
                var centerSource: com.google.zxing.LuminanceSource = PlanarYUVLuminanceSource(
                    yBytes, rowStride, height, cropX, cropY, cropW, cropH, false
                )
                for (i in 0 until numRotations) {
                    if (centerSource.isRotateSupported) {
                        centerSource = centerSource.rotateCounterClockwise()
                    }
                }
                binaryBitmap = BinaryBitmap(HybridBinarizer(centerSource))
                result = reader.decodeWithState(binaryBitmap)
                if (result == null) {
                    binaryBitmap = BinaryBitmap(com.google.zxing.common.GlobalHistogramBinarizer(centerSource))
                    result = reader.decodeWithState(binaryBitmap)
                }
            } catch (_: Exception) {}
        }

        result?.text?.let { onResult(it) }
    } catch (_: Exception) {
        // Normal frame-by-frame non-match
    } finally {
        reader.reset()
        imageProxy.close()
    }
}

fun decodeQrFromImageUri(context: Context, uri: Uri): String? {
    return try {
        val inputStream = context.contentResolver.openInputStream(uri) ?: return null
        val boundsOptions = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeStream(inputStream, null, boundsOptions)
        inputStream.close()

        val sampleSize = calculateInSampleSize(boundsOptions, 1200, 1200)
        val decodeOptions = BitmapFactory.Options().apply {
            inSampleSize = sampleSize
            inPreferredConfig = android.graphics.Bitmap.Config.ARGB_8888
        }
        val stream2 = context.contentResolver.openInputStream(uri) ?: return null
        val bitmap = BitmapFactory.decodeStream(stream2, null, decodeOptions) ?: return null
        stream2.close()

        val width = bitmap.width
        val height = bitmap.height
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

        val reader = MultiFormatReader().apply {
            setHints(
                mapOf(
                    DecodeHintType.POSSIBLE_FORMATS to listOf(BarcodeFormat.QR_CODE),
                    DecodeHintType.TRY_HARDER to true,
                    DecodeHintType.CHARACTER_SET to "UTF-8"
                )
            )
        }

        // 1. Full Image RGBLuminanceSource
        val fullSource = RGBLuminanceSource(width, height, pixels)
        val fullBinarizers = listOf(
            HybridBinarizer(fullSource),
            com.google.zxing.common.GlobalHistogramBinarizer(fullSource),
            HybridBinarizer(InvertedLuminanceSource(fullSource))
        )
        for (b in fullBinarizers) {
            try {
                val res = reader.decodeWithState(BinaryBitmap(b))
                if (res != null && res.text.isNotBlank()) return res.text
            } catch (_: Exception) {}
            reader.reset()
        }

        // 2. Center crop (75%)
        val cropW = (width * 0.75).toInt()
        val cropH = (height * 0.75).toInt()
        val cropX = (width - cropW) / 2
        val cropY = (height - cropH) / 2
        if (cropW > 50 && cropH > 50) {
            val croppedSource = fullSource.crop(cropX, cropY, cropW, cropH)
            val cropBinarizers = listOf(
                HybridBinarizer(croppedSource),
                com.google.zxing.common.GlobalHistogramBinarizer(croppedSource)
            )
            for (b in cropBinarizers) {
                try {
                    val res = reader.decodeWithState(BinaryBitmap(b))
                    if (res != null && res.text.isNotBlank()) return res.text
                } catch (_: Exception) {}
                reader.reset()
            }
        }

        // 3. Tighter center crop (50%)
        val tightW = (width * 0.50).toInt()
        val tightH = (height * 0.50).toInt()
        val tightX = (width - tightW) / 2
        val tightY = (height - tightH) / 2
        if (tightW > 50 && tightH > 50) {
            val tightSource = fullSource.crop(tightX, tightY, tightW, tightH)
            try {
                val res = reader.decodeWithState(BinaryBitmap(HybridBinarizer(tightSource)))
                if (res != null && res.text.isNotBlank()) return res.text
            } catch (_: Exception) {}
            reader.reset()
        }

        null
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

private fun calculateInSampleSize(options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {
    val height = options.outHeight
    val width = options.outWidth
    var inSampleSize = 1
    if (height > reqHeight || width > reqWidth) {
        val halfHeight = height / 2
        val halfWidth = width / 2
        while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth) {
            inSampleSize *= 2
        }
    }
    return inSampleSize
}

private fun extractPassIdentifier(rawText: String): String {
    val clean = rawText.trim()
    val passRegex = Regex("""(PASS-\d{3,6})""", RegexOption.IGNORE_CASE)
    val match = passRegex.find(clean)
    if (match != null) {
        return match.groupValues[1].uppercase()
    }

    if (clean.contains("::")) {
        val firstToken = clean.substringBefore("::").trim()
        if (firstToken.isNotBlank()) return firstToken
    }

    if (clean.startsWith("{") && clean.endsWith("}")) {
        val jsonIdRegex = Regex(""""id"\s*:\s*"([^"]+)"""")
        val jsonMatch = jsonIdRegex.find(clean)
        if (jsonMatch != null) {
            return jsonMatch.groupValues[1]
        }
    }

    return clean
}
