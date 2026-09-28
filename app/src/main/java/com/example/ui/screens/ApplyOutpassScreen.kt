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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocationCity
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import com.example.ui.components.GreenAnimatedButton
import com.example.ui.theme.CardBorderColor
import com.example.ui.theme.CrimsonError
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.GreenContainer
import com.example.ui.theme.GreenDark
import com.example.ui.theme.GreenPrimary
import com.example.ui.theme.IndigoPrimary
import com.example.ui.theme.TextDark
import com.example.ui.theme.TextMuted
import com.example.ui.theme.WhiteCard
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun ApplyOutpassScreen(
    currentUser: User?,
    onSubmitOutpass: (OutpassType, String, String, Long, Long) -> Unit,
    onNavigateBack: () -> Unit
) {
    // Only 2 Outpass Types: Local Outpass and Home Outpass (Emergency Outpass removed)
    var selectedType by remember { mutableStateOf(OutpassType.LOCAL) }
    var destination by remember { mutableStateOf("") }
    var reason by remember { mutableStateOf("") }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    val dfDateOnly = remember { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()) }
    val dfTimeOnly = remember { SimpleDateFormat("hh:mm a", Locale.getDefault()) }

    val now = remember { Calendar.getInstance() }
    val initialDepDate = remember { dfDateOnly.format(now.time) }
    val initialDepTime = remember { dfTimeOnly.format(now.time) }

    // Initial arrival time: 4 hours later for local pass
    val laterCalendar = remember {
        Calendar.getInstance().apply {
            add(Calendar.HOUR_OF_DAY, 4)
        }
    }
    val initialArrDate = remember { dfDateOnly.format(laterCalendar.time) }
    val initialArrTime = remember { dfTimeOnly.format(laterCalendar.time) }

    // Direct Filling Departure & Arriving Time Fields
    var departureDate by remember { mutableStateOf(initialDepDate) }
    var departureTime by remember { mutableStateOf(initialDepTime) }

    var arrivalDate by remember { mutableStateOf(initialArrDate) }
    var arrivalTime by remember { mutableStateOf(initialArrTime) }

    // Helper to calculate timestamp in ms from filled date and time
    fun computeTimeMs(dateStr: String, timeStr: String, fallbackMs: Long): Long {
        val formats = listOf(
            SimpleDateFormat("dd MMM yyyy hh:mm a", Locale.getDefault()),
            SimpleDateFormat("dd MMM yyyy HH:mm", Locale.getDefault()),
            SimpleDateFormat("dd/MM/yyyy hh:mm a", Locale.getDefault()),
            SimpleDateFormat("dd-MM-yyyy hh:mm a", Locale.getDefault()),
            SimpleDateFormat("yyyy-MM-dd hh:mm a", Locale.getDefault())
        )
        val combined = "${dateStr.trim()} ${timeStr.trim()}"
        for (fmt in formats) {
            try {
                val d = fmt.parse(combined)
                if (d != null) return d.time
            } catch (_: Exception) {}
        }
        return fallbackMs
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
    ) {
        item {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                IconButton(onClick = onNavigateBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextDark)
                }
                Spacer(modifier = Modifier.width(4.dp))
                Column {
                    Text(
                        text = "Apply for Digital Outpass",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = TextDark
                    )
                    Text(
                        text = "Submit request for Staff Advisor & HOD approval",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 1. Select Outpass Type (Only Local and Home)
            Text(
                text = "1. Select Outpass Type",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = GreenDark
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutpassOptionCard(
                    title = "Local Outpass",
                    subtitle = "Day outing, city/market visit",
                    badge = "Same-Day Return",
                    badgeColor = GreenPrimary,
                    icon = Icons.Default.LocationCity,
                    selected = selectedType == OutpassType.LOCAL,
                    onSelect = {
                        selectedType = OutpassType.LOCAL
                        // Reset arrival date to same day
                        arrivalDate = departureDate
                    },
                    modifier = Modifier.weight(1f)
                )

                OutpassOptionCard(
                    title = "Home Outpass",
                    subtitle = "Weekend / family home leave",
                    badge = "Multi-Day Leave",
                    badgeColor = IndigoPrimary,
                    icon = Icons.Default.Home,
                    selected = selectedType == OutpassType.HOME,
                    onSelect = {
                        selectedType = OutpassType.HOME
                        // Set arrival date to 2 days later
                        val cal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, 2) }
                        arrivalDate = dfDateOnly.format(cal.time)
                    },
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            val isHomePass = selectedType == OutpassType.HOME

            // 2. Filling Departure Time (and Arriving Time for Local Outpass)
            Text(
                text = if (isHomePass) "2. Fill Departure Details (Out Time)" else "2. Fill Departure Time & Arriving Time",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = GreenDark
            )

            Spacer(modifier = Modifier.height(8.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, CardBorderColor),
                colors = CardDefaults.cardColors(containerColor = WhiteCard)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    // DEPARTURE TIME FILLING
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(GreenContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Schedule, contentDescription = null, tint = GreenDark, modifier = Modifier.size(18.dp))
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "Departure Details (Out Time)",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = TextDark
                            )
                            Text(
                                text = "Fill the exact date & time you leave college",
                                fontSize = 11.sp,
                                color = TextMuted
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = departureDate,
                            onValueChange = {
                                departureDate = it
                                errorMsg = null
                            },
                            label = { Text("Departure Date") },
                            placeholder = { Text("e.g. 27 Sep 2026") },
                            leadingIcon = { Icon(Icons.Default.CalendarToday, contentDescription = null, tint = GreenPrimary, modifier = Modifier.size(16.dp)) },
                            modifier = Modifier.weight(1.1f),
                            colors = inputFieldColors(),
                            singleLine = true
                        )

                        OutlinedTextField(
                            value = departureTime,
                            onValueChange = {
                                departureTime = it
                                errorMsg = null
                            },
                            label = { Text("Departure Time") },
                            placeholder = { Text("e.g. 02:30 PM") },
                            leadingIcon = { Icon(Icons.Default.Schedule, contentDescription = null, tint = GreenPrimary, modifier = Modifier.size(16.dp)) },
                            modifier = Modifier.weight(1f),
                            colors = inputFieldColors(),
                            singleLine = true
                        )
                    }

                    // Quick-fill buttons for Departure
                    Spacer(modifier = Modifier.height(8.dp))
                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        item {
                            QuickFillChip(
                                label = "Now",
                                onClick = {
                                    val c = Calendar.getInstance()
                                    departureDate = dfDateOnly.format(c.time)
                                    departureTime = dfTimeOnly.format(c.time)
                                }
                            )
                        }
                        item {
                            QuickFillChip(
                                label = "Today",
                                onClick = {
                                    departureDate = dfDateOnly.format(Calendar.getInstance().time)
                                }
                            )
                        }
                        item {
                            QuickFillChip(
                                label = "Tomorrow",
                                onClick = {
                                    val c = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, 1) }
                                    departureDate = dfDateOnly.format(c.time)
                                }
                            )
                        }
                    }

                    // ARRIVING TIME FILLING: Shown ONLY for Local Outpass
                    if (!isHomePass) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(CardBorderColor))
                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(IndigoPrimary.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Alarm, contentDescription = null, tint = IndigoPrimary, modifier = Modifier.size(18.dp))
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "Arriving Details (Return Time)",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = TextDark
                                )
                                Text(
                                    text = "Fill the exact date & time you return to college",
                                    fontSize = 11.sp,
                                    color = TextMuted
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = arrivalDate,
                                onValueChange = {
                                    arrivalDate = it
                                    errorMsg = null
                                },
                                label = { Text("Arriving Date") },
                                placeholder = { Text("e.g. 27 Sep 2026") },
                                leadingIcon = { Icon(Icons.Default.CalendarToday, contentDescription = null, tint = IndigoPrimary, modifier = Modifier.size(16.dp)) },
                                modifier = Modifier.weight(1.1f),
                                colors = inputFieldColors(),
                                singleLine = true
                            )

                            OutlinedTextField(
                                value = arrivalTime,
                                onValueChange = {
                                    arrivalTime = it
                                    errorMsg = null
                                },
                                label = { Text("Arriving Time") },
                                placeholder = { Text("e.g. 06:30 PM") },
                                leadingIcon = { Icon(Icons.Default.Alarm, contentDescription = null, tint = IndigoPrimary, modifier = Modifier.size(16.dp)) },
                                modifier = Modifier.weight(1f),
                                colors = inputFieldColors(),
                                singleLine = true
                            )
                        }

                        // Quick-fill buttons for Arrival (Smooth scrollable LazyRow with fixed-width, unclipped chips)
                        Spacer(modifier = Modifier.height(8.dp))
                        LazyRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            item {
                                QuickFillChip(
                                    label = "Same Day",
                                    onClick = {
                                        arrivalDate = departureDate
                                    }
                                )
                            }
                            item {
                                QuickFillChip(
                                    label = "Tomorrow",
                                    onClick = {
                                        val c = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, 1) }
                                        arrivalDate = dfDateOnly.format(c.time)
                                    }
                                )
                            }
                            item {
                                QuickFillChip(
                                    label = "In 2 Days",
                                    onClick = {
                                        val c = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, 2) }
                                        arrivalDate = dfDateOnly.format(c.time)
                                    }
                                )
                            }
                            item {
                                QuickFillChip(
                                    label = "06:30 PM",
                                    onClick = { arrivalTime = "06:30 PM" }
                                )
                            }
                            item {
                                QuickFillChip(
                                    label = "08:00 PM",
                                    onClick = { arrivalTime = "08:00 PM" }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Filled Time Confirmation Box
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(containerColor = GreenContainer.copy(alpha = 0.5f)),
                        border = androidx.compose.foundation.BorderStroke(1.dp, GreenPrimary.copy(alpha = 0.3f))
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("Departure: ", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = GreenDark)
                                Text("$departureDate at $departureTime", fontSize = 12.sp, color = TextDark)
                            }
                            if (!isHomePass) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("Arriving: ", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = IndigoPrimary)
                                    Text("$arrivalDate at $arrivalTime", fontSize = 12.sp, color = TextDark)
                                }
                            } else {
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("Pass Type: ", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = IndigoPrimary)
                                    Text("Home Outpass (Going Home Leave)", fontSize = 12.sp, color = TextDark)
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 3. Destination & Reason
            Text(
                text = "3. Destination & Purpose Details",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = GreenDark
            )

            Spacer(modifier = Modifier.height(8.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, CardBorderColor),
                colors = CardDefaults.cardColors(containerColor = WhiteCard)
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
                        placeholder = { Text("e.g. City Market / Home") },
                        leadingIcon = { Icon(Icons.Default.LocationOn, contentDescription = null, tint = GreenPrimary) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = inputFieldColors(),
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
                        placeholder = { Text("e.g. Purchase study materials, family festival") },
                        leadingIcon = { Icon(Icons.Default.EditNote, contentDescription = null, tint = GreenPrimary) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = inputFieldColors(),
                        maxLines = 3
                    )

                    if (errorMsg != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = errorMsg!!,
                            color = CrimsonError,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    GreenAnimatedButton(
                        onClick = {
                            val nowMs = System.currentTimeMillis()
                            val outMs = computeTimeMs(departureDate, departureTime, nowMs)

                            if (destination.isBlank() || reason.isBlank()) {
                                errorMsg = "Please enter both destination and purpose details."
                            } else if (departureDate.isBlank() || departureTime.isBlank()) {
                                errorMsg = "Please fill in the departure date and time."
                            } else if (!isHomePass) {
                                val returnMs = computeTimeMs(arrivalDate, arrivalTime, nowMs + (4 * 3600_000L))
                                if (arrivalDate.isBlank() || arrivalTime.isBlank()) {
                                    errorMsg = "Please fill in the arriving date and time."
                                } else if (returnMs <= outMs) {
                                    errorMsg = "Arriving time must be after the departure time. Please check the filled date and time."
                                } else {
                                    onSubmitOutpass(selectedType, destination.trim(), reason.trim(), outMs, returnMs)
                                    onNavigateBack()
                                }
                            } else {
                                // For Home Outpass: no arriving time required
                                val homeReturnMs = outMs + (14 * 24 * 3600_000L) // 14-day leave window placeholder
                                onSubmitOutpass(selectedType, destination.trim(), reason.trim(), outMs, homeReturnMs)
                                onNavigateBack()
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Send, contentDescription = null, tint = Color.White)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Submit Digital Outpass Request", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun QuickFillChip(
    label: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(GreenContainer)
            .border(1.dp, GreenPrimary.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .padding(horizontal = 10.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontSize = 11.5.sp,
            fontWeight = FontWeight.Bold,
            color = GreenDark,
            maxLines = 1,
            softWrap = false
        )
    }
}

@Composable
private fun OutpassOptionCard(
    title: String,
    subtitle: String,
    badge: String,
    badgeColor: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean,
    onSelect: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.clickable { onSelect() },
        shape = RoundedCornerShape(14.dp),
        border = androidx.compose.foundation.BorderStroke(
            if (selected) 2.dp else 1.dp,
            if (selected) GreenPrimary else CardBorderColor
        ),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) GreenContainer.copy(alpha = 0.7f) else WhiteCard
        )
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(icon, contentDescription = null, tint = if (selected) GreenDark else TextMuted, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = TextDark
                    )
                }
                if (selected) {
                    Box(
                        modifier = Modifier
                            .size(18.dp)
                            .clip(CircleShape)
                            .background(GreenPrimary),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(12.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = TextMuted,
                fontSize = 11.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(badgeColor.copy(alpha = 0.15f))
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            ) {
                Text(
                    text = badge,
                    color = badgeColor,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun inputFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = GreenPrimary,
    unfocusedBorderColor = CardBorderColor,
    focusedContainerColor = Color.White,
    unfocusedContainerColor = Color.White,
    focusedTextColor = Color.Black,
    unfocusedTextColor = Color.Black,
    cursorColor = Color.Black
)
