package com.example.paymenttracker.ui.screens

import android.content.Context
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.paymenttracker.sync.InvoiceParser
import com.example.paymenttracker.sync.SlipOcr
import com.example.paymenttracker.viewmodel.PaymentsViewModel
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InvoiceImportScreen(
    vm: PaymentsViewModel,
    initialText: String = "",
    onBack: () -> Unit,
    onSaved: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }

    var loading by remember { mutableStateOf(false) }
    var pastedText by remember { mutableStateOf(initialText) }
    var rawExtracted by remember { mutableStateOf(if (initialText.isNotBlank()) initialText else "") }

    // Parsed / editable fields
    var beneficiary by remember { mutableStateOf("") }
    var amountText by remember { mutableStateOf("") }
    var reference by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }

    fun applyParsed(text: String) {
        rawExtracted = text
        val parsed = InvoiceParser.parse(text)
        if (beneficiary.isBlank()) beneficiary = parsed.beneficiary
        if (amountText.isBlank()) amountText = parsed.amountText
        if (reference.isBlank()) reference = parsed.reference
    }

    // Pre-parse if text was shared into app
    if (initialText.isNotBlank() && rawExtracted.isBlank()) {
        applyParsed(initialText)
    }

    // Image picker
    val imageLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                loading = true
                val text = SlipOcr.readTextFromUri(context, uri.toString()) ?: ""
                if (text.isNotBlank()) applyParsed(text)
                else snackbar.showSnackbar("No text found in image")
                loading = false
            }
        }
    }

    // PDF picker
    val pdfLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                loading = true
                val text = extractTextFromPdf(context, uri)
                if (text.isNotBlank()) applyParsed(text)
                else snackbar.showSnackbar("No text found in PDF")
                loading = false
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                title = { Text("Import Invoice") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            // --- Import source buttons ---
            ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("Choose source", style = MaterialTheme.typography.titleSmall)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { imageLauncher.launch("image/*") },
                            modifier = Modifier.weight(1f),
                            enabled = !loading
                        ) {
                            Icon(Icons.Default.Image, contentDescription = null)
                            Text(" Screenshot", style = MaterialTheme.typography.labelMedium)
                        }

                        OutlinedButton(
                            onClick = { pdfLauncher.launch("application/pdf") },
                            modifier = Modifier.weight(1f),
                            enabled = !loading
                        ) {
                            Icon(Icons.Default.Description, contentDescription = null)
                            Text(" PDF", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
            }

            // --- Paste text area ---
            ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                        Icon(Icons.Default.ContentPaste, contentDescription = null)
                        Text(
                            " Paste email / invoice text",
                            style = MaterialTheme.typography.titleSmall,
                            modifier = Modifier.padding(start = 4.dp)
                        )
                    }
                    OutlinedTextField(
                        value = pastedText,
                        onValueChange = { pastedText = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp),
                        placeholder = { Text("Paste invoice or email content here…") }
                    )
                    Button(
                        onClick = {
                            if (pastedText.isNotBlank()) applyParsed(pastedText)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = pastedText.isNotBlank() && !loading
                    ) {
                        Text("Parse Pasted Text")
                    }
                }
            }

            if (loading) {
                CircularProgressIndicator()
            }

            // --- Parsed / editable fields ---
            if (rawExtracted.isNotBlank() || beneficiary.isNotBlank() || amountText.isNotBlank()) {
                ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("Parsed Details — edit if needed",
                            style = MaterialTheme.typography.titleSmall)

                        OutlinedTextField(
                            value = beneficiary,
                            onValueChange = { beneficiary = it },
                            label = { Text("Beneficiary / Vendor") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = amountText,
                            onValueChange = { amountText = it },
                            label = { Text("Amount (e.g. 1234.56)") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = reference,
                            onValueChange = { reference = it },
                            label = { Text("Reference / Invoice No.") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = note,
                            onValueChange = { note = it },
                            label = { Text("Note (optional)") },
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(Modifier.height(4.dp))

                        Button(
                            onClick = {
                                val cents = amountText.replace(Regex("[,\\s]"), "")
                                    .toDoubleOrNull()?.let { (it * 100).toLong() } ?: 0L
                                vm.upsertPayment(
                                    date = LocalDate.now().toString(),
                                    amountCents = cents,
                                    beneficiary = beneficiary,
                                    note = note,
                                    bankName = "",
                                    accountName = "",
                                    accountNumber = "",
                                    branchCode = "",
                                    paymentReference = reference,
                                    isTaxDeductible = false,
                                    frequency = "once",
                                    slipRawText = rawExtracted.take(2000)
                                )
                                scope.launch {
                                    snackbar.showSnackbar("Payment saved!")
                                    onSaved()
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = beneficiary.isNotBlank() || amountText.isNotBlank()
                        ) {
                            Text("Save as Payment")
                        }
                    }
                }
            }

            // Raw extracted text (collapsed preview)
            if (rawExtracted.isNotBlank()) {
                ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("Raw extracted text", style = MaterialTheme.typography.titleSmall)
                        Spacer(Modifier.height(4.dp))
                        Text(
                            rawExtracted.take(600) + if (rawExtracted.length > 600) "…" else "",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }
    }
}

private suspend fun extractTextFromPdf(context: Context, uri: Uri): String {
    return withContext(Dispatchers.IO) {
        try {
            val fd = context.contentResolver.openFileDescriptor(uri, "r")
                ?: return@withContext ""
            val renderer = PdfRenderer(fd)
            val sb = StringBuilder()
            val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

            for (i in 0 until renderer.pageCount) {
                val page = renderer.openPage(i)
                val bmp = Bitmap.createBitmap(
                    page.width * 2, page.height * 2, Bitmap.Config.ARGB_8888
                )
                page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                page.close()
                val image = InputImage.fromBitmap(bmp, 0)
                val result = recognizer.process(image).await()
                sb.appendLine(result.text)
                bmp.recycle()
            }
            renderer.close()
            fd.close()
            sb.toString().trim()
        } catch (e: Exception) {
            android.util.Log.e("INVOICE_IMPORT", "PDF extract failed", e)
            ""
        }
    }
}
