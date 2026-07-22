package com.example.data.model

data class ReportDetailItem(
    val id: String,
    val name: String,
    val categoryOrRoom: String,
    val quantity: Int,
    val dateOrStatus: String,
    val extra: String = ""
)
