package com.moneytracker.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val amount: Long, // Changed from Double to Long to avoid precision issues
    val note: String,
    val category: String,
    val type: String, // "income" or "expense"
    val date: Long, // timestamp
    val quantity: Int = 1
)
