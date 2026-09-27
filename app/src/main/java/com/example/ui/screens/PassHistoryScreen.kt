package com.example.ui.screens

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.models.CompleteAiReport
import com.example.data.models.Outpass
import com.example.data.models.OutpassStatus
import com.example.data.models.ReportDatePreset
import com.example.data.models.User
import com.example.data.models.UserRole
import com.example.ui.components.AiReportButton
import com.example.ui.components.AiReportModal
import com.example.ui.components.StatusBadge
import com.example.ui.theme.CardBorderColor
import com.example.ui.theme.GreenContainer
import com.example.ui.theme.GreenDark
import com.example.ui.theme.GreenPrimary
import com.example.ui.theme.IndigoPrimary
import com.example.ui.theme.Slate700
import com.example.ui.theme.Slate800
import com.example.ui.theme.TextDark
import com.example.ui.theme.TextMuted
import com.example.ui.theme.WhiteCard
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun PassHistoryScreen(
    outpasses: List<Outpass>,
    selectedFilter: OutpassStatus?,
    onFilterSelect: (OutpassStatus?) -> Unit,
    currentUser: User? = null,
    currentAiReport: CompleteAiReport? = null,
    isGeneratingAiReport: Boolean = false,
    aiReportError: String? = null,
    onGenerateAiReport: ((ReportDatePreset, Long?, Long?) -> Unit)? = null,
    onDismissAiReport: (() -> Unit)? = null
) {
    var showReportDialog by remember { mutableStateOf(false) }
    val df = remember { SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()) }

    // Filter strictly by the selected tab / filter
    val displayedOutpasses = remember(outpasses, selectedFilter) {
        if (selectedFilter == null) outpasses
        else outpasses.filter { it.status == selectedFilter }
    }

    // Dynamic Heading based on current category
    val headingTitle = when (selectedFilter) {
        null -> "All Student Outpass History"
        OutpassStatus.PENDING_STAFF -> "Staff Approval Pending History"
        OutpassStatus.PENDING_HOD -> "Pending HOD Sign-Off History"
        OutpassStatus.APPROVED -> "Approved Outpass History"
        OutpassStatus.CHECKED_OUT -> "Outside Campus Active History"
        OutpassStatus.CHECKED_IN -> "Returned & Completed History"
        OutpassStatus.REJECTED -> "Rejected Outpass History"
        OutpassStatus.EXPIRED -> "Expired Outpass History"
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Outpass History & Logs",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = TextDark
                )

                Text(
                    text = "Comprehensive record of student digital passes",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMuted
                )
            }

            if (currentUser != null && (currentUser.role == UserRole.STAFF_ADVISOR || currentUser.role == UserRole.HOD)) {
                AiReportButton(
                    role = currentUser.role,
                    onClick = { showReportDialog = true }
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Filter Chips Row
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            item {
                FilterChip(
                    selected = selectedFilter == null,
                    onClick = { onFilterSelect(null) },
                    label = { Text("All Passes", fontWeight = if (selectedFilter == null) FontWeight.Bold else FontWeight.Normal) },
                    colors = chipColors()
                )
            }
            items(OutpassStatus.values()) { status ->
                val isSelected = selectedFilter == status
                FilterChip(
                    selected = isSelected,
                    onClick = { onFilterSelect(status) },
                    label = { Text(status.displayName, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                    colors = chipColors()
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Specific Heading matching the selected filter category
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = headingTitle,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = TextDark
            )
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(GreenContainer)
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            ) {
                Text(
                    text = "${displayedOutpasses.size} records",
                    style = MaterialTheme.typography.labelSmall,
                    color = GreenDark,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        if (displayedOutpasses.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, CardBorderColor),
                colors = CardDefaults.cardColors(containerColor = WhiteCard)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.History,
                        contentDescription = null,
                        tint = TextMuted,
                        modifier = Modifier.size(36.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "No Records in ${selectedFilter?.displayName ?: "All Passes"}",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = TextDark
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "There are currently no outpass logs matching this heading.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted
                    )
                }
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(displayedOutpasses, key = { it.id }) { pass ->
                    HistoryPassCard(outpass = pass, df = df)
                }
            }
        }
    }

    if ((showReportDialog || currentAiReport != null) && currentUser != null) {
        AiReportModal(
            currentUser = currentUser,
            allOutpasses = outpasses,
            currentReport = currentAiReport,
            isGenerating = isGeneratingAiReport,
            errorMessage = aiReportError,
            onGenerate = { preset, start, end ->
                onGenerateAiReport?.invoke(preset, start, end)
            },
            onDismiss = {
                showReportDialog = false
                onDismissAiReport?.invoke()
            }
        )
    }
}

@Composable
private fun HistoryPassCard(outpass: Outpass, df: SimpleDateFormat) {
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    // Student Photo / Avatar like in Student Dashboard
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(GreenContainer)
                            .border(1.dp, GreenPrimary, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        if (outpass.studentPhotoUri != null) {
                            AsyncImage(
                                model = outpass.studentPhotoUri,
                                contentDescription = outpass.studentName,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                tint = GreenDark,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column {
                        Text(
                            text = outpass.id,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = GreenDark
                        )
                        Text(
                            text = "${outpass.studentName} (${outpass.regNo})",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = TextDark
                        )
                        Text(
                            text = "${outpass.department} • ${outpass.type.displayName}",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextMuted,
                            fontSize = 10.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))
                StatusBadge(status = outpass.status)
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "Destination: ${outpass.destination}",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = TextDark
            )

            Text(
                text = "Reason: ${outpass.reason}",
                style = MaterialTheme.typography.bodySmall,
                color = TextMuted
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Applied: ${df.format(Date(outpass.appliedAt))}",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMuted,
                    fontSize = 10.sp
                )

                Text(
                    text = "Return: ${df.format(Date(outpass.returnDateTime))}",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextDark,
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.sp
                )
            }

            if (outpass.staffApproval != null || outpass.hodApproval != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth()) {
                    outpass.staffApproval?.let {
                        Text(
                            text = "Advisor: ${it.approverName} (${it.status})",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextMuted,
                            fontSize = 10.sp,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    outpass.hodApproval?.let {
                        Text(
                            text = "HOD: ${it.approverName} (${it.status})",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextMuted,
                            fontSize = 10.sp,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun chipColors() = FilterChipDefaults.filterChipColors(
    selectedContainerColor = GreenPrimary,
    selectedLabelColor = Color.White,
    containerColor = WhiteCard,
    labelColor = TextDark
)
