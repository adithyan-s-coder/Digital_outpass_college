package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.example.data.models.CompleteAiReport
import com.example.data.models.Outpass
import com.example.data.models.OutpassStatus
import com.example.data.models.ReportDatePreset
import com.example.data.models.User
import com.example.data.models.UserRole
import com.example.ui.components.AiReportButton
import com.example.ui.components.AiReportModal
import com.example.ui.components.StatusBadge
import com.example.ui.theme.IndigoPrimary
import com.example.ui.theme.Slate700
import com.example.ui.theme.Slate800
import com.example.ui.theme.Slate900
import com.example.ui.theme.TextDark
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
    val context = LocalContext.current
    var showReportDialog by remember { mutableStateOf(false) }

    val df = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault())

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
                    text = "Comprehensive record of all past digital passes",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
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
                    label = { Text("All Passes") },
                    colors = chipColors()
                )
            }
            items(OutpassStatus.values()) { status ->
                FilterChip(
                    selected = selectedFilter == status,
                    onClick = { onFilterSelect(status) },
                    label = { Text(status.displayName) },
                    colors = chipColors()
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (outpasses.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Slate700),
                colors = CardDefaults.cardColors(containerColor = Slate800)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "No Outpasses Found",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "There are no outpasses matching the selected filter.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(outpasses) { pass ->
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
                Column {
                    Text(
                        text = outpass.id,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = IndigoPrimary
                    )
                    Text(
                        text = "${outpass.studentName} (${outpass.regNo})",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = TextDark
                    )
                }

                StatusBadge(status = outpass.status)
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Destination: ${outpass.destination}",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = TextDark
            )

            Text(
                text = "Reason: ${outpass.reason}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Applied: ${df.format(Date(outpass.appliedAt))}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 10.sp
                )

                Text(
                    text = "Return: ${df.format(Date(outpass.returnDateTime))}",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextDark,
                    fontWeight = FontWeight.SemiBold,
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
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 10.sp,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    outpass.hodApproval?.let {
                        Text(
                            text = "HOD: ${it.approverName} (${it.status})",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
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
    selectedContainerColor = IndigoPrimary,
    selectedLabelColor = Color.White,
    containerColor = Slate800,
    labelColor = TextDark
)
