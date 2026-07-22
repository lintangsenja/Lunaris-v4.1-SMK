package com.example.data.model

import com.example.data.entity.LoanItemEntity
import com.example.data.entity.LoanTransactionEntity

data class LoanWithItems(
    val transaction: LoanTransactionEntity,
    val items: List<LoanItemEntity>
)
