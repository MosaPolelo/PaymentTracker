package com.example.paymenttracker.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.paymenttracker.data.PaymentDao
import com.example.paymenttracker.data.PaymentEntity
import com.example.paymenttracker.sync.ConfigDataStore
import com.example.paymenttracker.sync.GmailFinanceEmail
import com.example.paymenttracker.sync.GmailSyncService
import com.example.paymenttracker.sync.PaymentMatcher
import com.example.paymenttracker.sync.SheetsSyncService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import com.example.paymenttracker.sync.MatchResult

class PaymentsViewModel(
    private val dao: PaymentDao
) : ViewModel() {

    val payments: StateFlow<List<PaymentEntity>> =
        dao.getAllPayments()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = emptyList()
            )

    fun paymentById(id: Long) = dao.getPaymentById(id)

    fun upsertPayment(
        id: Long = 0,
        date: String = LocalDate.now().toString(),
        amountCents: Long,
        beneficiary: String,
        note: String,
        bankName: String,
        accountName: String,
        accountNumber: String,
        branchCode: String,
        paymentReference: String,
        isTaxDeductible: Boolean,
        frequency: String,
        slipImageUri: String? = null,
        slipRawText: String? = null
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            dao.upsert(
                PaymentEntity(
                    id = id,
                    date = date,
                    amountCents = amountCents,
                    beneficiary = beneficiary,
                    note = note,
                    bankName = bankName,
                    accountName = accountName,
                    accountNumber = accountNumber,
                    branchCode = branchCode,
                    paymentReference = paymentReference,
                    isTaxDeductible = isTaxDeductible,
                    frequency = frequency,
                    slipImageUri = slipImageUri,
                    slipRawText = slipRawText
                )
            )
        }
    }

    fun deletePayment(payment: PaymentEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            dao.delete(payment)
        }
    }

    fun syncAllToSheets(
        context: Context,
        onResult: (Boolean, String) -> Unit
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val savedUrl = ConfigDataStore.getUrl(context)
                val savedKey = ConfigDataStore.getKey(context)

                android.util.Log.d("SYNC_DEBUG", "savedUrl=$savedUrl")
                android.util.Log.d("SYNC_DEBUG", "savedKey=$savedKey")

                if (savedUrl.isNullOrBlank() || savedKey.isNullOrBlank()) {
                    withContext(Dispatchers.Main) {
                        onResult(false, "Check QR Config")
                    }
                    return@launch
                }

                val service = SheetsSyncService()

                val ok = service.testConnection(savedUrl, savedKey)
                android.util.Log.d("SYNC_DEBUG", "testConnection ok=$ok")

                if (!ok) {
                    withContext(Dispatchers.Main) {
                        onResult(false, "Connection failed. Check URL / key.")
                    }
                    return@launch
                }

                val list = dao.getAllPaymentsOnce()
                var successCount = 0

                list.forEach { payment ->
                    if (service.uploadPayment(payment, savedUrl, savedKey)) {
                        successCount++
                    }
                }

                withContext(Dispatchers.Main) {
                    onResult(true, "Synced $successCount / ${list.size} payments ✅")
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    onResult(false, "Sync error: ${e.message}")
                }
            }
        }
    }

    fun syncFinanceEmails(
        context: Context,
        onResult: (String, List<GmailFinanceEmail>) -> Unit
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val savedUrl = ConfigDataStore.getUrl(context)
                val savedKey = ConfigDataStore.getKey(context)

                if (savedUrl.isNullOrBlank() || savedKey.isNullOrBlank()) {
                    withContext(Dispatchers.Main) {
                        onResult("Check QR Config first.", emptyList())
                    }
                    return@launch
                }

                val gmailSyncService = GmailSyncService()
                val result = gmailSyncService.fetchFinanceEmails(savedUrl, savedKey)

                withContext(Dispatchers.Main) {
                    if (result.isSuccess) {
                        val emails = result.getOrDefault(emptyList())
                        onResult("Gmail scan complete. Found ${emails.size} finance-like emails.", emails)
                    } else {
                        val err = result.exceptionOrNull()?.message ?: "Unknown Gmail sync error"
                        onResult("Gmail sync error: $err", emptyList())
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    onResult("Gmail sync error: ${e.message}", emptyList())
                }
            }
        }
    }

    fun matchEmailsToPayments(
        emails: List<GmailFinanceEmail>,
        onResult: (List<MatchResult>) -> Unit
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val allPayments = dao.getAllPaymentsOnce()
            val results = PaymentMatcher.match(emails, allPayments)

            // Update matchStatus in DB for any matched payments
            results.forEach { result ->
                if (result.payment != null && result.matchType != "none") {
                    dao.updateMatchStatus(result.payment.id, "matched")
                }
            }

            withContext(Dispatchers.Main) {
                onResult(results)
            }
        }
    }

}

class PaymentsViewModelFactory(
    private val dao: PaymentDao
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(PaymentsViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return PaymentsViewModel(dao) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

// Add to PaymentsViewModel.kt:

