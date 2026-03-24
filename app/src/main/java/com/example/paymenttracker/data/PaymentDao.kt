package com.example.paymenttracker.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface PaymentDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(payment: PaymentEntity)

    @Delete
    suspend fun delete(payment: PaymentEntity)

    @Query("SELECT * FROM payments WHERE id = :id LIMIT 1")
    fun getPaymentById(id: Long): Flow<PaymentEntity?>

    @Query("SELECT * FROM payments ORDER BY date DESC, id DESC")
    fun getAllPayments(): Flow<List<PaymentEntity>>

    @Query("SELECT * FROM payments ORDER BY date DESC, id DESC")
    suspend fun getAllPaymentsOnce(): List<PaymentEntity>

    @Query("DELETE FROM payments")
    suspend fun clearAll()

    @Query("UPDATE payments SET matchStatus = :status WHERE id = :id")
    suspend fun updateMatchStatus(id: Long, status: String)
}