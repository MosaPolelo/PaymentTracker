package com.example.paymenttracker.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.paymenttracker.sync.GmailFinanceEmail
import com.example.paymenttracker.sync.SlipOcr
import com.example.paymenttracker.sync.SlipStorage
import com.example.paymenttracker.data.PaymentEntity
import com.example.paymenttracker.viewmodel.PaymentsViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import com.example.paymenttracker.sync.MatchResult


@Composable
fun DashboardScreen(
    onNewPayment: () -> Unit,
    onBankingDetails: () -> Unit,
    onSettings: () -> Unit,
    onPayments: () -> Unit
) {
    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = "Dashboard",
                style = MaterialTheme.typography.headlineLarge
            )

            BigTile(
                title = "New Payment",
                subtitle = "Add a payment with notes and proof of payment",
                modifier = Modifier.weight(1f),
                onClick = onNewPayment
            )

            BigTile(
                title = "Payments",
                subtitle = "View and manage your saved payments",
                modifier = Modifier.weight(1f),
                onClick = onPayments
            )

            BigTile(
                title = "Banking Details",
                subtitle = "Quick copy account details later",
                modifier = Modifier.weight(1f),
                onClick = onBankingDetails
            )

            BigTile(
                title = "Settings",
                subtitle = "Google Sheets and Gmail sync",
                modifier = Modifier.weight(1f),
                onClick = onSettings
            )
        }
    }
}

@Composable
private fun BigTile(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    ElevatedCard(
        onClick = onClick,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(18.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = title, style = MaterialTheme.typography.headlineMedium)
            Text(text = subtitle, style = MaterialTheme.typography.bodyLarge)
        }
    }
}

private fun centsToRandString(cents: Long): String {
    val rands = cents / 100
    val rem = (cents % 100).toInt()
    val rem2 = if (rem < 10) "0$rem" else "$rem"
    return "R $rands.$rem2"
}

fun parseAmountToCents(input: String): Long? {
    val clean = input.trim()
        .replace("R", "", ignoreCase = true)
        .replace(",", "")
        .trim()

    if (clean.isBlank()) return null

    return try {
        val parts = clean.split(".")
        val whole = parts[0].toLong()
        val cents = when (parts.size) {
            1 -> 0
            else -> parts[1].take(2).padEnd(2, '0').toInt()
        }
        whole * 100 + cents
    } catch (_: Exception) {
        null
    }
}

@Composable
fun PaymentsScreen(
    vm: PaymentsViewModel,
    onAdd: () -> Unit,
    onEdit: (Long) -> Unit,
    onViewSlip: (String) -> Unit
) {
    val payments by vm.payments.collectAsState()

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Payments", style = MaterialTheme.typography.headlineMedium)
                Button(onClick = onAdd) { Text("Add") }
            }

            Spacer(Modifier.height(12.dp))

            if (payments.isEmpty()) {
                Text(
                    "No payments yet. Tap Add.",
                    style = MaterialTheme.typography.bodyMedium
                )
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(items = payments, key = { it.id }) { p ->
                        PaymentRow(
                            p = p,
                            onEdit = { onEdit(p.id) },
                            onDelete = { vm.deletePayment(p) },
                            onViewSlip = { uri -> onViewSlip(uri) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PaymentRow(
    p: PaymentEntity,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onViewSlip: (String) -> Unit
) {
    var showBanking by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Delete payment?") },
            text = { Text("This will remove the payment for '${p.beneficiary}'.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmDelete = false
                        onDelete()
                    }
                ) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) { Text("Cancel") }
            }
        )
    }

    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(p.beneficiary, style = MaterialTheme.typography.titleMedium)
                    Text(
                        "${p.date} • ${centsToRandString(p.amountCents)}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                Row {
                    IconButton(onClick = onEdit) {
                        Icon(Icons.Filled.Edit, contentDescription = "Edit")
                    }
                    IconButton(onClick = { confirmDelete = true }) {
                        Icon(Icons.Filled.Delete, contentDescription = "Delete")
                    }
                }
            }

            if (p.note.isNotBlank()) {
                Text(p.note, style = MaterialTheme.typography.bodySmall)
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AssistChip(
                    onClick = {},
                    label = { Text(if (p.isTaxDeductible) "Tax Deductible" else "Not Deductible") }
                )
                AssistChip(
                    onClick = {},
                    label = { Text("Frequency: ${p.frequency}") }
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = { showBanking = !showBanking }) {
                    Text(if (showBanking) "Hide banking details" else "Show banking details")
                }

                if (!p.slipImageUri.isNullOrBlank()) {
                    TextButton(onClick = { onViewSlip(p.slipImageUri!!) }) {
                        Icon(Icons.Filled.Visibility, contentDescription = null)
                        Spacer(Modifier.size(4.dp))
                        Text("View Slip")
                    }
                }
            }

            if (showBanking) {
                HorizontalDivider()
                Text("Bank: ${p.bankName}")
                Text("Account Name: ${p.accountName}")
                Text("Account Number: ${p.accountNumber}")
                Text("Branch Code: ${p.branchCode}")
                Text("Reference: ${p.paymentReference}")
            }
        }
    }
}

@Composable
fun AddPaymentScreen(
    vm: PaymentsViewModel,
    onDone: () -> Unit
) {
    PaymentFormScreen(
        title = "Add Payment",
        initial = null,
        onSave = { form ->
            vm.upsertPayment(
                amountCents = form.amountCents,
                beneficiary = form.beneficiary,
                note = form.note,
                bankName = form.bankName,
                accountName = form.accountName,
                accountNumber = form.accountNumber,
                branchCode = form.branchCode,
                paymentReference = form.reference,
                isTaxDeductible = form.isTaxDeductible,
                frequency = form.frequency,
                slipImageUri = form.slipImageUri,
                slipRawText = form.slipRawText
            )
            onDone()
        }
    )
}

@Composable
fun EditPaymentScreen(
    vm: PaymentsViewModel,
    paymentId: Long,
    onDone: () -> Unit
) {
    var loaded by remember { mutableStateOf<PaymentEntity?>(null) }

    LaunchedEffect(paymentId) {
        vm.paymentById(paymentId).collectLatest { p ->
            loaded = p
        }
    }

    if (loaded == null) {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
        return
    }

    PaymentFormScreen(
        title = "Edit Payment",
        initial = loaded,
        onSave = { form ->
            vm.upsertPayment(
                id = loaded!!.id,
                date = loaded!!.date,
                amountCents = form.amountCents,
                beneficiary = form.beneficiary,
                note = form.note,
                bankName = form.bankName,
                accountName = form.accountName,
                accountNumber = form.accountNumber,
                branchCode = form.branchCode,
                paymentReference = form.reference,
                isTaxDeductible = form.isTaxDeductible,
                frequency = form.frequency,
                slipImageUri = form.slipImageUri,
                slipRawText = form.slipRawText
            )
            onDone()
        }
    )
}

private data class PaymentForm(
    val amountCents: Long,
    val beneficiary: String,
    val note: String,
    val bankName: String,
    val accountName: String,
    val accountNumber: String,
    val branchCode: String,
    val reference: String,
    val isTaxDeductible: Boolean,
    val frequency: String,
    val slipImageUri: String? = null,
    val slipRawText: String? = null
)

@Composable
private fun PaymentFormScreen(
    title: String,
    initial: PaymentEntity?,
    onSave: (PaymentForm) -> Unit
) {
    var amount by remember { mutableStateOf(initial?.let { (it.amountCents / 100.0).toString() } ?: "") }
    var beneficiary by remember { mutableStateOf(initial?.beneficiary ?: "") }
    var note by remember { mutableStateOf(initial?.note ?: "") }

    var bankName by remember { mutableStateOf(initial?.bankName ?: "") }
    var accountName by remember { mutableStateOf(initial?.accountName ?: "") }
    var accountNumber by remember { mutableStateOf(initial?.accountNumber ?: "") }
    var branchCode by remember { mutableStateOf(initial?.branchCode ?: "") }
    var reference by remember { mutableStateOf(initial?.paymentReference ?: "") }

    var isTaxDeductible by remember { mutableStateOf(initial?.isTaxDeductible ?: false) }

    val frequencyOptions = listOf("Once", "Weekly", "Monthly", "Quarterly", "Yearly")
    var frequency by remember { mutableStateOf(initial?.frequency ?: "Once") }
    var frequencyMenuExpanded by remember { mutableStateOf(false) }

    var generalError by remember { mutableStateOf<String?>(null) }

    var amountError by remember { mutableStateOf(false) }
    var beneficiaryError by remember { mutableStateOf(false) }
    var bankNameError by remember { mutableStateOf(false) }
    var accountNameError by remember { mutableStateOf(false) }
    var accountNumberError by remember { mutableStateOf(false) }
    var branchCodeError by remember { mutableStateOf(false) }
    var referenceError by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var slipPreviewUri by remember {
        mutableStateOf(
            initial?.slipImageUri?.let { Uri.parse(it) }
        )
    }
    var savedSlipUriString by remember { mutableStateOf(initial?.slipImageUri) }
    var slipRawText by remember { mutableStateOf(initial?.slipRawText) }

    var isSavingSlip by remember { mutableStateOf(false) }
    var showSlipDialog by remember { mutableStateOf(false) }

    val tempCameraUri = remember { SlipStorage.createTempSlipUri(context) }

    val takePictureLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            isSavingSlip = true
            scope.launch(Dispatchers.IO) {
                val copiedUriString = SlipStorage.copyUriToAppStorage(context, tempCameraUri)
                if (copiedUriString != null) {
                    val ocrText = SlipOcr.readTextFromUri(context, copiedUriString)
                    launch(Dispatchers.Main) {
                        savedSlipUriString = copiedUriString
                        slipPreviewUri = Uri.parse(copiedUriString)
                        slipRawText = ocrText
                        isSavingSlip = false
                    }
                } else {
                    launch(Dispatchers.Main) {
                        generalError = "Failed to save payment slip image"
                        isSavingSlip = false
                    }
                }
            }
        }
    }

    if (showSlipDialog && !savedSlipUriString.isNullOrBlank()) {
        AlertDialog(
            onDismissRequest = { showSlipDialog = false },
            confirmButton = {
                TextButton(onClick = { showSlipDialog = false }) { Text("Close") }
            },
            title = { Text("Payment Slip") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    AsyncImage(
                        model = savedSlipUriString,
                        contentDescription = "Payment slip",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(240.dp),
                        contentScale = ContentScale.Fit
                    )

                    if (!slipRawText.isNullOrBlank()) {
                        Text("Slip OCR Text", style = MaterialTheme.typography.titleSmall)
                        Text(slipRawText!!, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        )
    }

    if (isSavingSlip) {
        AlertDialog(
            onDismissRequest = {},
            confirmButton = {},
            title = { Text("Saving slip") },
            text = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                    Text("Please wait...")
                }
            }
        )
    }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .imePadding()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(title, style = MaterialTheme.typography.headlineMedium)

            if (generalError != null) {
                Text(
                    text = generalError!!,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            OutlinedTextField(
                value = amount,
                onValueChange = {
                    amount = it
                    amountError = false
                    generalError = null
                },
                label = { Text("Amount (e.g. 250.00)") },
                isError = amountError,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            if (amountError) {
                Text("Please enter a valid amount", color = MaterialTheme.colorScheme.error)
            }

            OutlinedTextField(
                value = beneficiary,
                onValueChange = {
                    beneficiary = it
                    beneficiaryError = false
                    generalError = null
                },
                label = { Text("Beneficiary / Who did you pay?") },
                isError = beneficiaryError,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            if (beneficiaryError) {
                Text("Beneficiary is required", color = MaterialTheme.colorScheme.error)
            }

            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                label = { Text("Note (what was it for?)") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3
            )

            HorizontalDivider()

            Text("Tax & Frequency", style = MaterialTheme.typography.titleMedium)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Mark as tax deduction?")
                Switch(
                    checked = isTaxDeductible,
                    onCheckedChange = { isTaxDeductible = it }
                )
            }

            Box {
                Button(
                    onClick = { frequencyMenuExpanded = true }
                ) {
                    Text("Frequency: $frequency")
                }

                DropdownMenu(
                    expanded = frequencyMenuExpanded,
                    onDismissRequest = { frequencyMenuExpanded = false }
                ) {
                    frequencyOptions.forEach { option ->
                        DropdownMenuItem(
                            text = { Text(option) },
                            onClick = {
                                frequency = option
                                frequencyMenuExpanded = false
                            }
                        )
                    }
                }
            }

            HorizontalDivider()

            Text("Payment Slip / Proof", style = MaterialTheme.typography.titleMedium)

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = { takePictureLauncher.launch(tempCameraUri) },
                    enabled = !isSavingSlip
                ) {
                    Text(if (savedSlipUriString.isNullOrBlank()) "Capture Slip" else "Retake Slip")
                }

                if (!savedSlipUriString.isNullOrBlank()) {
                    Button(
                        onClick = { showSlipDialog = true },
                        enabled = !isSavingSlip
                    ) {
                        Icon(Icons.Filled.Visibility, contentDescription = null)
                        Spacer(Modifier.size(4.dp))
                        Text("View Slip")
                    }
                }
            }

            if (slipPreviewUri != null) {
                AsyncImage(
                    model = slipPreviewUri,
                    contentDescription = "Slip preview",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp),
                    contentScale = ContentScale.Fit
                )
            }

            if (!slipRawText.isNullOrBlank()) {
                ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("Slip OCR Text", style = MaterialTheme.typography.titleSmall)
                        Spacer(Modifier.height(6.dp))
                        Text(slipRawText!!, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }

            HorizontalDivider()

            Text("Banking Details (for this payment)", style = MaterialTheme.typography.titleMedium)

            OutlinedTextField(
                value = bankName,
                onValueChange = {
                    bankName = it
                    bankNameError = false
                    generalError = null
                },
                label = { Text("Bank name") },
                isError = bankNameError,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            if (bankNameError) {
                Text("Bank name is required", color = MaterialTheme.colorScheme.error)
            }

            OutlinedTextField(
                value = accountName,
                onValueChange = {
                    accountName = it
                    accountNameError = false
                    generalError = null
                },
                label = { Text("Account name") },
                isError = accountNameError,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            if (accountNameError) {
                Text("Account name is required", color = MaterialTheme.colorScheme.error)
            }

            OutlinedTextField(
                value = accountNumber,
                onValueChange = {
                    accountNumber = it
                    accountNumberError = false
                    generalError = null
                },
                label = { Text("Account number") },
                isError = accountNumberError,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            if (accountNumberError) {
                Text("Account number is required", color = MaterialTheme.colorScheme.error)
            }

            OutlinedTextField(
                value = branchCode,
                onValueChange = {
                    branchCode = it
                    branchCodeError = false
                    generalError = null
                },
                label = { Text("Branch code") },
                isError = branchCodeError,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            if (branchCodeError) {
                Text("Branch code is required", color = MaterialTheme.colorScheme.error)
            }

            OutlinedTextField(
                value = reference,
                onValueChange = {
                    reference = it
                    referenceError = false
                    generalError = null
                },
                label = { Text("Payment reference") },
                isError = referenceError,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            if (referenceError) {
                Text("Payment reference is required", color = MaterialTheme.colorScheme.error)
            }

            Spacer(Modifier.height(6.dp))

            Button(
                onClick = {
                    amountError = false
                    beneficiaryError = false
                    bankNameError = false
                    accountNameError = false
                    accountNumberError = false
                    branchCodeError = false
                    referenceError = false
                    generalError = null

                    val cents = parseAmountToCents(amount)
                    amountError = cents == null || cents <= 0
                    beneficiaryError = beneficiary.isBlank()
                    bankNameError = bankName.isBlank()
                    accountNameError = accountName.isBlank()
                    accountNumberError = accountNumber.isBlank()
                    branchCodeError = branchCode.isBlank()
                    referenceError = reference.isBlank()

                    val hasErrors =
                        amountError || beneficiaryError || bankNameError ||
                                accountNameError || accountNumberError ||
                                branchCodeError || referenceError

                    if (hasErrors) {
                        generalError = "Please fix the highlighted fields below."
                        return@Button
                    }

                    onSave(
                        PaymentForm(
                            amountCents = cents!!,
                            beneficiary = beneficiary,
                            note = note,
                            bankName = bankName,
                            accountName = accountName,
                            accountNumber = accountNumber,
                            branchCode = branchCode,
                            reference = reference,
                            isTaxDeductible = isTaxDeductible,
                            frequency = frequency,
                            slipImageUri = savedSlipUriString,
                            slipRawText = slipRawText
                        )
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isSavingSlip
            ) {
                Text("Save")
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SlipViewerScreen(
    slipUri: String,
    onBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Slip Viewer") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                AsyncImage(
                    model = slipUri,
                    contentDescription = "Slip image",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(420.dp),
                    contentScale = ContentScale.Fit
                )
            }
        }
    }
}

@Composable
fun BankingDetailsScreen() {
    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Banking Details", style = MaterialTheme.typography.headlineMedium)

            ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text("Bank: (set later)", style = MaterialTheme.typography.titleMedium)
                    Text("Account Name: (set later)")
                    Text("Account Number: (set later)")
                    Text("Branch Code: (set later)")
                    Text("Reference: (set later)")
                }
            }

            Text(
                text = "Next step: we’ll store these in Settings and add a Copy button.",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
fun SettingsScreen(
    vm: PaymentsViewModel,
    onScanConfigQr: () -> Unit,
    onOpenGmailSync: () -> Unit
) {
    val context = LocalContext.current
    var status by remember { mutableStateOf("Not connected") }

    var scriptUrl by remember {
        mutableStateOf("https://script.google.com/macros/s/AKfycbwUqgxW1Kg2c-fewqrRIIXSaHdWHMDzgAjzcaqQ0bSHXOp6aET2pw4-ztPV2mZaboQfyQ/exec")
    }
    var apiKey by remember {
        mutableStateOf("mosa-pt-2026-secret")
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Settings",
                style = MaterialTheme.typography.headlineMedium
            )

            ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("Google Sheets Config", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Scan a QR code that contains your Web App URL and API key.",
                        style = MaterialTheme.typography.bodyMedium
                    )

                    Button(
                        onClick = onScanConfigQr,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Scan Config QR")
                    }
                }
            }

            ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("Google Sheets Sync", style = MaterialTheme.typography.titleMedium)
                    Text("Status: $status", style = MaterialTheme.typography.bodySmall)
                    Text("Saved URL: $scriptUrl", style = MaterialTheme.typography.bodySmall)
                    Text("Saved Key: $apiKey", style = MaterialTheme.typography.bodySmall)

                    Button(
                        onClick = {
                            status = "Syncing..."
                            vm.syncAllToSheets(context = context) { _, msg ->
                                status = msg
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Sync Payments to Google Sheets")
                    }
                }
            }

            ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("Gmail Sync", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Scan Gmail for invoices, payment confirmations and finance-like emails.",
                        style = MaterialTheme.typography.bodyMedium
                    )

                    Button(
                        onClick = onOpenGmailSync,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Open Gmail Sync")
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GmailSyncScreen(
    vm: PaymentsViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current

    var status by remember { mutableStateOf("Ready") }
    var loading by remember { mutableStateOf(false) }
    var matchResults by remember { mutableStateOf<List<MatchResult>>(emptyList()) }
    var unmatchedEmail by remember { mutableStateOf<GmailFinanceEmail?>(null) }
    var showUnmatchedDialog by remember { mutableStateOf(false) }

    fun runScan() {
        loading = true
        status = "Checking Gmail..."
        vm.syncFinanceEmails(context) { msg, result ->
            if (result.isNotEmpty()) {
                vm.matchEmailsToPayments(result) { matches ->
                    matchResults = matches
                    val matchedCount = matches.count { it.matchType != "none" }
                    status = "$msg — $matchedCount matched to saved payments"
                    loading = false
                }
            } else {
                status = msg
                loading = false
            }
        }
    }

    // Dialog for unmatched email — ask user what to do
    if (showUnmatchedDialog && unmatchedEmail != null) {
        val email = unmatchedEmail!!
        AlertDialog(
            onDismissRequest = { showUnmatchedDialog = false },
            title = { Text("No match found") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("This email couldn't be matched to a saved payment:")
                    Spacer(Modifier.height(4.dp))
                    Text("Subject: ${email.subject}",
                        style = MaterialTheme.typography.bodySmall)
                    Text("From: ${email.from}",
                        style = MaterialTheme.typography.bodySmall)
                    if (email.amountText.isNotBlank()) {
                        Text("Amount: ${email.amountText}",
                            style = MaterialTheme.typography.bodySmall)
                    }
                    Spacer(Modifier.height(8.dp))
                    Text("What would you like to do?",
                        style = MaterialTheme.typography.bodyMedium)
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    showUnmatchedDialog = false
                    // Re-run scan to try again
                    runScan()
                }) {
                    Text("Try matching again")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showUnmatchedDialog = false
                    // TODO: navigate to AddPaymentScreen pre-filled
                    // For now just close — you can wire navigation later
                }) {
                    Text("Create new payment")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Gmail Sync") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { runScan() }) {
                        Icon(Icons.Filled.Refresh, contentDescription = "Refresh")
                    }
                }
            )
        }
    ) { padding ->
        Surface(
            modifier = Modifier.fillMaxSize().padding(padding),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(
                modifier = Modifier.fillMaxSize().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("Status", style = MaterialTheme.typography.titleMedium)
                        Text(status, style = MaterialTheme.typography.bodyMedium)
                        Button(
                            onClick = { runScan() },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            if (loading) {
                                CircularProgressIndicator(modifier = Modifier.size(18.dp))
                            } else {
                                Text("Scan Gmail Now")
                            }
                        }
                    }
                }

                if (matchResults.isNotEmpty()) {
                    val matched = matchResults.filter { it.matchType != "none" }
                    val unmatched = matchResults.filter { it.matchType == "none" }

                    if (matched.isNotEmpty()) {
                        Text(
                            "Matched (${matched.size})",
                            style = MaterialTheme.typography.titleMedium
                        )
                        LazyColumn(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(matched) { result ->
                                MatchResultCard(result = result)
                            }

                            if (unmatched.isNotEmpty()) {
                                item {
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        "Unmatched (${unmatched.size})",
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                }
                                items(unmatched) { result ->
                                    UnmatchedEmailCard(
                                        result = result,
                                        onAction = {
                                            unmatchedEmail = result.email
                                            showUnmatchedDialog = true
                                        }
                                    )
                                }
                            }
                        }
                    } else if (unmatched.isNotEmpty()) {
                        Text(
                            "Unmatched (${unmatched.size})",
                            style = MaterialTheme.typography.titleMedium
                        )
                        LazyColumn(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(unmatched) { result ->
                                UnmatchedEmailCard(
                                    result = result,
                                    onAction = {
                                        unmatchedEmail = result.email
                                        showUnmatchedDialog = true
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MatchResultCard(result: MatchResult) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    result.email.subject,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f)
                )
                AssistChip(
                    onClick = {},
                    label = {
                        Text(
                            if (result.matchType == "reference") "✓ Ref match"
                            else "~ Name match"
                        )
                    }
                )
            }

            Text("From: ${result.email.from}", style = MaterialTheme.typography.bodySmall)
            Text("Date: ${result.email.date}", style = MaterialTheme.typography.bodySmall)

            if (result.email.amountText.isNotBlank()) {
                Text("Amount: ${result.email.amountText}",
                    style = MaterialTheme.typography.bodySmall)
            }
            if (result.email.reference.isNotBlank()) {
                Text("Email ref: ${result.email.reference}",
                    style = MaterialTheme.typography.bodySmall)
            }

            if (result.payment != null) {
                HorizontalDivider()
                Text("Matched to:", style = MaterialTheme.typography.labelSmall)
                Text("→ ${result.payment.beneficiary} • ${result.payment.date}",
                    style = MaterialTheme.typography.bodySmall)
                Text("→ Ref: ${result.payment.paymentReference}",
                    style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun UnmatchedEmailCard(
    result: MatchResult,
    onAction: () -> Unit
) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    result.email.subject,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f)
                )
                AssistChip(
                    onClick = {},
                    label = { Text("No match") }
                )
            }

            Text("From: ${result.email.from}", style = MaterialTheme.typography.bodySmall)
            Text("Date: ${result.email.date}", style = MaterialTheme.typography.bodySmall)

            if (result.email.amountText.isNotBlank()) {
                Text("Amount: ${result.email.amountText}",
                    style = MaterialTheme.typography.bodySmall)
            }

            Text(result.email.snippet, style = MaterialTheme.typography.bodySmall)

            Button(
                onClick = onAction,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("What do I do with this?")
            }
        }
    }
}



@Composable
private fun GmailEmailCard(email: GmailFinanceEmail) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(email.subject, style = MaterialTheme.typography.titleMedium)
            Text("From: ${email.from}", style = MaterialTheme.typography.bodySmall)
            Text("Date: ${email.date}", style = MaterialTheme.typography.bodySmall)
            Text("Type: ${email.type} (${email.confidence}%)", style = MaterialTheme.typography.bodySmall)

            if (email.vendor.isNotBlank()) {
                Text("Vendor: ${email.vendor}", style = MaterialTheme.typography.bodySmall)
            }
            if (email.amountText.isNotBlank()) {
                Text("Amount: ${email.amountText}", style = MaterialTheme.typography.bodySmall)
            }
            if (email.reference.isNotBlank()) {
                Text("Reference: ${email.reference}", style = MaterialTheme.typography.bodySmall)
            }

            if (email.hasAttachments) {
                AssistChip(
                    onClick = {},
                    label = { Text("Has attachment") },
                    leadingIcon = {
                        Icon(Icons.Filled.Image, contentDescription = null)
                    }
                )
            }

            Text(email.snippet, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun GmailEmailSection(
    title: String,
    items: List<GmailFinanceEmail>,
    emptyText: String
) {
    Text(title, style = MaterialTheme.typography.headlineSmall)

    if (items.isEmpty()) {
        ElevatedCard(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = emptyText,
                modifier = Modifier.padding(14.dp),
                style = MaterialTheme.typography.bodyMedium
            )
        }
    } else {
        LazyColumn(
            modifier = Modifier.height(220.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(items) { email ->
                GmailEmailCard(email = email)
            }
        }
    }
}