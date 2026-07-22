package com.example.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "profile")
data class ProfileEntity(
    @PrimaryKey val id: Int = 1,
    val namaPetugas: String,
    val nip: String,
    val namaInstansi: String,
    val fotoUri: String
)
