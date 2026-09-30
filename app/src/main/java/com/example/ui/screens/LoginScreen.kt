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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockReset
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SupervisorAccount
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.models.User
import com.example.data.models.UserRole
import com.example.ui.components.GreenAnimatedButton
import com.example.ui.components.PremiumAppLogo
import com.example.ui.theme.CardBorderColor
import com.example.ui.theme.CrimsonError
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.GreenContainer
import com.example.ui.theme.GreenDark
import com.example.ui.theme.GreenPrimary
import com.example.ui.theme.IndigoPrimary
import com.example.ui.theme.Slate700
import com.example.ui.theme.Slate800
import com.example.ui.theme.TextDark
import com.example.ui.theme.TextMuted
import com.example.ui.theme.WhiteCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    onNavigateRegister: () -> Unit,
    onSelectDemoRole: (UserRole) -> Unit = {},
    onLoginEmail: (String, String, UserRole) -> Pair<Boolean, String>,
    onResetPassword: ((identifier: String, newPassword: String, newName: String?) -> Pair<Boolean, String>)? = null,
    existingUsers: List<User> = emptyList()
) {
    var selectedRole by remember { mutableStateOf<UserRole>(UserRole.STUDENT) }
    var roleDropdownExpanded by remember { mutableStateOf(false) }
    var emailInput by remember { mutableStateOf("") }
    var passwordInput by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var successMessage by remember { mutableStateOf<String?>(null) }

    // Forgot Password Dialog State
    var showForgotPasswordDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item {
            Spacer(modifier = Modifier.height(12.dp))

            // Premium Gold-Accented Campus Shield Logo
            PremiumAppLogo(size = 96.dp)

            Spacer(modifier = Modifier.height(18.dp))

            Text(
                text = "Digital Outpass",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            Text(
                text = "Gate Security & Multi-Tier Approval System",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(EmeraldSuccess.copy(alpha = 0.12f))
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(7.dp)
                        .clip(CircleShape)
                        .background(EmeraldSuccess)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Cloud Synced • Multi-Device Active",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = EmeraldSuccess
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Standard Registered Account Login Form
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, CardBorderColor),
                colors = CardDefaults.cardColors(containerColor = WhiteCard)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                ) {
                    // Dropdown for Role Selection
                    ExposedDropdownMenuBox(
                        expanded = roleDropdownExpanded,
                        onExpandedChange = { roleDropdownExpanded = !roleDropdownExpanded }
                    ) {
                        OutlinedTextField(
                            value = selectedRole.displayName,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Selected Role") },
                            leadingIcon = {
                                val icon = when (selectedRole) {
                                    UserRole.STUDENT -> Icons.Default.School
                                    UserRole.STAFF_ADVISOR -> Icons.Default.Badge
                                    UserRole.HOD -> Icons.Default.SupervisorAccount
                                    UserRole.SECURITY_OFFICER -> Icons.Default.Security
                                    UserRole.ADMIN -> Icons.Default.AdminPanelSettings
                                }
                                Icon(imageVector = icon, contentDescription = null, tint = GreenPrimary, modifier = Modifier.size(18.dp))
                            },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = roleDropdownExpanded) },
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth(),
                            singleLine = true,
                            textStyle = TextStyle(color = Color.Black, fontSize = 14.sp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = GreenPrimary,
                                unfocusedBorderColor = CardBorderColor,
                                focusedContainerColor = Color.White,
                                unfocusedContainerColor = Color.White,
                                focusedTextColor = Color.Black,
                                unfocusedTextColor = Color.Black,
                                cursorColor = Color.Black,
                                focusedLabelColor = GreenPrimary,
                                unfocusedLabelColor = TextMuted
                            )
                        )

                        ExposedDropdownMenu(
                            expanded = roleDropdownExpanded,
                            onDismissRequest = { roleDropdownExpanded = false },
                            modifier = Modifier.background(Color.White)
                        ) {
                            UserRole.values().forEach { role ->
                                DropdownMenuItem(
                                    text = {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            val icon = when (role) {
                                                UserRole.STUDENT -> Icons.Default.School
                                                UserRole.STAFF_ADVISOR -> Icons.Default.Badge
                                                UserRole.HOD -> Icons.Default.SupervisorAccount
                                                UserRole.SECURITY_OFFICER -> Icons.Default.Security
                                                UserRole.ADMIN -> Icons.Default.AdminPanelSettings
                                            }
                                            Icon(imageVector = icon, contentDescription = null, tint = GreenPrimary, modifier = Modifier.size(18.dp))
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = role.displayName,
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = TextDark
                                            )
                                        }
                                    },
                                    onClick = {
                                        selectedRole = role
                                        roleDropdownExpanded = false
                                        emailInput = ""
                                        passwordInput = ""
                                        errorMessage = null
                                        successMessage = null
                                    }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    val isStudent = selectedRole == UserRole.STUDENT
                    val identifierLabel = if (isStudent) "Email / Roll No" else "Email Address"
                    val identifierPlaceholder = if (isStudent) "Enter your Email / Roll No" else "Enter your Email Address"
                    val identifierIcon = if (isStudent) Icons.Default.Badge else Icons.Default.Email

                    OutlinedTextField(
                        value = emailInput,
                        onValueChange = {
                            emailInput = it
                            errorMessage = null
                        },
                        label = { Text(identifierLabel) },
                        placeholder = { Text(identifierPlaceholder) },
                        leadingIcon = {
                            Icon(imageVector = identifierIcon, contentDescription = null, tint = GreenPrimary)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        textStyle = TextStyle(color = Color.Black, fontSize = 15.sp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = GreenPrimary,
                            unfocusedBorderColor = CardBorderColor,
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White,
                            focusedTextColor = Color.Black,
                            unfocusedTextColor = Color.Black,
                            cursorColor = Color.Black,
                            focusedLabelColor = GreenPrimary,
                            unfocusedLabelColor = TextMuted,
                            focusedPlaceholderColor = TextMuted,
                            unfocusedPlaceholderColor = TextMuted
                        ),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = passwordInput,
                        onValueChange = { 
                            passwordInput = it 
                            errorMessage = null
                        },
                        label = { Text("Password") },
                        placeholder = { Text("Enter your Password") },
                        leadingIcon = {
                            Icon(imageVector = Icons.Default.Lock, contentDescription = null, tint = GreenPrimary)
                        },
                        trailingIcon = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(
                                    imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                    contentDescription = if (passwordVisible) "Hide password" else "Show password",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        },
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        modifier = Modifier.fillMaxWidth(),
                        textStyle = TextStyle(color = Color.Black, fontSize = 15.sp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = GreenPrimary,
                            unfocusedBorderColor = CardBorderColor,
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White,
                            focusedTextColor = Color.Black,
                            unfocusedTextColor = Color.Black,
                            cursorColor = Color.Black,
                            focusedLabelColor = GreenPrimary,
                            unfocusedLabelColor = TextMuted,
                            focusedPlaceholderColor = TextMuted,
                            unfocusedPlaceholderColor = TextMuted
                        ),
                        singleLine = true
                    )

                    // Forgot Password Link
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(
                            onClick = {
                                showForgotPasswordDialog = true
                            }
                        ) {
                            Text(
                                text = "Forgot Password?",
                                color = GreenDark,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    if (errorMessage != null) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = errorMessage!!,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }

                    if (successMessage != null) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = successMessage!!,
                            color = EmeraldSuccess,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    GreenAnimatedButton(
                        onClick = {
                            if (emailInput.isBlank()) {
                                errorMessage = if (isStudent) {
                                    "Please enter your Email or Roll No."
                                } else {
                                    "Please enter your Email Address."
                                }
                            } else if (passwordInput.isBlank()) {
                                errorMessage = "Please enter your password."
                            } else {
                                val (success, message) = onLoginEmail(emailInput.trim(), passwordInput.trim(), selectedRole)
                                if (success) {
                                    onLoginSuccess()
                                } else {
                                    errorMessage = message
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Sign In", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Registration Link
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Don't have an account?",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium
                )
                TextButton(onClick = onNavigateRegister) {
                    Icon(imageVector = Icons.Default.PersonAdd, contentDescription = null, tint = GreenPrimary, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Register Here", color = GreenPrimary, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    // FORGOT PASSWORD DIALOG
    if (showForgotPasswordDialog) {
        ForgotPasswordDialog(
            initialIdentifier = emailInput,
            existingUsers = existingUsers,
            onDismiss = { showForgotPasswordDialog = false },
            onPasswordReset = { identifier, newPassword, newName ->
                if (onResetPassword != null) {
                    val result = onResetPassword(identifier, newPassword, newName)
                    if (result.first) {
                        emailInput = identifier
                        passwordInput = newPassword
                        successMessage = "Password reset successfully! You can now sign in."
                        errorMessage = null
                        showForgotPasswordDialog = false
                    }
                    result
                } else {
                    Pair(false, "Password reset handler is not configured.")
                }
            }
        )
    }
}

@Composable
fun ForgotPasswordDialog(
    initialIdentifier: String,
    existingUsers: List<User>,
    onDismiss: () -> Unit,
    onPasswordReset: (identifier: String, newPassword: String, newName: String?) -> Pair<Boolean, String>
) {
    var identifier by remember { mutableStateOf(initialIdentifier) }
    var createPassword by remember { mutableStateOf("") }
    var createPasswordVisible by remember { mutableStateOf(false) }
    var confirmPassword by remember { mutableStateOf("") }
    var confirmPasswordVisible by remember { mutableStateOf(false) }
    var updatedName by remember { mutableStateOf("") }
    var dialogError by remember { mutableStateOf<String?>(null) }

    // Auto-detect matching user from existing users
    val matchedUser = remember(identifier, existingUsers) {
        val clean = identifier.trim()
        if (clean.isBlank()) null
        else existingUsers.firstOrNull {
            it.email.equals(clean, ignoreCase = true) ||
            it.regNo.equals(clean, ignoreCase = true) ||
            it.id.equals(clean, ignoreCase = true)
        }
    }

    // Set initial updatedName when matched user is detected
    LaunchedEffect(matchedUser) {
        if (matchedUser != null && updatedName.isBlank()) {
            updatedName = matchedUser.name
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = WhiteCard),
            border = androidx.compose.foundation.BorderStroke(1.dp, CardBorderColor)
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(GreenContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.LockReset,
                                    contentDescription = null,
                                    tint = GreenDark,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Reset Password",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = TextDark
                                )
                                Text(
                                    text = "Create new password for account",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextMuted,
                                    fontSize = 11.sp
                                )
                            }
                        }

                        IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = TextDark)
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Registered Identifier Field
                    OutlinedTextField(
                        value = identifier,
                        onValueChange = {
                            identifier = it
                            dialogError = null
                        },
                        label = { Text("Registered Email / Roll No") },
                        placeholder = { Text("Enter your Email or Roll No") },
                        leadingIcon = { Icon(Icons.Default.Email, contentDescription = null, tint = GreenPrimary) },
                        modifier = Modifier.fillMaxWidth(),
                        textStyle = TextStyle(color = Color.Black, fontSize = 14.sp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = GreenPrimary,
                            unfocusedBorderColor = CardBorderColor,
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White,
                            focusedTextColor = Color.Black,
                            unfocusedTextColor = Color.Black
                        ),
                        singleLine = true
                    )

                    // If user matched, display account confirmation card
                    if (matchedUser != null) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Card(
                            shape = RoundedCornerShape(10.dp),
                            colors = CardDefaults.cardColors(containerColor = GreenContainer),
                            border = androidx.compose.foundation.BorderStroke(1.dp, GreenPrimary.copy(alpha = 0.5f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = GreenDark, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = "Account: ${matchedUser.name} (${matchedUser.role.displayName})",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = GreenDark
                                    )
                                    Text(
                                        text = "Dept: ${matchedUser.department} • Reg: ${if (matchedUser.regNo.isNotBlank()) matchedUser.regNo else "Faculty"}",
                                        fontSize = 11.sp,
                                        color = TextDark
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Full Name (Allows updating name which reflects in admin module users list)
                    OutlinedTextField(
                        value = updatedName,
                        onValueChange = {
                            updatedName = it
                            dialogError = null
                        },
                        label = { Text("Account Holder Full Name") },
                        placeholder = { Text("Enter full name (updates in admin directory)") },
                        leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = GreenPrimary) },
                        modifier = Modifier.fillMaxWidth(),
                        supportingText = {
                            Text(
                                text = "Name entered here will update in the admin module users directory",
                                fontSize = 10.sp,
                                color = TextMuted
                            )
                        },
                        textStyle = TextStyle(color = Color.Black, fontSize = 14.sp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = GreenPrimary,
                            unfocusedBorderColor = CardBorderColor,
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White,
                            focusedTextColor = Color.Black,
                            unfocusedTextColor = Color.Black
                        ),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Create Password Field
                    OutlinedTextField(
                        value = createPassword,
                        onValueChange = {
                            createPassword = it
                            dialogError = null
                        },
                        label = { Text("Create Password") },
                        placeholder = { Text("Enter new password") },
                        leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = GreenPrimary) },
                        trailingIcon = {
                            IconButton(onClick = { createPasswordVisible = !createPasswordVisible }) {
                                Icon(
                                    imageVector = if (createPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        },
                        visualTransformation = if (createPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        supportingText = {
                            Text(
                                text = "Minimum 8 characters with at least 1 symbol (e.g. @, #, $, !)",
                                fontSize = 10.sp,
                                color = TextMuted
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                        textStyle = TextStyle(color = Color.Black, fontSize = 14.sp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = GreenPrimary,
                            unfocusedBorderColor = CardBorderColor,
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White,
                            focusedTextColor = Color.Black,
                            unfocusedTextColor = Color.Black
                        ),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Create Confirm Password Field
                    OutlinedTextField(
                        value = confirmPassword,
                        onValueChange = {
                            confirmPassword = it
                            dialogError = null
                        },
                        label = { Text("Create Confirm Password") },
                        placeholder = { Text("Re-enter new password to confirm") },
                        leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = GreenPrimary) },
                        trailingIcon = {
                            IconButton(onClick = { confirmPasswordVisible = !confirmPasswordVisible }) {
                                Icon(
                                    imageVector = if (confirmPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        },
                        visualTransformation = if (confirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        modifier = Modifier.fillMaxWidth(),
                        textStyle = TextStyle(color = Color.Black, fontSize = 14.sp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = GreenPrimary,
                            unfocusedBorderColor = CardBorderColor,
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White,
                            focusedTextColor = Color.Black,
                            unfocusedTextColor = Color.Black
                        ),
                        singleLine = true
                    )

                    if (dialogError != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = dialogError!!,
                            color = CrimsonError,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    // Action Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = onDismiss,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, CardBorderColor)
                        ) {
                            Text("Cancel", color = TextDark)
                        }

                        Button(
                            onClick = {
                                val trimmedId = identifier.trim()
                                val hasSymbol = createPassword.any { !it.isLetterOrDigit() && !it.isWhitespace() }

                                if (trimmedId.isBlank()) {
                                    dialogError = "Please enter your Registered Email or Roll No."
                                } else if (createPassword.isBlank()) {
                                    dialogError = "Please create a new password."
                                } else if (createPassword.length < 8) {
                                    dialogError = "Password must be at least 8 characters long."
                                } else if (!hasSymbol) {
                                    dialogError = "Password must contain at least 1 symbol (e.g. @, #, $, !)."
                                } else if (confirmPassword.isBlank()) {
                                    dialogError = "Please confirm your new password in the Confirm Password field."
                                } else if (createPassword != confirmPassword) {
                                    dialogError = "Password mismatch! Create Password and Confirm Password must match."
                                } else {
                                    val result = onPasswordReset(trimmedId, createPassword, updatedName.trim().ifBlank { null })
                                    if (!result.first) {
                                        dialogError = result.second
                                    }
                                }
                            },
                            modifier = Modifier.weight(1.3f),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary)
                        ) {
                            Text("Update Password", fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }
            }
        }
    }
}
