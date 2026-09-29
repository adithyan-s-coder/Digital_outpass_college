package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import com.example.ui.components.UserAvatar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MeetingRoom
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.platform.LocalContext
import com.example.data.models.CompleteAiReport
import com.example.data.models.Outpass
import com.example.data.models.ReportDatePreset
import com.example.data.models.User
import com.example.data.models.UserRole
import com.example.ui.components.AiReportButton
import com.example.ui.components.AiReportModal
import com.example.ui.components.StatusBadge
import com.example.ui.theme.CrimsonError
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.IndigoPrimary
import com.example.ui.theme.Slate700
import com.example.ui.theme.Slate800
import com.example.ui.theme.Slate900
import com.example.ui.theme.TextDark
import com.example.ui.theme.GreenDark
import com.example.ui.theme.GreenContainer
import com.example.ui.components.GreenAnimatedButton
import com.example.ui.components.HodAutomatedReportBadge
import com.example.util.DailyHodReportScheduler
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ApprovalsScreen(
    currentUser: User?,
    pendingPasses: List<Outpass>,
    allOutpasses: List<Outpass> = emptyList(),
    onApprovePass: (String, String) -> Unit,
    onRejectPass: (String, String) -> Unit,
    currentAiReport: CompleteAiReport? = null,
    isGeneratingAiReport: Boolean = false,
    aiReportError: String? = null,
    onGenerateAiReport: ((ReportDatePreset, Long?, Long?) -> Unit)? = null,
    onDismissAiReport: (() -> Unit)? = null
) {
    val context = LocalContext.current
    var selectedPassForDialog by remember { mutableStateOf<Outpass?>(null) }
    var isApproveAction by remember { mutableStateOf(true) }
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
                    text = "Pending Outpass Approvals",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = TextDark
                )

                Text(
                    text = "Approvals Queue for ${currentUser?.department ?: "Department"}",
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

        // 4:10 PM Automated HOD Daily Report Status Banner (Shown ONLY in Staff Advisor module per requirements)
        if (currentUser != null && currentUser.role == UserRole.STAFF_ADVISOR) {
            val lastDispatchedInfo = DailyHodReportScheduler.getLastDispatchedInfo(context)
            HodAutomatedReportBadge(lastDispatchedInfo = lastDispatchedInfo)
            Spacer(modifier = Modifier.height(14.dp))
        } else {
            Spacer(modifier = Modifier.height(4.dp))
        }

        if (pendingPasses.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Slate800)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = EmeraldSuccess,
                        modifier = Modifier.size(56.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "All Caught Up!",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextDark
                    )
                    Text(
                        text = "There are no pending outpass applications requiring your signature right now.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp
                    )
                }
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(pendingPasses) { pass ->
                    PendingApprovalCard(
                        outpass = pass,
                        df = df,
                        onApproveClick = {
                            selectedPassForDialog = pass
                            isApproveAction = true
                        },
                        onRejectClick = {
                            selectedPassForDialog = pass
                            isApproveAction = false
                        }
                    )
                }
            }
        }
    }

    // Approval / Rejection Modal Dialog
    if (selectedPassForDialog != null) {
        ApprovalDecisionDialog(
            outpass = selectedPassForDialog!!,
            isApprove = isApproveAction,
            onConfirm = { remarks ->
                if (isApproveAction) {
                    onApprovePass(selectedPassForDialog!!.id, remarks)
                } else {
                    onRejectPass(selectedPassForDialog!!.id, remarks)
                }
                selectedPassForDialog = null
            },
            onDismiss = { selectedPassForDialog = null }
        )
    }

    // AI Outpass Report Modal
    if ((showReportDialog || currentAiReport != null) && currentUser != null) {
        AiReportModal(
            currentUser = currentUser,
            allOutpasses = if (allOutpasses.isNotEmpty()) allOutpasses else pendingPasses,
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
private fun PendingApprovalCard(
    outpass: Outpass,
    df: SimpleDateFormat,
    onApproveClick: () -> Unit,
    onRejectClick: () -> Unit
) {
    val context = LocalContext.current

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Slate700),
        colors = CardDefaults.cardColors(containerColor = Slate800)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
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
                        size = 46.dp
                    )

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text(
                            text = outpass.studentName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextDark
                        )
                        Text(
                            text = "Reg: ${outpass.regNo} • ${outpass.hostelBlock} #${outpass.roomNo}",
                            style = MaterialTheme.typography.bodySmall,
                            color = GreenDark,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))
                StatusBadge(status = outpass.status)
            }

            Spacer(modifier = Modifier.height(10.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(12.dp)
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.LocationOn, contentDescription = null, tint = EmeraldSuccess, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = "Destination: ", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(text = outpass.destination, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = TextDark)
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "Reason: ${outpass.reason}",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextDark
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Out: ${df.format(Date(outpass.outDateTime))}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 11.sp
                        )

                        Text(
                            text = "Return: ${df.format(Date(outpass.returnDateTime))}",
                            style = MaterialTheme.typography.labelSmall,
                            color = EmeraldSuccess,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Mandatory Parent Call Verification Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFFFEF3C7))
                    .border(1.dp, Color(0xFFFDE68A), RoundedCornerShape(12.dp))
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Phone,
                            contentDescription = "Parent Contact",
                            tint = Color(0xFFD97706),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Parent: ${outpass.parentPhone}",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF92400E)
                        )
                    }
                    Text(
                        text = "Mandatory parent call verification before staff approval",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFFB45309),
                        fontSize = 10.sp
                    )
                }

                Button(
                    onClick = {
                        val cleanPhone = outpass.parentPhone.replace("[^0-9+]".toRegex(), "")
                        val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$cleanPhone"))
                        context.startActivity(intent)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD97706)),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(imageVector = Icons.Default.Call, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color.White)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Call Parent", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onRejectClick,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = CrimsonError),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Reject")
                }

                GreenAnimatedButton(
                    onClick = onApproveClick,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Approve", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun ApprovalDecisionDialog(
    outpass: Outpass,
    isApprove: Boolean,
    onConfirm: (remarks: String) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var remarks by remember { mutableStateOf(if (isApprove) "Approved for leave." else "Reason for rejection.") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(20.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, Slate700),
            colors = CardDefaults.cardColors(containerColor = Slate800)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Text(
                    text = if (isApprove) "Approve Outpass Request" else "Reject Outpass Request",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (isApprove) EmeraldSuccess else CrimsonError
                )

                Text(
                    text = "Student: ${outpass.studentName} (${outpass.regNo})",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextDark
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Direct Call Option inside Decision Modal (Calls parent directly via phone dialer)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Phone, contentDescription = null, tint = Color(0xFFB45309), modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Parent: ${outpass.parentPhone}", fontSize = 12.sp, color = TextDark)
                    }
                    TextButton(
                        onClick = {
                            val cleanPhone = outpass.parentPhone.replace("[^0-9+]".toRegex(), "")
                            val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$cleanPhone"))
                            context.startActivity(intent)
                        }
                    ) {
                        Icon(Icons.Default.Call, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color(0xFFB45309))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Call Parent", color = Color(0xFFB45309), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = remarks,
                    onValueChange = { remarks = it },
                    label = { Text(if (isApprove) "Approval Remarks" else "Rejection Reason") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = IndigoPrimary,
                        unfocusedBorderColor = Slate700,
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White,
                        focusedTextColor = TextDark,
                        unfocusedTextColor = TextDark
                    ),
                    maxLines = 3
                )

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel", color = Color.Gray)
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    if (isApprove) {
                        GreenAnimatedButton(
                            onClick = { onConfirm(remarks) },
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Confirm Approval", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    } else {
                        Button(
                            onClick = { onConfirm(remarks) },
                            colors = ButtonDefaults.buttonColors(containerColor = CrimsonError),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Confirm Rejection", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
