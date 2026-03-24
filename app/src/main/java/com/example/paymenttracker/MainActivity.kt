package com.example.paymenttracker

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

        setContent {
            PaymentTrackerTheme {
                AppNav(vm) // <-- IMPORTANT: no named parameter
            }
        }
    }
}