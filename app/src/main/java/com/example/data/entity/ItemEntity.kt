package com.example.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "items")
data class ItemEntity(
    @PrimaryKey val idBarang: String,
    val namaBarang: String,
    val stokAwal: Int = 0,
    val kategori: String = "",
    val satuan: String = "",
    val stokRusak: Int = 0,
    val merekAlat: String = "",
    val ruang: String = "",
    val sumberDana: String? = null,
    val kondisi: String = "",
    val keterangan: String = "",
    val isDemo: Boolean = false,
    val type: String = "ALAT",
    val isBorrowable: Boolean = true
)
