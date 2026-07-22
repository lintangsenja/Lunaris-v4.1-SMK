package com.example.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "bahan_afkir")
data class BahanAfkirEntity(
    @PrimaryKey val idAfkir: String,
    val idBarang: String,
    val namaBarang: String,
    val jumlahAfkir: Int,
    val satuan: String,
    val alasan: String, // Kedaluwarsa, Rusak Fisik, Hilang
    val tanggalAfkir: String,
    val status: String = "Aktif",
    val isDemo: Boolean = false
)
