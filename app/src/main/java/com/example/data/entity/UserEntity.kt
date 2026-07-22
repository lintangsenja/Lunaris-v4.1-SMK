package com.example.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey val username: String,
    val password: String,
    val role: String, // "super_admin", "admin", "siswa"
    val fullName: String = "",
    val createdAt: Long = System.currentTimeMillis()
)
