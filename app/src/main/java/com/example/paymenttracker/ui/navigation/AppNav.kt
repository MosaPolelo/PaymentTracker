package com.example.paymenttracker.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.paymenttracker.ui.screens.AddPaymentScreen
import com.example.paymenttracker.ui.screens.BankingDetailsScreen
import com.example.paymenttracker.ui.screens.ConfigScanScreen
import com.example.paymenttracker.ui.screens.DashboardScreen
import com.example.paymenttracker.ui.screens.EditPaymentScreen
import com.example.paymenttracker.ui.screens.GmailSyncScreen
import com.example.paymenttracker.ui.screens.InvoiceImportScreen
import com.example.paymenttracker.ui.screens.PaymentsScreen
import com.example.paymenttracker.ui.screens.SettingsScreen
import com.example.paymenttracker.ui.screens.SlipViewerScreen
import com.example.paymenttracker.viewmodel.PaymentsViewModel

object Routes {
    const val DASHBOARD = "dashboard"
    const val PAYMENTS = "payments"
    const val ADD_PAYMENT = "add_payment"
    const val EDIT_PAYMENT = "edit_payment"
    const val BANKING = "banking"
    const val SETTINGS = "settings"
    const val CONFIG_QR = "config_qr"
    const val VIEW_SLIP = "view_slip"
    const val GMAIL_SYNC = "gmail_sync"
    const val INVOICE_IMPORT = "invoice_import"
}

@Composable
fun AppNav(vm: PaymentsViewModel, initialSharedText: String = "") {
    val navController = rememberNavController()

    Scaffold(
        topBar = { }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = if (initialSharedText.isNotBlank()) Routes.INVOICE_IMPORT else Routes.DASHBOARD,
            modifier = Modifier.padding(padding)
        ) {
            composable(Routes.DASHBOARD) {
                DashboardScreen(
                    onNewPayment = { navController.navigate(Routes.ADD_PAYMENT) },
                    onPayments = { navController.navigate(Routes.PAYMENTS) },
                    onBankingDetails = { navController.navigate(Routes.BANKING) },
                    onSettings = { navController.navigate(Routes.SETTINGS) }
                )
            }

            composable(Routes.PAYMENTS) {
                PaymentsScreen(
                    vm = vm,
                    onAdd = { navController.navigate(Routes.ADD_PAYMENT) },
                    onEdit = { id ->
                        navController.navigate("${Routes.EDIT_PAYMENT}/$id")
                    },
                    onViewSlip = { uri ->
                        val encoded = java.net.URLEncoder.encode(uri, "UTF-8")
                        navController.navigate("${Routes.VIEW_SLIP}/$encoded")
                    }
                )
            }

            composable(Routes.ADD_PAYMENT) {
                AddPaymentScreen(
                    vm = vm,
                    onDone = { navController.popBackStack() }
                )
            }

            composable(
                route = "${Routes.EDIT_PAYMENT}/{id}",
                arguments = listOf(navArgument("id") { type = NavType.LongType })
            ) { backStackEntry ->
                val id = backStackEntry.arguments?.getLong("id") ?: 0L
                EditPaymentScreen(
                    vm = vm,
                    paymentId = id,
                    onDone = { navController.popBackStack() }
                )
            }

            composable(Routes.BANKING) {
                BankingDetailsScreen()
            }

            composable(Routes.SETTINGS) {
                SettingsScreen(
                    vm = vm,
                    onScanConfigQr = {
                        navController.navigate(Routes.CONFIG_QR)
                    },
                    onOpenGmailSync = {
                        navController.navigate(Routes.GMAIL_SYNC)
                    },
                    onOpenInvoiceImport = {
                        navController.navigate(Routes.INVOICE_IMPORT)
                    }
                )
            }

            composable(Routes.CONFIG_QR) {
                ConfigScanScreen(
                    onConfigSaved = {
                        navController.popBackStack()
                    }
                )
            }

            composable(Routes.GMAIL_SYNC) {
                GmailSyncScreen(
                    vm = vm,
                    onBack = { navController.popBackStack() }
                )
            }

            composable(Routes.INVOICE_IMPORT) {
                InvoiceImportScreen(
                    vm = vm,
                    initialText = initialSharedText,
                    onBack = { navController.popBackStack() },
                    onSaved = { navController.popBackStack() }
                )
            }

            composable(
                route = "${Routes.VIEW_SLIP}/{slipUri}",
                arguments = listOf(navArgument("slipUri") { type = NavType.StringType })
            ) { backStackEntry ->
                val slipUri = backStackEntry.arguments?.getString("slipUri").orEmpty()
                SlipViewerScreen(
                    slipUri = java.net.URLDecoder.decode(slipUri, "UTF-8"),
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}