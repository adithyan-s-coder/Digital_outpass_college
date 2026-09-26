package com.example.ui.screens

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.models.OutpassType
import com.example.data.models.User
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.IndigoPrimary
import com.example.ui.theme.Slate700
import com.example.ui.theme.Slate800
import com.example.ui.theme.Slate900
import com.example.ui.theme.TextDark
import com.example.ui.theme.GreenPrimary
import com.example.ui.theme.GreenContainer
import com.example.ui.components.GreenAnimatedButton
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ApplyOutpassScreen(
    currentUser: User?,
    onSubmitOutpass: (OutpassType, String, String, Long, Long) -> Unit,
    onNavigateBack: () -> Unit
) {
    var selectedType by remember { mutableStateOf(OutpassType.LOCAL) }
    var destination by remember { mutableStateOf("") }
    var reason by remember { mutableStateOf("") }
    var durationHours by remember { mutableStateOf(4) }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    val nowMs = System.currentTimeMillis()
    val hourMs = 3600_000L
    val df = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault())

    val outTime = nowMs
    val returnTime = nowMs + (durationHours * hourMs)

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
    ) {
        item {
            Text(
                text = "Apply for Digital Outpass",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = TextDark
            )

            Text(
                text = "Submit request for Staff Advisor & HOD approval",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Select Pass Type Cards
            Text(
                text = "1. Select Outpass Type",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = IndigoPrimary
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutpassTypeCard(
                    type = OutpassType.LOCAL,
                    selected = selectedType == OutpassType.LOCAL,
                    onSelect = {
                        selectedType = OutpassType.LOCAL
                        durationHours = 4
                    },
                    modifier = Modifier.weight(1f)
                )
                OutpassTypeCard(
                    type = OutpassType.HOME,
                    selected = selectedType == OutpassType.HOME,
                    onSelect = {
                        selectedType = OutpassType.HOME
                        durationHours = 48
                    },
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutpassTypeCard(
                    type = OutpassType.EMERGENCY,
                    selected = selectedType == OutpassType.EMERGENCY,
                    onSelect = {
                        selectedType = OutpassType.EMERGENCY
                        durationHours = 12
                    },
                    modifier = Modifier.weight(1f)
                )
                OutpassTypeCard(
                    type = OutpassType.SPECIAL_EVENT,
                    selected = selectedType == OutpassType.SPECIAL_EVENT,
                    onSelect = {
                        selectedType = OutpassType.SPECIAL_EVENT
                        durationHours = 24
                    },
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Outpass Details Form
            Text(
                text = "2. Destination & Reason Details",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = IndigoPrimary
            )

            Spacer(modifier = Modifier.height(8.dp))

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
                    OutlinedTextField(
                        value = destination,
                        onValueChange = {
                            destination = it
                            errorMsg = null
                        },
                        label = { Text("Destination / Place of Visit") },
                        placeholder = { Text("e.g. Green Valley Market / Home") },
                        leadingIcon = { Icon(Icons.Default.LocationOn, contentDescription = null, tint = IndigoPrimary) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = fieldColors(),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = reason,
                        onValueChange = {
                            reason = it
                            errorMsg = null
                        },
                        label = { Text("Detailed Purpose / Reason") },
                        placeholder = { Text("e.g. Buying lab stationery and course books") },
                        leadingIcon = { Icon(Icons.Default.EditNote, contentDescription = null, tint = IndigoPrimary) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = fieldColors(),
                        maxLines = 3
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "3. Duration Preset",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(3, 6, 12, 24, 48).forEach { hrs ->
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (durationHours == hrs) IndigoPrimary else Slate900)
                                    .clickable { durationHours = hrs }
                                    .padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "${hrs}h",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (durationHours == hrs) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Date & Time Summary Card
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .padding(12.dp)
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.CalendarToday, contentDescription = null, tint = EmeraldSuccess, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Estimated Exit: ",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = df.format(Date(outTime)),
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold,
                                    color = TextDark
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Alarm, contentDescription = null, tint = EmeraldSuccess, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Expected Return: ",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = df.format(Date(returnTime)),
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold,
                                    color = EmeraldSuccess
                                )
                            }
                        }
                    }

                    if (errorMsg != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = errorMsg!!,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    GreenAnimatedButton(
                        onClick = {
                            if (destination.isBlank() || reason.isBlank()) {
                                errorMsg = "Please enter destination and reason."
                            } else {
                                onSubmitOutpass(selectedType, destination, reason, outTime, returnTime)
                                onNavigateBack()
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Send, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Submit Outpass Application", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }

            Spacer(modifier = Modifier.height(30.dp))
        }
    }
}

@Composable
private fun OutpassTypeCard(
    type: OutpassType,
    selected: Boolean,
    onSelect: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.clickable { onSelect() },
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, if (selected) GreenPrimary else Slate700),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) GreenPrimary else Color.White
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = type.displayName,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = if (selected) Color.White else TextDark
                )
                if (selected) {
                    Icon(imageVector = Icons.Default.Check, contentDescription = "Selected", tint = Color.White, modifier = Modifier.size(16.dp))
                }
            }
            Text(
                text = "Max ${type.maxHours} hours",
                style = MaterialTheme.typography.labelSmall,
                color = if (selected) Color.White.copy(alpha = 0.85f) else MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 10.sp
            )
        }
    }
}

@Composable
private fun fieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = IndigoPrimary,
    unfocusedBorderColor = Slate700,
    focusedContainerColor = Color.White,
    unfocusedContainerColor = Color.White,
    focusedTextColor = Color.Black,
    unfocusedTextColor = Color.Black,
    cursorColor = Color.Black
)
