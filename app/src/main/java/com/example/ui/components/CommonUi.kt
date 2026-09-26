package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.models.OutpassStatus
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Shape
import com.example.ui.theme.GreenDark
import com.example.ui.theme.GreenHover
import com.example.ui.theme.GreenPrimary
import com.example.data.models.User
import com.example.data.models.UserRole
import com.example.ui.theme.AmberContainer
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.CrimsonContainer
import com.example.ui.theme.CrimsonError
import com.example.ui.theme.EmeraldContainer
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.IndigoContainer
import com.example.ui.theme.IndigoLight
import com.example.ui.theme.IndigoPrimary
import com.example.ui.theme.Slate700
import com.example.ui.theme.Slate800
import com.example.ui.theme.Slate900

import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.Dp

@Composable
fun PremiumAppLogo(
    modifier: Modifier = Modifier,
    size: Dp = 88.dp
) {
    val goldBrush = Brush.sweepGradient(
        colors = listOf(
            Color(0xFFF59E0B),
            Color(0xFFFCD34D),
            Color(0xFFB45309),
            Color(0xFFF59E0B)
        )
    )

    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(goldBrush)
            .padding(2.5.dp) // Outer Metallic Gold Rim
            .clip(CircleShape)
            .background(Slate900)
            .padding(3.dp) // Inner Slate Gap
            .clip(CircleShape)
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        IndigoPrimary,
                        Color(0xFF1E1B4B)
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        // Inner Glassmorphism Ring Accent
        Box(
            modifier = Modifier
                .size(size * 0.72f)
                .clip(CircleShape)
                .border(1.dp, Color(0xFFFCD34D).copy(alpha = 0.5f), CircleShape)
                .background(Color.White.copy(alpha = 0.08f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Shield,
                contentDescription = "Premium Campus Shield Logo",
                tint = Color(0xFFFCD34D), // Metallic Gold Shield
                modifier = Modifier.size(size * 0.42f)
            )
            Icon(
                imageVector = Icons.Default.VerifiedUser,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(size * 0.26f)
            )
        }
    }
}

@Composable
fun StatusBadge(status: OutpassStatus) {
    val (bgColor, textColor, label) = when (status) {
        OutpassStatus.PENDING_STAFF -> Triple(AmberContainer, AmberWarning, "Pending Staff")
        OutpassStatus.PENDING_HOD -> Triple(AmberContainer, AmberWarning, "Pending HOD")
        OutpassStatus.APPROVED -> Triple(EmeraldContainer, EmeraldSuccess, "Approved")
        OutpassStatus.CHECKED_OUT -> Triple(IndigoContainer, IndigoPrimary, "Outside Campus")
        OutpassStatus.CHECKED_IN -> Triple(Slate700, Color.White, "Returned")
        OutpassStatus.REJECTED -> Triple(CrimsonContainer, CrimsonError, "Rejected")
        OutpassStatus.EXPIRED -> Triple(Slate700, Color.Gray, "Expired")
    }

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(bgColor)
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = textColor,
            fontSize = 11.sp
        )
    }
}

@Composable
fun RoleBadge(role: UserRole) {
    val (bgColor, textColor) = when (role) {
        UserRole.STUDENT -> Pair(IndigoContainer, IndigoPrimary)
        UserRole.STAFF_ADVISOR -> Pair(AmberContainer, AmberWarning)
        UserRole.HOD -> Pair(EmeraldContainer, EmeraldSuccess)
        UserRole.SECURITY_OFFICER -> Pair(CrimsonContainer, CrimsonError)
        UserRole.ADMIN -> Pair(Slate700, Color.White)
    }

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(bgColor)
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(
            text = role.displayName,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = textColor,
            fontSize = 10.sp
        )
    }
}

@Composable
fun TopUserHeaderBar(
    currentUser: User?,
    onLogoutClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(18.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(IndigoPrimary.copy(alpha = 0.2f))
                    .border(1.dp, IndigoLight.copy(alpha = 0.4f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                if (currentUser?.photoUri != null) {
                    AsyncImage(
                        model = currentUser.photoUri,
                        contentDescription = "User Avatar",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(30.dp)
                            .clip(CircleShape)
                            .background(IndigoPrimary),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AccountCircle,
                            contentDescription = "User Avatar",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = currentUser?.name ?: "Guest User",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    currentUser?.role?.let { RoleBadge(it) }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = if (currentUser?.role == UserRole.STUDENT)
                        "Reg: ${currentUser.regNo} • ${currentUser.department}"
                    else
                        "${currentUser?.department ?: "Campus System"}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 11.sp
                )
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(CrimsonContainer)
                    .border(1.dp, CrimsonError.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                    .clickable { onLogoutClick() }
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Logout,
                        contentDescription = "Logout",
                        tint = CrimsonError,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Logout",
                        style = MaterialTheme.typography.labelSmall,
                        color = CrimsonError,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

/**
 * Modern Green Button with dynamic transition animations:
 * Smooth scale bounce on press, animated background transitions, and elevated drop shadow.
 */
@Composable
fun GreenAnimatedButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = RoundedCornerShape(12.dp),
    contentPadding: PaddingValues = ButtonDefaults.ContentPadding,
    content: @Composable RowScope.() -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1.0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow),
        label = "greenBtnScale"
    )

    val animatedBgColor by animateColorAsState(
        targetValue = if (!enabled) Color(0xFFCBD5E1) else if (isPressed) GreenHover else GreenPrimary,
        animationSpec = tween(durationMillis = 150),
        label = "greenBtnBg"
    )

    Button(
        onClick = onClick,
        modifier = modifier.scale(scale),
        enabled = enabled,
        shape = shape,
        colors = ButtonDefaults.buttonColors(
            containerColor = animatedBgColor,
            contentColor = Color.White
        ),
        interactionSource = interactionSource,
        elevation = ButtonDefaults.buttonElevation(
            defaultElevation = 2.dp,
            pressedElevation = 6.dp
        ),
        contentPadding = contentPadding,
        content = content
    )
}

/**
 * Status indicator displaying automated 4:10 PM HOD report dispatch readiness.
 */
@Composable
fun HodAutomatedReportBadge(
    modifier: Modifier = Modifier,
    lastDispatchedInfo: Pair<String?, String?>? = null
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF0FDF4)),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFA7F3D0))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFDCFCE7)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.AccessTime,
                    contentDescription = null,
                    tint = Color(0xFF047857),
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Automated 4:10 PM HOD Dispatch Active",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF065F46)
                )
                Text(
                    text = if (lastDispatchedInfo?.first != null)
                        "Today's report delivered at: ${lastDispatchedInfo.first}"
                    else
                        "Daily department outpasses auto-send to HOD Gmail at exactly 4:10 PM",
                    fontSize = 11.sp,
                    color = Color(0xFF047857)
                )
            }
        }
    }
}
