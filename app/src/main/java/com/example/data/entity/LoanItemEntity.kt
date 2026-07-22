package com.example.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "loan_items")
data class LoanItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val idTransaksi: String,
    val idBarang: String,
    val namaBarang: String,
    val jumlah: Int,
    val isDemo: Boolean = false
)
