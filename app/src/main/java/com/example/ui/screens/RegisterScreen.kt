package com.example.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContactPhone
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.RotateRight
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOut
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.ui.window.Dialog
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.material.icons.filled.Warning
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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import coil.compose.AsyncImage
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.CrimsonError
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.IndigoPrimary
import com.example.ui.theme.Slate700
import com.example.ui.theme.Slate800
import com.example.ui.theme.Slate900
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.net.Uri
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.example.ui.util.ImageCropUtil
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.foundation.Canvas
import androidx.compose.ui.geometry.Offset
import com.example.data.models.User
import com.example.data.models.UserRole
import com.example.data.models.DepartmentConstants
import com.example.ui.theme.GreenContainer
import com.example.ui.theme.GreenPrimary
import com.example.ui.theme.GreenDark
import com.example.ui.theme.TextDark
import com.example.ui.theme.TextMuted
import com.example.ui.theme.WhiteCard
import com.example.ui.theme.CardBorderColor
import com.example.ui.theme.GreenMintBackground
import com.example.ui.theme.GreenBorder
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.IndigoPrimary
import com.example.ui.theme.Slate700
import com.example.ui.theme.Slate800
import com.example.ui.theme.Slate900
import java.util.UUID
import com.example.ui.components.PremiumAppLogo

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterScreen(
    existingUsers: List<User> = emptyList(),
    onRegisterSuccess: (User) -> Unit,
    onBackToLogin: () -> Unit
) {
    var selectedTab by remember { mutableStateOf(0) }

    // Faculty Portal Security Lock State
    var isStaffPortalUnlocked by remember { mutableStateOf(false) }
    var showPasscodeDialog by remember { mutableStateOf(false) }
    var dialogPasscodePrompt by remember { mutableStateOf("") }
    var dialogErrorMsg by remember { mutableStateOf<String?>(null) }

    // Student Registration State
    var fullName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var regNo by remember { mutableStateOf("") }
    var department by remember { mutableStateOf("Computer Science") }
    var phone by remember { mutableStateOf("") }
    var parentPhone by remember { mutableStateOf("") }
    var studentPhotoUri by remember { mutableStateOf<String?>(null) }

    // Staff / Faculty Registration State
    var staffFullName by remember { mutableStateOf("") }
    var staffEmail by remember { mutableStateOf("") }
    var staffPassword by remember { mutableStateOf("") }
    var staffPasswordVisible by remember { mutableStateOf(false) }
    var staffRole by remember { mutableStateOf(UserRole.STAFF_ADVISOR) }
    var staffDepartment by remember { mutableStateOf("Computer Science") }
    var staffPhone by remember { mutableStateOf("") }
    var staffPasscode by remember { mutableStateOf("") }
    var staffPhotoUri by remember { mutableStateOf<String?>(null) }

    var roleDropdownExpanded by remember { mutableStateOf(false) }
    var deptDropdownExpanded by remember { mutableStateOf(false) }
    var staffDeptDropdownExpanded by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    val departments = DepartmentConstants.ALL_DEPARTMENTS

    if (showPasscodeDialog) {
        AlertDialog(
            onDismissRequest = { showPasscodeDialog = false },
            containerColor = WhiteCard,
            titleContentColor = TextDark,
            icon = {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    tint = Color(0xFFD97706),
                    modifier = Modifier.size(28.dp)
                )
            },
            title = {
                Text(
                    text = "Faculty Security Passcode",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = TextDark
                )
            },
            text = {
                Column {
                    Text(
                        text = "Non-student registration is restricted to authorized personnel. Enter your official institutional security passcode to unlock the Faculty & Staff Portal.",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    OutlinedTextField(
                        value = dialogPasscodePrompt,
                        onValueChange = {
                            dialogPasscodePrompt = it
                            dialogErrorMsg = null
                        },
                        label = { Text("Faculty Security Passcode") },
                        placeholder = { Text("e.g. VETIAS2026") },
                        singleLine = true,
                        textStyle = fieldTextStyle(),
                        colors = fieldColors(),
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (dialogErrorMsg != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = dialogErrorMsg!!,
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 12.sp
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val validPasscodes = listOf("VETIAS2026", "STAFF2026", "HOD2026", "SECURITY2026", "ADMIN2026", "VETIAS")
                        if (validPasscodes.any { it.equals(dialogPasscodePrompt.trim(), ignoreCase = true) }) {
                            isStaffPortalUnlocked = true
                            selectedTab = 1
                            showPasscodeDialog = false
                        } else {
                            dialogErrorMsg = "Access Denied: Invalid Security Passcode. Access is strictly restricted to authorized faculty."
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD97706))
                ) {
                    Text("Verify & Unlock", fontWeight = FontWeight.Bold, color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showPasscodeDialog = false }) {
                    Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(20.dp)
    ) {
        item {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                IconButton(onClick = onBackToLogin) {
                    Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.onBackground)
                }
                Spacer(modifier = Modifier.width(4.dp))
                PremiumAppLogo(size = 36.dp)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isStaffPortalUnlocked && selectedTab == 1) "Faculty Registration" else "Student Outpass Registration",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            Text(
                text = if (isStaffPortalUnlocked && selectedTab == 1) "Authorized Faculty & Institutional Staff Account Setup" else "Register for VETIAS Student Outpass & Digital Campus Pass",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 48.dp)
            )

            Spacer(modifier = Modifier.height(18.dp))

            // Segmented Tab Switcher - ONLY shown if unlocked by staff passcode
            if (isStaffPortalUnlocked) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Slate800)
                        .padding(4.dp)
                ) {
                    Button(
                        onClick = { selectedTab = 0 },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (selectedTab == 0) IndigoPrimary else Color.Transparent,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Student Registration", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }

                    Button(
                        onClick = { selectedTab = 1 },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (selectedTab == 1) Color(0xFFD97706) else Color.Transparent,
                            contentColor = if (selectedTab == 1) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Faculty & Staff Portal", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))
            }

            if (selectedTab == 0) {
                // STUDENT REGISTRATION FORM
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Slate700),
                    colors = CardDefaults.cardColors(containerColor = Slate800)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp)
                    ) {
                        // Role Lock Badge
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(IndigoPrimary.copy(alpha = 0.15f))
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = IndigoPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Role: Student Outpass Member",
                                style = MaterialTheme.typography.labelMedium,
                                color = IndigoPrimary,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Profile Photo Picker (STUDENT)
                        ProfilePhotoPickerField(
                            selectedPhotoUri = studentPhotoUri,
                            onPhotoSelected = { studentPhotoUri = it },
                            title = "Student Profile Photo",
                            subtitle = "Official photo displayed on digital outpass & campus ID",
                            accentColor = IndigoPrimary
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Full Name
                        OutlinedTextField(
                            value = fullName,
                            onValueChange = { fullName = it },
                            label = { Text("Full Name") },
                            placeholder = { Text("Enter your Full Name") },
                            leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = IndigoPrimary) },
                            modifier = Modifier.fillMaxWidth(),
                            textStyle = fieldTextStyle(),
                            colors = fieldColors(),
                            singleLine = true
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Email
                        OutlinedTextField(
                            value = email,
                            onValueChange = { email = it },
                            label = { Text("College Email Address") },
                            placeholder = { Text("Enter your College Email Address") },
                            leadingIcon = { Icon(Icons.Default.Email, contentDescription = null, tint = IndigoPrimary) },
                            modifier = Modifier.fillMaxWidth(),
                            textStyle = fieldTextStyle(),
                            colors = fieldColors(),
                            singleLine = true
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Password
                        OutlinedTextField(
                            value = password,
                            onValueChange = {
                                password = it
                                errorMsg = null
                            },
                            label = { Text("Password") },
                            placeholder = { Text("Enter your Password") },
                            supportingText = {
                                Text(
                                    text = "Must be at least 8 characters with at least 1 symbol (e.g. @, #, $, !)",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 11.sp
                                )
                            },
                            leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = IndigoPrimary) },
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
                            textStyle = fieldTextStyle(),
                            colors = fieldColors(),
                            singleLine = true
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Roll No / Register Number (STUDENT MANDATORY FIELD)
                        OutlinedTextField(
                            value = regNo,
                            onValueChange = { regNo = it },
                            label = { Text("Roll No / Register Number") },
                            placeholder = { Text("Enter your Roll No / Register Number") },
                            leadingIcon = { Icon(Icons.Default.Badge, contentDescription = null, tint = IndigoPrimary) },
                            modifier = Modifier.fillMaxWidth(),
                            textStyle = fieldTextStyle(),
                            colors = fieldColors(),
                            singleLine = true
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Department Dropdown
                        ExposedDropdownMenuBox(
                            expanded = deptDropdownExpanded,
                            onExpandedChange = { deptDropdownExpanded = !deptDropdownExpanded }
                        ) {
                            OutlinedTextField(
                                value = department,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Department") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = deptDropdownExpanded) },
                                modifier = Modifier
                                    .menuAnchor()
                                    .fillMaxWidth(),
                                textStyle = fieldTextStyle(),
                                colors = fieldColors()
                            )
                            ExposedDropdownMenu(
                                expanded = deptDropdownExpanded,
                                onDismissRequest = { deptDropdownExpanded = false }
                            ) {
                                departments.forEach { dept ->
                                    val isCs = DepartmentConstants.isComputerScienceBased(dept)
                                    DropdownMenuItem(
                                        text = {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = dept,
                                                    color = TextDark,
                                                    fontWeight = if (dept == department) FontWeight.Bold else FontWeight.Normal,
                                                    modifier = Modifier.weight(1f)
                                                )
                                                if (isCs) {
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Box(
                                                        modifier = Modifier
                                                            .clip(RoundedCornerShape(4.dp))
                                                            .background(GreenContainer)
                                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                                    ) {
                                                        Text(
                                                            text = "CS Stream",
                                                            fontSize = 10.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = GreenPrimary
                                                        )
                                                    }
                                                }
                                            }
                                        },
                                        onClick = {
                                            department = dept
                                            deptDropdownExpanded = false
                                        }
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Personal Mobile Phone
                        OutlinedTextField(
                            value = phone,
                            onValueChange = { phone = it },
                            label = { Text("Personal Contact Mobile") },
                            placeholder = { Text("Enter your Phone Number") },
                            leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null, tint = IndigoPrimary) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            modifier = Modifier.fillMaxWidth(),
                            textStyle = fieldTextStyle(),
                            colors = fieldColors(),
                            singleLine = true
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Emergency / Parent Contact (STUDENT MANDATORY FIELD)
                        OutlinedTextField(
                            value = parentPhone,
                            onValueChange = { parentPhone = it },
                            label = { Text("Emergency / Parent Contact (Calling)") },
                            placeholder = { Text("Enter Parent Phone Number") },
                            leadingIcon = { Icon(Icons.Default.ContactPhone, contentDescription = null, tint = EmeraldSuccess) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            modifier = Modifier.fillMaxWidth(),
                            textStyle = fieldTextStyle(),
                            colors = fieldColors(),
                            singleLine = true
                        )

                        if (errorMsg != null) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = errorMsg!!,
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        Button(
                            onClick = {
                                val trimmedName = fullName.trim()
                                val trimmedEmail = email.trim()
                                val trimmedRegNo = regNo.trim()
                                val trimmedPhone = phone.trim()
                                val trimmedParentPhone = parentPhone.trim()
                                val hasSymbol = password.any { !it.isLetterOrDigit() && !it.isWhitespace() }

                                val emailExists = existingUsers.any { it.email.equals(trimmedEmail, ignoreCase = true) }
                                val regNoExists = existingUsers.any { it.regNo.equals(trimmedRegNo, ignoreCase = true) }

                                if (trimmedName.isBlank()) {
                                    errorMsg = "Full Name is required. Please enter your full name."
                                } else if (trimmedEmail.isBlank()) {
                                    errorMsg = "College Email Address is required. Please enter your email."
                                } else if (emailExists) {
                                    errorMsg = "Email address '$trimmedEmail' is already registered. Each account must have a unique email."
                                } else if (password.isBlank()) {
                                    errorMsg = "Password is required. Please enter a password."
                                } else if (password.length < 8) {
                                    errorMsg = "Password must be at least 8 characters long."
                                } else if (!hasSymbol) {
                                    errorMsg = "Password must contain at least 1 symbol (e.g. @, #, $, !)."
                                } else if (trimmedRegNo.isBlank()) {
                                    errorMsg = "Roll No / Register Number is required. Please enter your Roll No."
                                } else if (regNoExists) {
                                    errorMsg = "Roll No / Register Number '$trimmedRegNo' is already registered to another student. Each student must have a unique Roll No."
                                } else if (department.isBlank()) {
                                    errorMsg = "Department selection is required. Please select your department."
                                } else if (trimmedPhone.isBlank()) {
                                    errorMsg = "Personal Contact Mobile is required. Please enter your contact phone number."
                                } else if (trimmedParentPhone.isBlank()) {
                                    errorMsg = "Emergency / Parent Contact Phone is required. Please enter parent mobile."
                                } else if (studentPhotoUri.isNullOrBlank()) {
                                    errorMsg = "Profile Photo is required. Please tap 'Upload from Device' to add and adjust your official student photo."
                                } else {
                                    val newUser = User(
                                        id = "u-" + UUID.randomUUID().toString().take(6),
                                        name = trimmedName,
                                        email = trimmedEmail,
                                        role = UserRole.STUDENT,
                                        regNo = trimmedRegNo,
                                        department = department,
                                        hostelBlock = "Campus Residence",
                                        roomNumber = "N/A",
                                        phone = trimmedPhone,
                                        parentPhone = trimmedParentPhone,
                                        photoUri = studentPhotoUri,
                                        password = password
                                    )
                                    onRegisterSuccess(newUser)
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = IndigoPrimary),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Create Student Outpass Account", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }

                if (!isStaffPortalUnlocked) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(
                            onClick = {
                                dialogPasscodePrompt = ""
                                dialogErrorMsg = null
                                showPasscodeDialog = true
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Faculty / Staff Authorization Setup",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                // FACULTY & STAFF REGISTRATION FORM
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFF59E0B).copy(alpha = 0.5f)),
                    colors = CardDefaults.cardColors(containerColor = Slate800)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp)
                    ) {
                        // Official Authorization Header
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFFF59E0B).copy(alpha = 0.15f))
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = null,
                                tint = Color(0xFFFCD34D),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Protected Faculty & Staff Portal Setup",
                                style = MaterialTheme.typography.labelMedium,
                                color = Color(0xFFFCD34D),
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Role Selector Dropdown (Staff Advisor / HOD / Security / Admin)
                        ExposedDropdownMenuBox(
                            expanded = roleDropdownExpanded,
                            onExpandedChange = { roleDropdownExpanded = !roleDropdownExpanded }
                        ) {
                            OutlinedTextField(
                                value = staffRole.displayName,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Institutional Role") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = roleDropdownExpanded) },
                                modifier = Modifier
                                    .menuAnchor()
                                    .fillMaxWidth(),
                                colors = fieldColors()
                            )
                            ExposedDropdownMenu(
                                expanded = roleDropdownExpanded,
                                onDismissRequest = { roleDropdownExpanded = false }
                            ) {
                                listOf(
                                    UserRole.STAFF_ADVISOR,
                                    UserRole.HOD,
                                    UserRole.SECURITY_OFFICER,
                                    UserRole.ADMIN
                                ).forEach { role ->
                                    DropdownMenuItem(
                                        text = { Text(role.displayName) },
                                        onClick = {
                                            staffRole = role
                                            if (role == UserRole.SECURITY_OFFICER) {
                                                staffDepartment = "Main Gate Security"
                                            } else if (role == UserRole.ADMIN) {
                                                staffDepartment = "Campus Administration"
                                            }
                                            roleDropdownExpanded = false
                                        }
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Profile Photo Picker (FACULTY & STAFF)
                        ProfilePhotoPickerField(
                            selectedPhotoUri = staffPhotoUri,
                            onPhotoSelected = { staffPhotoUri = it },
                            title = "Official ID Photo",
                            subtitle = "Photo used for ${staffRole.displayName} badge & audit verification",
                            accentColor = Color(0xFFF59E0B)
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        // Full Name
                        OutlinedTextField(
                            value = staffFullName,
                            onValueChange = { staffFullName = it },
                            label = { Text("Faculty / Officer Full Name") },
                            placeholder = { Text("Enter your Full Name") },
                            leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = IndigoPrimary) },
                            modifier = Modifier.fillMaxWidth(),
                            colors = fieldColors(),
                            singleLine = true
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Official Email
                        OutlinedTextField(
                            value = staffEmail,
                            onValueChange = { staffEmail = it },
                            label = { Text("Official Institutional Email") },
                            placeholder = { Text("Enter your Email") },
                            leadingIcon = { Icon(Icons.Default.Email, contentDescription = null, tint = IndigoPrimary) },
                            modifier = Modifier.fillMaxWidth(),
                            colors = fieldColors(),
                            singleLine = true
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Password
                        OutlinedTextField(
                            value = staffPassword,
                            onValueChange = {
                                staffPassword = it
                                errorMsg = null
                            },
                            label = { Text("Password") },
                            placeholder = { Text("Enter your Password") },
                            supportingText = {
                                Text(
                                    text = "Must be at least 8 characters with at least 1 symbol (e.g. @, #, $, !)",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 11.sp
                                )
                            },
                            leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = IndigoPrimary) },
                            trailingIcon = {
                                IconButton(onClick = { staffPasswordVisible = !staffPasswordVisible }) {
                                    Icon(
                                        imageVector = if (staffPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                        contentDescription = if (staffPasswordVisible) "Hide password" else "Show password",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            },
                            visualTransformation = if (staffPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            modifier = Modifier.fillMaxWidth(),
                            colors = fieldColors(),
                            singleLine = true
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Department Dropdown (Only shown for Staff Advisor and HOD roles)
                        if (staffRole == UserRole.STAFF_ADVISOR || staffRole == UserRole.HOD) {
                            ExposedDropdownMenuBox(
                                expanded = staffDeptDropdownExpanded,
                                onExpandedChange = { staffDeptDropdownExpanded = !staffDeptDropdownExpanded }
                            ) {
                                OutlinedTextField(
                                    value = staffDepartment,
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text("Department") },
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = staffDeptDropdownExpanded) },
                                    modifier = Modifier
                                        .menuAnchor()
                                        .fillMaxWidth(),
                                    colors = fieldColors()
                                )
                                ExposedDropdownMenu(
                                    expanded = staffDeptDropdownExpanded,
                                    onDismissRequest = { staffDeptDropdownExpanded = false }
                                ) {
                                    departments.forEach { dept ->
                                        val isCs = DepartmentConstants.isComputerScienceBased(dept)
                                        DropdownMenuItem(
                                            text = {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Text(
                                                        text = dept,
                                                        color = TextDark,
                                                        fontWeight = if (dept == staffDepartment) FontWeight.Bold else FontWeight.Normal,
                                                        modifier = Modifier.weight(1f)
                                                    )
                                                    if (isCs) {
                                                        Spacer(modifier = Modifier.width(6.dp))
                                                        Box(
                                                            modifier = Modifier
                                                                .clip(RoundedCornerShape(4.dp))
                                                                .background(GreenContainer)
                                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                                        ) {
                                                            Text(
                                                                text = "CS Stream",
                                                                fontSize = 10.sp,
                                                                fontWeight = FontWeight.Bold,
                                                                color = GreenPrimary
                                                            )
                                                        }
                                                    }
                                                }
                                            },
                                            onClick = {
                                                staffDepartment = dept
                                                staffDeptDropdownExpanded = false
                                            }
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))
                        }

                        // Contact Mobile
                        OutlinedTextField(
                            value = staffPhone,
                            onValueChange = { staffPhone = it },
                            label = { Text("Contact Mobile Number") },
                            placeholder = { Text("Enter your Phone Number") },
                            leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null, tint = IndigoPrimary) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            modifier = Modifier.fillMaxWidth(),
                            colors = fieldColors(),
                            singleLine = true
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Security Authorization Passcode Field
                        OutlinedTextField(
                            value = staffPasscode,
                            onValueChange = {
                                staffPasscode = it
                                errorMsg = null
                            },
                            label = { Text("Faculty Security Passcode") },
                            placeholder = { Text("Enter Security Passcode") },
                            supportingText = {
                                Text(
                                    text = "Requires institutional security passcode (Default Passcode: VETIAS2026)",
                                    color = Color(0xFFFCD34D),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            },
                            leadingIcon = { Icon(Icons.Default.Badge, contentDescription = null, tint = Color(0xFFF59E0B)) },
                            modifier = Modifier.fillMaxWidth(),
                            colors = fieldColors(),
                            singleLine = true
                        )

                        if (errorMsg != null) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = errorMsg!!,
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        Button(
                            onClick = {
                                val trimmedStaffName = staffFullName.trim()
                                val trimmedStaffEmail = staffEmail.trim()
                                val trimmedStaffPhone = staffPhone.trim()
                                val trimmedStaffPasscode = staffPasscode.trim()
                                val hasSymbol = staffPassword.any { !it.isLetterOrDigit() && !it.isWhitespace() }
                                val validPasscodes = listOf("VETIAS2026", "STAFF2026", "HOD2026", "SECURITY2026", "ADMIN2026", "VETIAS")

                                val emailExists = existingUsers.any { it.email.equals(trimmedStaffEmail, ignoreCase = true) }

                                if (trimmedStaffName.isBlank()) {
                                    errorMsg = "Faculty / Officer Full Name is required. Please enter your name."
                                } else if (trimmedStaffEmail.isBlank()) {
                                    errorMsg = "Official Institutional Email is required. Please enter your email."
                                } else if (emailExists) {
                                    errorMsg = "Email address '$trimmedStaffEmail' is already registered. Each account must have a unique email."
                                } else if (staffPassword.isBlank()) {
                                    errorMsg = "Password is required. Please enter a password."
                                } else if (staffPassword.length < 8) {
                                    errorMsg = "Password must be at least 8 characters long."
                                } else if (!hasSymbol) {
                                    errorMsg = "Password must contain at least 1 symbol (e.g. @, #, $, !)."
                                } else if ((staffRole == UserRole.STAFF_ADVISOR || staffRole == UserRole.HOD) && staffDepartment.isBlank()) {
                                    errorMsg = "Department selection is required. Please select department."
                                } else if (trimmedStaffPhone.isBlank()) {
                                    errorMsg = "Contact Mobile Number is required. Please enter your phone number."
                                } else if (trimmedStaffPasscode.isBlank()) {
                                    errorMsg = "Faculty Security Passcode is required. Please enter passcode."
                                } else if (validPasscodes.none { it.equals(trimmedStaffPasscode, ignoreCase = true) }) {
                                    errorMsg = "Access Denied: Invalid Faculty Security Passcode. Non-student registration requires the official passcode (Default: VETIAS2026)."
                                } else if (staffPhotoUri.isNullOrBlank()) {
                                    errorMsg = "Profile Photo is required. Please tap 'Upload from Device' to add and adjust your official faculty photo."
                                } else {
                                    val finalDept = when (staffRole) {
                                        UserRole.SECURITY_OFFICER -> "Main Gate Security"
                                        UserRole.ADMIN -> "Campus Administration"
                                        else -> staffDepartment
                                    }
                                    val newUser = User(
                                        id = "u-" + UUID.randomUUID().toString().take(6),
                                        name = trimmedStaffName,
                                        email = trimmedStaffEmail,
                                        role = staffRole,
                                        regNo = if (staffRole == UserRole.ADMIN) "ADM-" + UUID.randomUUID().toString().take(4).uppercase() else "EMP-" + UUID.randomUUID().toString().take(5).uppercase(),
                                        department = finalDept,
                                        hostelBlock = if (staffRole == UserRole.ADMIN) "Admin Tower" else "Faculty Quarters",
                                        roomNumber = "N/A",
                                        phone = trimmedStaffPhone,
                                        parentPhone = "",
                                        photoUri = staffPhotoUri,
                                        password = staffPassword
                                    )
                                    onRegisterSuccess(newUser)
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD97706)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, tint = Color.White)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Create Official " + staffRole.displayName + " Account", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(30.dp))
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
    cursorColor = Color.Black,
    focusedLabelColor = IndigoPrimary,
    unfocusedLabelColor = TextMuted,
    focusedPlaceholderColor = TextMuted,
    unfocusedPlaceholderColor = TextMuted,
    disabledTextColor = Color.Black,
    errorTextColor = CrimsonError
)

private fun fieldTextStyle() = TextStyle(
    color = Color.Black,
    fontSize = 15.sp,
    fontWeight = FontWeight.Normal
)

@Composable
fun ProfilePhotoPickerField(
    selectedPhotoUri: String?,
    onPhotoSelected: (String?) -> Unit,
    title: String,
    subtitle: String,
    accentColor: Color
) {
    var showAdjustDialog by remember { mutableStateOf(false) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            onPhotoSelected(uri.toString())
            showAdjustDialog = true
        }
    }

    if (showAdjustDialog && selectedPhotoUri != null) {
        AdjustPhotoDialog(
            photoUri = selectedPhotoUri,
            accentColor = accentColor,
            onSaveCroppedUri = { croppedUri ->
                onPhotoSelected(croppedUri)
                showAdjustDialog = false
            },
            onDismiss = { showAdjustDialog = false }
        )
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, if (selectedPhotoUri != null) accentColor.copy(alpha = 0.5f) else CardBorderColor),
        colors = CardDefaults.cardColors(containerColor = WhiteCard)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.AddAPhoto,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = TextDark
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "*Required",
                            style = MaterialTheme.typography.labelSmall,
                            color = CrimsonError,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp
                        )
                    }
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted,
                        fontSize = 11.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Clean, non-broken Profile Photo Viewport
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(150.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFE2E8F0))
                    .border(3.dp, if (selectedPhotoUri != null) accentColor else CardBorderColor, CircleShape)
                    .clickable {
                        if (selectedPhotoUri != null) {
                            showAdjustDialog = true
                        } else {
                            photoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        }
                    }
            ) {
                if (selectedPhotoUri != null) {
                    AsyncImage(
                        model = selectedPhotoUri,
                        contentDescription = "Adjustable Profile Photo",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                tint = TextMuted,
                                modifier = Modifier.size(44.dp)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "TAP TO UPLOAD",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = accentColor
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            if (selectedPhotoUri != null) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(GreenContainer)
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Crop,
                        contentDescription = null,
                        tint = GreenDark,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = "Photo adjusted & ready • Tap 'Adjust with Finger' to edit",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = GreenDark
                    )
                }
            } else {
                Text(
                    text = "* Photo is required for campus ID and outpass gate exit",
                    fontSize = 11.sp,
                    color = AmberWarning,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = {
                        photoPickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, accentColor),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = accentColor)
                ) {
                    Icon(
                        imageVector = Icons.Default.Image,
                        contentDescription = null,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (selectedPhotoUri != null) "Change Photo" else "Upload from Device",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                if (selectedPhotoUri != null) {
                    Spacer(modifier = Modifier.width(8.dp))

                    Button(
                        onClick = { showAdjustDialog = true },
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = accentColor)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Crop,
                            contentDescription = null,
                            modifier = Modifier.size(15.dp),
                            tint = Color.White
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = "Adjust with Finger", fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    TextButton(
                        onClick = {
                            onPhotoSelected(null)
                        },
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = null,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                        Text(text = "Remove", fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun AdjustPhotoDialog(
    photoUri: String,
    accentColor: Color,
    onSaveCroppedUri: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var tempZoom by remember { mutableFloatStateOf(1.0f) }
    var tempOffsetX by remember { mutableFloatStateOf(0f) }
    var tempOffsetY by remember { mutableFloatStateOf(0f) }
    var isProcessing by remember { mutableStateOf(false) }

    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = WhiteCard),
            border = BorderStroke(1.dp, CardBorderColor)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Crop,
                            contentDescription = null,
                            tint = accentColor,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Adjust Profile Photo",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextDark
                        )
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = TextMuted)
                    }
                }

                Text(
                    text = "Slide & drag image with your finger in any direction to frame face. Pinch to zoom.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMuted,
                    fontSize = 11.sp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp, bottom = 12.dp)
                )

                // Large Interactive Finger-Adjust Canvas with strictly clamped bounds
                val currentMaxPan = ((tempZoom - 1.0f) * 110f).coerceAtLeast(0f)

                Box(
                    modifier = Modifier
                        .size(240.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color(0xFF0F172A))
                        .border(2.dp, accentColor, RoundedCornerShape(20.dp))
                        .pointerInput(photoUri) {
                            detectTransformGestures { _, pan, zoom, _ ->
                                val newZoom = (tempZoom * zoom).coerceIn(1.0f, 4.0f)
                                tempZoom = newZoom
                                val maxP = ((newZoom - 1.0f) * 110f).coerceAtLeast(0f)
                                tempOffsetX = (tempOffsetX + pan.x).coerceIn(-maxP, maxP)
                                tempOffsetY = (tempOffsetY + pan.y).coerceIn(-maxP, maxP)
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    AsyncImage(
                        model = photoUri,
                        contentDescription = "Finger Adjust Photo",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                scaleX = tempZoom
                                scaleY = tempZoom
                                translationX = tempOffsetX
                                translationY = tempOffsetY
                            }
                    )

                    // Translucent circular photo frame guide
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val radius = 100.dp.toPx()
                        val center = Offset(size.width / 2, size.height / 2)
                        drawCircle(
                            color = Color.White,
                            radius = radius,
                            center = center,
                            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 3.dp.toPx())
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Directional D-Pad Finger Adjuster
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(GreenContainer)
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Pan with Finger:",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = GreenDark
                    )

                    IconButton(
                        onClick = {
                            if (tempZoom <= 1.05f) tempZoom = 1.3f
                            val maxP = ((tempZoom - 1f) * 110f).coerceAtLeast(0f)
                            tempOffsetX = (tempOffsetX - 20f).coerceIn(-maxP, maxP)
                        },
                        modifier = Modifier.size(30.dp)
                    ) {
                        Icon(Icons.Default.KeyboardArrowLeft, contentDescription = "Left", tint = TextDark, modifier = Modifier.size(20.dp))
                    }

                    IconButton(
                        onClick = {
                            if (tempZoom <= 1.05f) tempZoom = 1.3f
                            val maxP = ((tempZoom - 1f) * 110f).coerceAtLeast(0f)
                            tempOffsetY = (tempOffsetY - 20f).coerceIn(-maxP, maxP)
                        },
                        modifier = Modifier.size(30.dp)
                    ) {
                        Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Up", tint = TextDark, modifier = Modifier.size(20.dp))
                    }

                    IconButton(
                        onClick = {
                            tempOffsetX = 0f
                            tempOffsetY = 0f
                        },
                        modifier = Modifier.size(30.dp)
                    ) {
                        Icon(Icons.Default.CenterFocusStrong, contentDescription = "Center", tint = accentColor, modifier = Modifier.size(18.dp))
                    }

                    IconButton(
                        onClick = {
                            if (tempZoom <= 1.05f) tempZoom = 1.3f
                            val maxP = ((tempZoom - 1f) * 110f).coerceAtLeast(0f)
                            tempOffsetY = (tempOffsetY + 20f).coerceIn(-maxP, maxP)
                        },
                        modifier = Modifier.size(30.dp)
                    ) {
                        Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Down", tint = TextDark, modifier = Modifier.size(20.dp))
                    }

                    IconButton(
                        onClick = {
                            if (tempZoom <= 1.05f) tempZoom = 1.3f
                            val maxP = ((tempZoom - 1f) * 110f).coerceAtLeast(0f)
                            tempOffsetX = (tempOffsetX + 20f).coerceIn(-maxP, maxP)
                        },
                        modifier = Modifier.size(30.dp)
                    ) {
                        Icon(Icons.Default.KeyboardArrowRight, contentDescription = "Right", tint = TextDark, modifier = Modifier.size(20.dp))
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Zoom Slider & Controls
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = {
                            val newZ = (tempZoom - 0.15f).coerceAtLeast(1.0f)
                            tempZoom = newZ
                            val maxP = ((newZ - 1f) * 110f).coerceAtLeast(0f)
                            tempOffsetX = tempOffsetX.coerceIn(-maxP, maxP)
                            tempOffsetY = tempOffsetY.coerceIn(-maxP, maxP)
                        },
                        modifier = Modifier.size(34.dp)
                    ) {
                        Icon(Icons.Default.ZoomOut, contentDescription = "Zoom Out", tint = TextDark, modifier = Modifier.size(20.dp))
                    }

                    Slider(
                        value = tempZoom,
                        onValueChange = { newZ ->
                            tempZoom = newZ
                            val maxP = ((newZ - 1f) * 110f).coerceAtLeast(0f)
                            tempOffsetX = tempOffsetX.coerceIn(-maxP, maxP)
                            tempOffsetY = tempOffsetY.coerceIn(-maxP, maxP)
                        },
                        valueRange = 1.0f..4.0f,
                        modifier = Modifier.weight(1f),
                        colors = SliderDefaults.colors(
                            thumbColor = accentColor,
                            activeTrackColor = accentColor,
                            inactiveTrackColor = CardBorderColor
                        )
                    )

                    IconButton(
                        onClick = {
                            val newZ = (tempZoom + 0.15f).coerceAtMost(4.0f)
                            tempZoom = newZ
                        },
                        modifier = Modifier.size(34.dp)
                    ) {
                        Icon(Icons.Default.ZoomIn, contentDescription = "Zoom In", tint = TextDark, modifier = Modifier.size(20.dp))
                    }

                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${(tempZoom * 100).toInt()}%",
                        color = TextDark,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.width(40.dp)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Bottom Buttons (Reset, Save & Apply)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            tempZoom = 1.0f
                            tempOffsetX = 0f
                            tempOffsetY = 0f
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = TextDark),
                        border = BorderStroke(1.dp, CardBorderColor),
                        enabled = !isProcessing
                    ) {
                        Text("Reset All", fontSize = 12.sp, color = TextDark)
                    }

                    Button(
                        onClick = {
                            if (isProcessing) return@Button
                            isProcessing = true
                            coroutineScope.launch(Dispatchers.IO) {
                                val panXRatio = if (currentMaxPan > 0f) -(tempOffsetX / currentMaxPan).coerceIn(-1f, 1f) else 0f
                                val panYRatio = if (currentMaxPan > 0f) -(tempOffsetY / currentMaxPan).coerceIn(-1f, 1f) else 0f

                                val cropped = ImageCropUtil.cropAndSaveSquare(
                                    context = context,
                                    sourceUri = Uri.parse(photoUri),
                                    zoom = tempZoom,
                                    panXPercent = panXRatio,
                                    panYPercent = panYRatio
                                )

                                withContext(Dispatchers.Main) {
                                    isProcessing = false
                                    if (cropped != null) {
                                        onSaveCroppedUri(cropped)
                                    } else {
                                        onSaveCroppedUri(photoUri)
                                    }
                                }
                            }
                        },
                        modifier = Modifier.weight(1.3f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                        enabled = !isProcessing
                    ) {
                        if (isProcessing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Saving...", fontSize = 13.sp, color = Color.White)
                        } else {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.White)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Save & Apply", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }
            }
        }
    }
}



