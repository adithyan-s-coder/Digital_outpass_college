package com.example.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.data.models.User
import com.example.data.models.UserRole

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey val id: String,
    val name: String,
    val email: String,
    val role: String,
    val regNo: String = "",
    val department: String = "Computer Science",
    val hostelBlock: String = "Block A",
    val roomNumber: String = "101",
    val phone: String = "",
    val parentPhone: String = "",
    val photoUri: String? = null,
    val password: String = "Pass@1234",
    val lastPasswordResetAt: Long? = null
) {
    fun toUser(): User = User(
        id = id,
        name = name,
        email = email,
        role = try { UserRole.valueOf(role) } catch (_: Exception) { UserRole.STUDENT },
        regNo = regNo,
        department = department,
        hostelBlock = hostelBlock,
        roomNumber = roomNumber,
        phone = phone,
        parentPhone = parentPhone,
        photoUri = photoUri,
        password = password,
        lastPasswordResetAt = lastPasswordResetAt
    )

    companion object {
        fun fromUser(user: User): UserEntity = UserEntity(
            id = user.id,
            name = user.name,
            email = user.email,
            role = user.role.name,
            regNo = user.regNo,
            department = user.department,
            hostelBlock = user.hostelBlock,
            roomNumber = user.roomNumber,
            phone = user.phone,
            parentPhone = user.parentPhone,
            photoUri = user.photoUri,
            password = user.password,
            lastPasswordResetAt = user.lastPasswordResetAt
        )
    }
}
