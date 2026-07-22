package com.example.data.model

data class ItemWithStock(
    val idBarang: String,
    val namaBarang: String,
    val stokAwal: Int,
    val stokTersedia: Int,
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
