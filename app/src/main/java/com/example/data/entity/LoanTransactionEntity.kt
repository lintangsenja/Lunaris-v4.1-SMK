package com.example.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "loan_transactions")
data class LoanTransactionEntity(
    @PrimaryKey val idTransaksi: String,
    val tanggal: String,
    val namaPeminjam: String,
    val kelas: String,
    val waktu: String,
    val kondisi: String, // "Baik" or "Rusak"
    val namaPetugas: String,
    val status: String, // "Dipinjam" or "Kembali"
    val tanggalKembali: String? = null,
    val waktuKembali: String? = null,
    val kondisiKembali: String? = null, // "Baik" or "Rusak"
    val petugasKembali: String? = null,
    val keteranganKerusakan: String? = null,
    val whatsappNumber: String? = null,
    val durasiHari: Int = 1,
    val isDemo: Boolean = false,
    val tujuanPeminjaman: String? = null,
    val detailTujuan: String? = null
)
