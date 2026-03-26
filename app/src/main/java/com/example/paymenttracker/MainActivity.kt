package com.example.paymenttracker

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import com.example.paymenttracker.data.AppDatabase
import com.example.paymenttracker.ui.navigation.AppNav
import com.example.paymenttracker.ui.theme.PaymentTrackerTheme
import com.example.paymenttracker.viewmodel.PaymentsViewModel
import com.example.paymenttracker.viewmodel.PaymentsViewModelFactory

class MainActivity : ComponentActivity() {

    private val vm: PaymentsViewModel by viewModels {
        val dao = AppDatabase.getInstance(applicationContext).paymentDao()
        PaymentsViewModelFactory(dao)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val sharedText = extractSharedText(intent)
        setContent {
            PaymentTrackerTheme {
                AppNav(vm, initialSharedText = sharedText)
            }
        }
    }

    private fun extractSharedText(intent: Intent?): String {
        if (intent?.action != Intent.ACTION_SEND) return ""
        val mimeType = intent.type ?: return ""
        return when {
            mimeType.startsWith("text/") ->
                intent.getStringExtra(Intent.EXTRA_TEXT).orEmpty()
            else -> ""  // image/* and pdf handled by file picker in InvoiceImportScreen
        }
    }
}
