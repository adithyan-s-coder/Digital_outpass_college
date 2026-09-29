package com.example.ui.screens

import com.example.ui.theme.*

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import com.example.ui.components.CameraQrScannerView
import com.example.ui.components.UserAvatar
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.models.GateLog
import com.example.data.models.Outpass
import com.example.data.models.OutpassStatus
import com.example.ui.components.StatusBadge
import com.example.ui.components.GreenAnimatedButton
import com.example.ui.theme.TextDark
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.IndigoPrimary
import com.example.ui.theme.Slate700
import com.example.ui.theme.Slate800
import com.example.ui.theme.Slate900
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun SecurityGateScreen(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onScanPass: (String) -> Unit = onSearchQueryChange,
    searchResults: List<Outpass>,
    gateLogs: List<GateLog>,
    onCheckOut: (String) -> Unit,
    onCheckIn: (String) -> Unit
) {
    var isSimulatingScanner by remember { mutableStateOf(false) }
    val df = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault())

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
    ) {
        Text(
            text = "Main Gate Verification Tool",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = TextDark
        )

        Text(
            text = "Scan Student QR Pass or Search Register Number",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Gate QR Scanner Simulator Toggle Box
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, Slate700),
            colors = CardDefaults.cardColors(containerColor = Slate800)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (!isSimulatingScanner) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(IndigoPrimary)
                            .clickable { isSimulatingScanner = true }
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(imageVector = Icons.Default.CameraAlt, contentDescription = null, tint = Color.White)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Launch Gate Camera Scanner",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                } else {
                    // Real CameraX QR Code Scanner
                    CameraQrScannerView(
                        onScanSuccess = { scannedPassData ->
                            onScanPass(scannedPassData)
                            isSimulatingScanner = false
                        },
                        onCloseScanner = { isSimulatingScanner = false }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Manual Search / QR Code ID Entry Bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchQueryChange,
            label = { Text("QR Code ID / Generated Pass Number / Reg No") },
            placeholder = { Text("e.g. PASS-1001 or 21CS045") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = IndigoPrimary) },
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = IndigoPrimary,
                unfocusedBorderColor = Slate700,
                focusedContainerColor = Color.White,
                unfocusedContainerColor = Color.White,
                focusedTextColor = TextDark,
                unfocusedTextColor = TextDark
            ),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Search Results List
        Text(
            text = "Pass Search Results (${searchResults.size})",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = TextDark
        )

        Spacer(modifier = Modifier.height(8.dp))

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(searchResults) { pass ->
                GatePassSearchResultCard(
                    outpass = pass,
                    df = df,
                    onCheckOut = { onCheckOut(pass.id) },
                    onCheckIn = { onCheckIn(pass.id) }
                )
            }
        }
    }
}

@Composable
private fun GatePassSearchResultCard(
    outpass: Outpass,
    df: SimpleDateFormat,
    onCheckOut: () -> Unit,
    onCheckIn: () -> Unit
) {
    val durationHours = ((outpass.returnDateTime - outpass.outDateTime) / 3600_000L).coerceAtLeast(1)
    val isLongPermanentLeave = durationHours >= 12

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Slate700),
        colors = CardDefaults.cardColors(containerColor = Slate800)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    UserAvatar(
                        photoUri = outpass.studentPhotoUri,
                        name = outpass.studentName,
                        role = com.example.data.models.UserRole.STUDENT,
                        size = 44.dp
                    )

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text(
                            text = "${outpass.studentName} (${outpass.regNo})",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextDark
                        )
                        Text(
                            text = "PASS: ${outpass.id} • Dept: ${outpass.department}",
                            style = MaterialTheme.typography.bodySmall,
                            color = GreenDark,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))
                StatusBadge(status = outpass.status)
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Destination: ${outpass.destination} (${outpass.type.displayName} - ${durationHours}h)",
                style = MaterialTheme.typography.bodySmall,
                color = TextDark
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Gate Exit: ${outpass.actualCheckOutTime?.let { df.format(Date(it)) } ?: "Not Checked Out"}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (outpass.type == com.example.data.models.OutpassType.LOCAL) {
                    Text(
                        text = "Expected Return: ${df.format(Date(outpass.returnDateTime))}",
                        style = MaterialTheme.typography.labelSmall,
                        color = EmeraldSuccess,
                        fontWeight = FontWeight.Bold
                    )
                } else {
                    Text(
                        text = "Pass: Home Outpass",
                        style = MaterialTheme.typography.labelSmall,
                        color = IndigoPrimary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (outpass.status == OutpassStatus.APPROVED) {
                    if (outpass.isQrUsed) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f))
                                .padding(10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "QR CODE EXPIRED: Already used for gate exit. Re-use prohibited.",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    } else if (outpass.isQrExpired()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f))
                                .padding(10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "QR CODE EXPIRED: 1-Hour validity window has passed.",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.error
                                )
                                Text(
                                    text = "QR pass is only valid for 1 hour after approval. Gate exit denied.",
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    } else {
                        GreenAnimatedButton(
                            onClick = onCheckOut,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("1-Tap Gate CHECK-OUT (Scan & Expire Exit QR)", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                } else if (outpass.status == OutpassStatus.CHECKED_OUT) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(GreenContainer)
                                .padding(10.dp)
                        ) {
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = GreenDark, modifier = Modifier.size(15.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (outpass.type == com.example.data.models.OutpassType.HOME) "Student Departed Campus for Home:" else "Student Currently Outside Campus:",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = GreenDark
                                    )
                                }
                                Spacer(modifier = Modifier.height(3.dp))
                                Text(
                                    text = "Exited: ${outpass.actualCheckOutTime?.let { df.format(Date(it)) } ?: "Exited"}",
                                    fontSize = 11.sp,
                                    color = TextDark
                                )
                                if (outpass.type == com.example.data.models.OutpassType.LOCAL) {
                                    Text(
                                        text = "Expected Return: ${df.format(Date(outpass.returnDateTime))}",
                                        fontSize = 11.sp,
                                        color = TextDark
                                    )
                                    Text(
                                        text = "If student returns to college today, tap below to record same-day return.",
                                        fontSize = 10.sp,
                                        color = TextMuted
                                    )
                                } else {
                                    Text(
                                        text = "Home Outpass: Student departed for home. Return gate scan is not required.",
                                        fontSize = 10.sp,
                                        color = TextMuted
                                    )
                                }
                            }
                        }
                        if (outpass.type == com.example.data.models.OutpassType.LOCAL) {
                            Spacer(modifier = Modifier.height(10.dp))
                            GreenAnimatedButton(
                                onClick = onCheckIn,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(Icons.AutoMirrored.Filled.Login, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("1-Tap Same-Day RETURNED (Student Entered Campus)", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                        }
                    }
                } else if (outpass.status == OutpassStatus.CHECKED_IN) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Slate700)
                            .padding(8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Pass Completed (Student returned inside campus)", style = MaterialTheme.typography.labelSmall, color = EmeraldSuccess)
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Slate700)
                            .padding(8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Pass Not Approved Yet (Exit Denied)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }
}
