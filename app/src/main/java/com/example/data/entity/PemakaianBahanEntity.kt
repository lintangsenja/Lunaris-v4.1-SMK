package com.example.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "pemakaian_bahan")
data class PemakaianBahanEntity(
    @PrimaryKey val idPemakaian: String,
    val idBarang: String,
    val namaBarang: String,
    val jumlahDiambil: Int,
    val satuan: String,
    val namaPeminta: String,
    val jabatan: String,
    val kelas: String?,
    val namaPetugas: String,
    val tanggalPemakaian: String,
    val keterangan: String,
    val isDemo: Boolean = false
)
