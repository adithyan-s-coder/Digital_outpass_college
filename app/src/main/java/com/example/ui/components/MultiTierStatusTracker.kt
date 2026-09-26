package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.models.Outpass
import com.example.data.models.OutpassStatus
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.CrimsonError
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.IndigoPrimary
import com.example.ui.theme.Slate700
import com.example.ui.theme.Slate800
import com.example.ui.theme.TextDark
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun MultiTierStatusTracker(
    outpass: Outpass,
    modifier: Modifier = Modifier
) {
    val isRejected = outpass.status == OutpassStatus.REJECTED

    // Step 1: Staff Advisor
    val step1State = when {
        outpass.staffApproval?.status == "APPROVED" -> StepState.COMPLETED
        outpass.staffApproval?.status == "REJECTED" -> StepState.REJECTED
        outpass.status == OutpassStatus.PENDING_STAFF -> StepState.ACTIVE
        else -> StepState.COMPLETED
    }

    // Step 2: HOD Sign-off
    val step2State = when {
        outpass.hodApproval?.status == "APPROVED" -> StepState.COMPLETED
        outpass.hodApproval?.status == "REJECTED" -> StepState.REJECTED
        outpass.status == OutpassStatus.PENDING_HOD -> StepState.ACTIVE
        step1State == StepState.COMPLETED && (outpass.status == OutpassStatus.APPROVED || outpass.status == OutpassStatus.CHECKED_OUT || outpass.status == OutpassStatus.CHECKED_IN) -> StepState.COMPLETED
        else -> StepState.LOCKED
    }

    // Step 3: Gate Check-out
    val step3State = when {
        outpass.actualCheckOutTime != null -> StepState.COMPLETED
        outpass.status == OutpassStatus.APPROVED -> StepState.ACTIVE
        outpass.status == OutpassStatus.CHECKED_OUT -> StepState.COMPLETED
        outpass.status == OutpassStatus.CHECKED_IN -> StepState.COMPLETED
        else -> StepState.LOCKED
    }

    // Step 4: Gate Check-in
    val step4State = when {
        outpass.actualCheckInTime != null -> StepState.COMPLETED
        outpass.status == OutpassStatus.CHECKED_OUT -> StepState.ACTIVE
        outpass.status == OutpassStatus.CHECKED_IN -> StepState.COMPLETED
        else -> StepState.LOCKED
    }

    val df = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault())

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Slate800)
            .border(1.dp, Slate700, RoundedCornerShape(20.dp))
            .padding(18.dp)
    ) {
        Text(
            text = "Approval & Gate Process Tracker",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = TextDark
        )
        Text(
            text = "Multi-tier security verification pipeline",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Step 1 Item
        TrackerStepRow(
            title = "1. Staff Advisor Approval",
            subtitle = when (step1State) {
                StepState.COMPLETED -> "Approved by ${outpass.staffApproval?.approverName ?: "Advisor"} • ${outpass.staffApproval?.timestamp?.let { df.format(Date(it)) } ?: ""}"
                StepState.REJECTED -> "Rejected: ${outpass.rejectionReason ?: "Reason not provided"}"
                StepState.ACTIVE -> "Awaiting Staff Advisor review..."
                StepState.LOCKED -> "Pending prior step"
            },
            state = step1State,
            isLast = false
        )

        // Step 2 Item
        TrackerStepRow(
            title = "2. Head of Department (HOD) Sign-off",
            subtitle = when (step2State) {
                StepState.COMPLETED -> "Approved by ${outpass.hodApproval?.approverName ?: "HOD"} • ${outpass.hodApproval?.timestamp?.let { df.format(Date(it)) } ?: ""}"
                StepState.REJECTED -> "Rejected by HOD"
                StepState.ACTIVE -> "Awaiting HOD approval sign-off..."
                StepState.LOCKED -> "Locked until Staff Advisor approves"
            },
            state = step2State,
            isLast = false
        )

        // Step 3 Item
        TrackerStepRow(
            title = "3. Gate Security Check-out",
            subtitle = when (step3State) {
                StepState.COMPLETED -> "Campus Exit Logged at ${outpass.actualCheckOutTime?.let { df.format(Date(it)) } ?: "Main Gate"}"
                StepState.ACTIVE -> "Pass Approved! Scan QR at security gate to check-out"
                StepState.REJECTED -> "N/A"
                StepState.LOCKED -> "Awaiting full approval"
            },
            state = step3State,
            isLast = false
        )

        // Step 4 Item
        TrackerStepRow(
            title = "4. Gate Security Check-in (Return)",
            subtitle = when (step4State) {
                StepState.COMPLETED -> "Campus Return Logged at ${outpass.actualCheckInTime?.let { df.format(Date(it)) } ?: "Main Gate"}"
                StepState.ACTIVE -> "Currently Outside Campus • Scan QR upon return"
                StepState.REJECTED -> "N/A"
                StepState.LOCKED -> "Awaiting gate exit"
            },
            state = step4State,
            isLast = true
        )
    }
}

enum class StepState {
    COMPLETED,
    ACTIVE,
    REJECTED,
    LOCKED
}

@Composable
private fun TrackerStepRow(
    title: String,
    subtitle: String,
    state: StepState,
    isLast: Boolean
) {
    val circleColor by animateColorAsState(
        targetValue = when (state) {
            StepState.COMPLETED -> EmeraldSuccess
            StepState.ACTIVE -> AmberWarning
            StepState.REJECTED -> CrimsonError
            StepState.LOCKED -> Slate700
        },
        label = "circle_color"
    )

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(circleColor),
                contentAlignment = Alignment.Center
            ) {
                when (state) {
                    StepState.COMPLETED -> Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Done",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                    StepState.REJECTED -> Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Rejected",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                    StepState.ACTIVE -> Icon(
                        imageVector = Icons.Default.HourglassEmpty,
                        contentDescription = "Active",
                        tint = Color.Black,
                        modifier = Modifier.size(16.dp)
                    )
                    StepState.LOCKED -> Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Locked",
                        tint = Color.Gray,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }

            if (!isLast) {
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .height(36.dp)
                        .background(if (state == StepState.COMPLETED) EmeraldSuccess else Slate700)
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (state == StepState.ACTIVE || state == StepState.COMPLETED) FontWeight.Bold else FontWeight.Normal,
                color = if (state == StepState.LOCKED) MaterialTheme.colorScheme.onSurfaceVariant else TextDark
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = when (state) {
                    StepState.COMPLETED -> EmeraldSuccess
                    StepState.ACTIVE -> AmberWarning
                    StepState.REJECTED -> CrimsonError
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                },
                fontSize = 11.sp
            )
        }
    }
}
