package com.example.paymenttracker.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDate

@Entity(tableName = "payments")
data class PaymentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,

    val date: String = LocalDate.now().toString(),

    val amountCents: Long,
    val beneficiary: String,
    val note: String,

    val bankName: String,
    val accountName: String,
    val accountNumber: String,
    val branchCode: String,
    val paymentReference: String,

    val isTaxDeductible: Boolean,
    val frequency: String,

    // NEW: payment slip support
    val slipImageUri: String? = null,
    val slipRawText: String? = null,
    val matchStatus: String = "unmatched"
)