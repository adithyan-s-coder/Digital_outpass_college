package com.example.ui.components

import android.content.Intent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Psychology
import android.widget.Toast
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.ListAlt
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Warning
import com.example.util.PdfExporter
import com.example.ui.components.StatusBadge
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.models.CompleteAiReport
import com.example.data.models.Outpass
import com.example.data.models.ReportDatePreset
import com.example.data.models.User
import com.example.data.models.UserRole
import com.example.ui.theme.AmberContainer
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.CardBorderColor
import com.example.ui.theme.CrimsonContainer
import com.example.ui.theme.CrimsonError
import com.example.ui.theme.EmeraldContainer
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.GreenContainer
import com.example.ui.theme.GreenDark
import com.example.ui.theme.GreenPrimary
import com.example.ui.theme.TextDark
import com.example.ui.theme.TextMuted
import com.example.ui.theme.WhiteCard
import com.example.util.OutpassStatisticsCalculator
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun AiReportButton(
    role: UserRole,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(
            containerColor = Color(0xFF1E1B4B),
            contentColor = Color(0xFFFCD34D)
        ),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, Color(0xFFF59E0B).copy(alpha = 0.6f)),
        modifier = modifier
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.Description,
                contentDescription = "Generate Report",
                tint = Color(0xFFFCD34D),
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "Generate Report",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AiReportModal(
    currentUser: User,
    allOutpasses: List<Outpass>,
    currentReport: CompleteAiReport?,
    isGenerating: Boolean,
    errorMessage: String?,
    onGenerate: (preset: ReportDatePreset, customStart: Long?, customEnd: Long?) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val dateTimeDf = remember { SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()) }
    val timeDf = remember { SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()) }
    val dateOnlyDf = remember { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()) }

    var selectedPreset by remember { mutableStateOf(ReportDatePreset.THIS_WEEK) }
    var customDaysBack by remember { mutableStateOf(7) }
    var userWantsReanalyze by remember { mutableStateOf(false) }

    val nowMs = System.currentTimeMillis()
    val customStartMs = nowMs - (customDaysBack * 86400_000L)
    val customEndMs = nowMs

    // Live list and count of matching student outpass records
    val filteredOutpasses = remember(selectedPreset, customDaysBack, allOutpasses, currentUser) {
        val (s, e) = OutpassStatisticsCalculator.getDateRangeBounds(selectedPreset, customStartMs, customEndMs)
        val deptScoped = if (currentUser.role == UserRole.ADMIN) allOutpasses
        else allOutpasses.filter { it.department.equals(currentUser.department, ignoreCase = true) }
        deptScoped.filter { pass ->
            val t = if (pass.appliedAt > 0) pass.appliedAt else pass.outDateTime
            t in s..e || pass.outDateTime in s..e
        }.sortedByDescending { if (it.appliedAt > 0) it.appliedAt else it.outDateTime }
    }
    val previewCount = filteredOutpasses.size

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.92f)
                .padding(vertical = 12.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = WhiteCard),
            border = BorderStroke(1.5.dp, CardBorderColor)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // Modal Top Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(GreenContainer)
                                .border(1.dp, GreenPrimary, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = GreenDark,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "AI-POWERED OUTPASS REPORT",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = TextDark
                            )
                            Text(
                                text = "${currentUser.role.displayName} • ${currentUser.department}",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                                color = GreenDark,
                                fontSize = 12.sp
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = TextDark
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                if (currentReport == null || userWantsReanalyze) {
                    // STEP 1: DATE RANGE SELECTION & GENERATION TRIGGER
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                                border = BorderStroke(1.dp, CardBorderColor)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            Icons.Default.DateRange,
                                            contentDescription = null,
                                            tint = GreenDark,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "Select Report Date Range",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = TextDark
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Only database records within the chosen period will be analyzed.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextMuted,
                                        fontSize = 11.sp
                                    )

                                    Spacer(modifier = Modifier.height(12.dp))

                                    FlowRow(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        ReportDatePreset.values().forEach { preset ->
                                            val isSelected = selectedPreset == preset
                                            FilterChip(
                                                selected = isSelected,
                                                onClick = { selectedPreset = preset },
                                                label = {
                                                    Text(
                                                        preset.displayName,
                                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                                    )
                                                },
                                                colors = FilterChipDefaults.filterChipColors(
                                                    selectedContainerColor = GreenPrimary,
                                                    selectedLabelColor = Color.White,
                                                    containerColor = WhiteCard,
                                                    labelColor = TextDark
                                                ),
                                                border = FilterChipDefaults.filterChipBorder(
                                                    enabled = true,
                                                    selected = isSelected,
                                                    borderColor = CardBorderColor,
                                                    selectedBorderColor = GreenPrimary
                                                )
                                            )
                                        }
                                    }

                                    if (selectedPreset == ReportDatePreset.CUSTOM) {
                                        Spacer(modifier = Modifier.height(12.dp))
                                        Card(
                                            shape = RoundedCornerShape(12.dp),
                                            colors = CardDefaults.cardColors(containerColor = WhiteCard),
                                            border = BorderStroke(1.dp, GreenPrimary.copy(alpha = 0.5f)),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Column(modifier = Modifier.padding(12.dp)) {
                                                Text(
                                                    text = "Custom Range: Past $customDaysBack Days",
                                                    style = MaterialTheme.typography.labelMedium,
                                                    fontWeight = FontWeight.Bold,
                                                    color = TextDark
                                                )
                                                Text(
                                                    text = "${dateOnlyDf.format(Date(customStartMs))} to ${dateOnlyDf.format(Date(customEndMs))}",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = GreenDark,
                                                    fontWeight = FontWeight.SemiBold,
                                                    fontSize = 11.sp
                                                )
                                                Spacer(modifier = Modifier.height(8.dp))
                                                Row(
                                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    listOf(3, 7, 14, 30, 60).forEach { days ->
                                                        val sel = customDaysBack == days
                                                        Box(
                                                            modifier = Modifier
                                                                .clip(RoundedCornerShape(8.dp))
                                                                .background(if (sel) GreenPrimary else Color(0xFFF1F5F9))
                                                                .clickable { customDaysBack = days }
                                                                .padding(horizontal = 10.dp, vertical = 6.dp)
                                                        ) {
                                                            Text(
                                                                text = "${days}d",
                                                                fontSize = 11.sp,
                                                                fontWeight = FontWeight.Bold,
                                                                color = if (sel) Color.White else TextDark
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        item {
                            // Database matching stats preview
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                                border = BorderStroke(1.dp, CardBorderColor)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Matching Outpass Records Found",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = TextMuted
                                        )
                                        Text(
                                            text = "$previewCount records in ${currentUser.department}",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = TextDark
                                        )
                                    }
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(if (previewCount > 0) GreenContainer else Color(0xFFF1F5F9))
                                            .padding(horizontal = 10.dp, vertical = 6.dp)
                                    ) {
                                        Text(
                                            text = if (previewCount > 0) "Ready to Analyze" else "No Records",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = if (previewCount > 0) GreenDark else TextMuted
                                        )
                                    }
                                }
                            }
                        }

                        if (errorMessage != null) {
                            item {
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(10.dp),
                                    colors = CardDefaults.cardColors(containerColor = CrimsonContainer),
                                    border = BorderStroke(1.dp, CrimsonError)
                                ) {
                                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = CrimsonError)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(text = errorMessage, color = CrimsonError, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Action Buttons for Step 1
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                val file = PdfExporter.generateAndSavePdf(
                                    context = context,
                                    user = currentUser,
                                    records = filteredOutpasses,
                                    isClassReport = currentUser.role == UserRole.STAFF_ADVISOR
                                )
                                if (file != null) {
                                    PdfExporter.openOrSharePdf(context, file, isShare = false)
                                    Toast.makeText(context, "Report downloaded: ${file.name}", Toast.LENGTH_LONG).show()
                                } else {
                                    Toast.makeText(context, "Unable to generate PDF report", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier.weight(1f).fillMaxHeight(),
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.5.dp, GreenPrimary)
                        ) {
                            Icon(Icons.Default.Download, contentDescription = null, tint = GreenPrimary, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Download PDF", color = GreenPrimary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }

                        Button(
                            onClick = {
                                userWantsReanalyze = false
                                val start = if (selectedPreset == ReportDatePreset.CUSTOM) customStartMs else null
                                val end = if (selectedPreset == ReportDatePreset.CUSTOM) customEndMs else null
                                onGenerate(selectedPreset, start, end)
                            },
                            enabled = !isGenerating,
                            modifier = Modifier.weight(1.1f).fillMaxHeight(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = GreenPrimary,
                                contentColor = Color.White,
                                disabledContainerColor = Color(0xFF94A3B8)
                            ),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            if (isGenerating) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    color = Color.White,
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Analyzing...",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            } else {
                                Icon(Icons.Default.Description, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Preview Report",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                } else {
                    // STEP 2: REPORT VIEWER (Display Generated Report)
                    val stats = currentReport.statistics
                    val analysis = currentReport.analysis

                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // Report Meta Header Card
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                                border = BorderStroke(1.dp, CardBorderColor)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = "REPORT PERIOD",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = TextMuted,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = stats.periodLabel,
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = TextDark
                                            )
                                        }

                                        Spacer(modifier = Modifier.width(8.dp))

                                        // Badge without vertical squishing
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(if (analysis.isAiGenerated) GreenContainer else Color(0xFFE2E8F0))
                                                .padding(horizontal = 10.dp, vertical = 5.dp)
                                        ) {
                                            Text(
                                                text = if (analysis.isAiGenerated) "Gemini AI" else "Calculated Audit",
                                                color = if (analysis.isAiGenerated) GreenDark else TextDark,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 11.sp,
                                                maxLines = 1,
                                                softWrap = false
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(10.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = "Generated: ${dateTimeDf.format(Date(currentReport.generatedAt))}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = TextMuted,
                                            fontSize = 11.sp
                                        )
                                        Text(
                                            text = "Dept: ${stats.department}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = GreenDark,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp
                                        )
                                    }
                                }
                            }
                        }

                        // Primary Calculated Metric Cards Grid
                        item {
                            Text(
                                text = "CALCULATED DATABASE STATISTICS",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = TextMuted,
                                fontSize = 11.sp
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                MetricPill(title = "Total", value = "${stats.totalRequests}", color = GreenPrimary, modifier = Modifier.weight(1f))
                                MetricPill(title = "Approved", value = "${stats.approvedCount}", color = EmeraldSuccess, modifier = Modifier.weight(1f))
                                MetricPill(title = "Rejected", value = "${stats.rejectedCount}", color = CrimsonError, modifier = Modifier.weight(1f))
                                MetricPill(title = "Pending", value = "${stats.pendingCount}", color = AmberWarning, modifier = Modifier.weight(1f))
                                MetricPill(title = "Late", value = "${stats.lateReturnsCount}", color = if (stats.lateReturnsCount > 0) CrimsonError else EmeraldSuccess, modifier = Modifier.weight(1f))
                            }
                        }

                        // Operational Details Card
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                                border = BorderStroke(1.dp, CardBorderColor)
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.AccessTime, contentDescription = null, tint = GreenDark, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("Peak Departure Window:", style = MaterialTheme.typography.bodySmall, color = TextMuted, fontSize = 11.sp)
                                        }
                                        Text(stats.peakExitHours, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = TextDark, fontSize = 11.sp)
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.HourglassTop, contentDescription = null, tint = EmeraldSuccess, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("Avg Approval Duration:", style = MaterialTheme.typography.bodySmall, color = TextMuted, fontSize = 11.sp)
                                        }
                                        Text(
                                            text = if (stats.averageApprovalTimeMinutes != null) "${stats.averageApprovalTimeMinutes} mins" else "N/A",
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.Bold,
                                            color = TextDark,
                                            fontSize = 11.sp
                                        )
                                    }

                                    if (stats.topReasons.isNotEmpty()) {
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(Icons.AutoMirrored.Filled.DirectionsWalk, contentDescription = null, tint = AmberWarning, modifier = Modifier.size(16.dp))
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text("Primary Reason:", style = MaterialTheme.typography.bodySmall, color = TextMuted, fontSize = 11.sp)
                                            }
                                            Text(
                                                text = "${stats.topReasons.first().first} (${stats.topReasons.first().second})",
                                                style = MaterialTheme.typography.bodySmall,
                                                fontWeight = FontWeight.Bold,
                                                color = TextDark,
                                                fontSize = 11.sp
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // AI SUMMARY
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = WhiteCard),
                                border = BorderStroke(1.5.dp, GreenPrimary.copy(alpha = 0.4f))
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Psychology, contentDescription = null, tint = GreenDark, modifier = Modifier.size(20.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "EXECUTIVE SUMMARY",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = GreenDark
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = analysis.executiveSummary,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = TextDark,
                                        lineHeight = 20.sp
                                    )
                                }
                            }
                        }

                        // KEY OBSERVATIONS
                        if (analysis.keyObservations.isNotEmpty()) {
                            item {
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(16.dp),
                                    colors = CardDefaults.cardColors(containerColor = WhiteCard),
                                    border = BorderStroke(1.dp, CardBorderColor)
                                ) {
                                    Column(modifier = Modifier.padding(16.dp)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = EmeraldSuccess, modifier = Modifier.size(18.dp))
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = "KEY OBSERVATIONS",
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = TextDark
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(10.dp))
                                        analysis.keyObservations.forEach { obs ->
                                            Row(modifier = Modifier.padding(vertical = 4.dp)) {
                                                Text("• ", color = EmeraldSuccess, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                                Text(obs, style = MaterialTheme.typography.bodySmall, color = TextDark, lineHeight = 18.sp)
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // IMPORTANT TRENDS
                        if (analysis.importantTrends.isNotEmpty()) {
                            item {
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(16.dp),
                                    colors = CardDefaults.cardColors(containerColor = WhiteCard),
                                    border = BorderStroke(1.dp, CardBorderColor)
                                ) {
                                    Column(modifier = Modifier.padding(16.dp)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.AutoMirrored.Filled.TrendingUp, contentDescription = null, tint = GreenDark, modifier = Modifier.size(18.dp))
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = "IMPORTANT TRENDS",
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = TextDark
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(10.dp))
                                        analysis.importantTrends.forEach { trend ->
                                            Row(modifier = Modifier.padding(vertical = 4.dp)) {
                                                Text("• ", color = GreenDark, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                                Text(trend, style = MaterialTheme.typography.bodySmall, color = TextDark, lineHeight = 18.sp)
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // STUDENT OUTPASS STATEMENT & DETAILS
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = WhiteCard),
                                border = BorderStroke(1.dp, CardBorderColor)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Default.ListAlt,
                                                contentDescription = null,
                                                tint = GreenDark,
                                                modifier = Modifier.size(20.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = "STUDENT OUTPASS DETAILS",
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = TextDark
                                            )
                                        }
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(GreenContainer)
                                                .padding(horizontal = 8.dp, vertical = 4.dp)
                                        ) {
                                            Text(
                                                text = "${filteredOutpasses.size} Records",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = GreenDark
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(12.dp))

                                    if (filteredOutpasses.isEmpty()) {
                                        Text(
                                            text = "No student outpasses found for the selected department and period.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = TextMuted,
                                            modifier = Modifier.padding(vertical = 12.dp)
                                        )
                                    } else {
                                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                            filteredOutpasses.forEachIndexed { index, pass ->
                                                Card(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    shape = RoundedCornerShape(12.dp),
                                                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                                                    border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                                                ) {
                                                    Column(modifier = Modifier.padding(12.dp)) {
                                                        // Row 1: Student Name, Reg No & Pass ID
                                                        Row(
                                                            modifier = Modifier.fillMaxWidth(),
                                                            horizontalArrangement = Arrangement.SpaceBetween,
                                                            verticalAlignment = Alignment.CenterVertically
                                                        ) {
                                                            Row(
                                                                verticalAlignment = Alignment.CenterVertically,
                                                                modifier = Modifier.weight(1f)
                                                            ) {
                                                                Box(
                                                                    modifier = Modifier
                                                                        .size(24.dp)
                                                                        .clip(CircleShape)
                                                                        .background(GreenContainer),
                                                                    contentAlignment = Alignment.Center
                                                                ) {
                                                                    Text(
                                                                        text = "${index + 1}",
                                                                        fontSize = 11.sp,
                                                                        fontWeight = FontWeight.Bold,
                                                                        color = GreenDark
                                                                    )
                                                                }
                                                                Spacer(modifier = Modifier.width(8.dp))
                                                                Column {
                                                                    Text(
                                                                        text = pass.studentName,
                                                                        style = MaterialTheme.typography.titleSmall,
                                                                        fontWeight = FontWeight.Bold,
                                                                        color = TextDark
                                                                    )
                                                                    Text(
                                                                        text = "Reg No: ${pass.regNo}",
                                                                        style = MaterialTheme.typography.labelSmall,
                                                                        fontWeight = FontWeight.SemiBold,
                                                                        color = GreenDark,
                                                                        fontSize = 11.sp
                                                                    )
                                                                }
                                                            }

                                                            Box(
                                                                modifier = Modifier
                                                                    .clip(RoundedCornerShape(6.dp))
                                                                    .background(Color(0xFFE2E8F0))
                                                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                                                            ) {
                                                                Text(
                                                                    text = pass.id,
                                                                    fontSize = 10.sp,
                                                                    fontWeight = FontWeight.Bold,
                                                                    color = Color(0xFF475569)
                                                                )
                                                            }
                                                        }

                                                        Spacer(modifier = Modifier.height(8.dp))

                                                        // Row 2: Outpass Timing (Departure & Return in clean alignment)
                                                        Row(
                                                            modifier = Modifier
                                                                .fillMaxWidth()
                                                                .clip(RoundedCornerShape(8.dp))
                                                                .background(Color.White)
                                                                .border(1.dp, Color(0xFFF1F5F9), RoundedCornerShape(8.dp))
                                                                .padding(horizontal = 8.dp, vertical = 6.dp),
                                                            horizontalArrangement = Arrangement.SpaceBetween,
                                                            verticalAlignment = Alignment.CenterVertically
                                                        ) {
                                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                                Icon(
                                                                    Icons.Default.Schedule,
                                                                    contentDescription = null,
                                                                    tint = GreenPrimary,
                                                                    modifier = Modifier.size(14.dp)
                                                                )
                                                                Spacer(modifier = Modifier.width(4.dp))
                                                                Text(
                                                                    text = "Timing:",
                                                                    fontSize = 11.sp,
                                                                    fontWeight = FontWeight.Bold,
                                                                    color = TextMuted
                                                                )
                                                            }
                                                            Text(
                                                                text = "${timeDf.format(Date(pass.outDateTime))}  →  ${timeDf.format(Date(pass.returnDateTime))}",
                                                                fontSize = 11.sp,
                                                                fontWeight = FontWeight.Bold,
                                                                color = TextDark
                                                            )
                                                        }

                                                        Spacer(modifier = Modifier.height(6.dp))

                                                        // Row 3: Reason with destination
                                                        Row(
                                                            modifier = Modifier.fillMaxWidth(),
                                                            verticalAlignment = Alignment.CenterVertically
                                                        ) {
                                                            Text(
                                                                text = "Reason: ",
                                                                fontSize = 11.sp,
                                                                fontWeight = FontWeight.SemiBold,
                                                                color = TextMuted
                                                            )
                                                            Text(
                                                                text = pass.reason.ifBlank { "Campus Permission" },
                                                                fontSize = 11.sp,
                                                                fontWeight = FontWeight.Medium,
                                                                color = TextDark
                                                            )
                                                            if (pass.destination.isNotBlank()) {
                                                                Text(
                                                                    text = " (${pass.destination})",
                                                                    fontSize = 11.sp,
                                                                    color = TextMuted
                                                                )
                                                            }
                                                        }

                                                        Spacer(modifier = Modifier.height(6.dp))

                                                        // Row 4: Leave Type & Status
                                                        Row(
                                                            modifier = Modifier.fillMaxWidth(),
                                                            horizontalArrangement = Arrangement.SpaceBetween,
                                                            verticalAlignment = Alignment.CenterVertically
                                                        ) {
                                                            Text(
                                                                text = pass.type.displayName,
                                                                fontSize = 11.sp,
                                                                fontWeight = FontWeight.SemiBold,
                                                                color = Color(0xFF64748B)
                                                            )
                                                            StatusBadge(status = pass.status)
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Action Buttons (Download PDF, Share PDF, Change Filter)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                userWantsReanalyze = true
                            },
                            modifier = Modifier.weight(0.9f),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, CardBorderColor)
                        ) {
                            Icon(Icons.Default.CalendarMonth, contentDescription = null, tint = TextDark, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Filter", fontSize = 11.sp, color = TextDark, fontWeight = FontWeight.SemiBold)
                        }

                        Button(
                            onClick = {
                                val file = PdfExporter.generateAndSavePdf(
                                    context = context,
                                    user = currentUser,
                                    records = filteredOutpasses,
                                    isClassReport = currentUser.role == UserRole.STAFF_ADVISOR
                                )
                                if (file != null) {
                                    PdfExporter.openOrSharePdf(context, file, isShare = false)
                                    Toast.makeText(context, "Report downloaded to: ${file.name}", Toast.LENGTH_LONG).show()
                                } else {
                                    Toast.makeText(context, "Could not generate PDF report", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier.weight(1.3f),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary)
                        ) {
                            Icon(Icons.Default.Download, contentDescription = null, tint = Color.White, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Download PDF", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }

                        OutlinedButton(
                            onClick = {
                                val file = PdfExporter.generateAndSavePdf(
                                    context = context,
                                    user = currentUser,
                                    records = filteredOutpasses,
                                    isClassReport = currentUser.role == UserRole.STAFF_ADVISOR
                                )
                                if (file != null) {
                                    PdfExporter.openOrSharePdf(context, file, isShare = true)
                                } else {
                                    Toast.makeText(context, "Could not export PDF to share", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier.weight(1.1f),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, GreenPrimary)
                        ) {
                            Icon(Icons.Default.PictureAsPdf, contentDescription = null, tint = GreenPrimary, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Share PDF", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = GreenPrimary)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MetricPill(
    title: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
        border = BorderStroke(1.dp, color.copy(alpha = 0.35f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp, horizontal = 2.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = color
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                color = TextDark,
                fontSize = 9.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1
            )
        }
    }
}
