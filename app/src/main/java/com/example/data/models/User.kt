package com.example.data.models

data class User(
    val id: String,
    val name: String,
    val email: String,
    val role: UserRole,
    val regNo: String = "",
    val department: String = "Computer Science",
    val hostelBlock: String = "Block A (Brahmaputra)",
    val roomNumber: String = "101",
    val phone: String = "+91 9876543210",
    val parentPhone: String = "+91 9123456789",
    val photoUri: String? = null
)
