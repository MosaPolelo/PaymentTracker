package com.example.paymenttracker.ui.screens

import android.app.Activity
import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.paymenttracker.camera.QRScanActivity
import com.example.paymenttracker.sync.ConfigDataStore
import com.example.paymenttracker.sync.SheetsConfig
import kotlinx.coroutines.launch
import org.json.JSONObject

@Composable
fun ConfigScanScreen(
    onConfigSaved: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var status by remember { mutableStateOf("Waiting to scan config QR") }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val raw = result.data?.getStringExtra("SCAN_RESULT")

            if (raw.isNullOrBlank()) {
                status = "No QR result received"
                Toast.makeText(context, "No QR result received", Toast.LENGTH_SHORT).show()
                return@rememberLauncherForActivityResult
            }

            try {
                val json = JSONObject(raw)
                val key = json.getString("k").trim()
                val url = SheetsConfig.WEB_APP_URL.trim()

                if (key.isBlank()) {
                    status = "QR key is blank"
                    Toast.makeText(context, "QR key is blank", Toast.LENGTH_SHORT).show()
                    return@rememberLauncherForActivityResult
                }

                if (url.isBlank()) {
                    status = "Web app URL is blank in SheetsConfig"
                    Toast.makeText(context, "Web app URL is blank", Toast.LENGTH_SHORT).show()
                    return@rememberLauncherForActivityResult
                }

                scope.launch {
                    ConfigDataStore.save(context, url, key)

                    val savedUrl = ConfigDataStore.getUrl(context)
                    val savedKey = ConfigDataStore.getKey(context)

                    if (!savedUrl.isNullOrBlank() && !savedKey.isNullOrBlank()) {
                        status = "Config saved successfully"
                        Toast.makeText(context, "Config saved successfully", Toast.LENGTH_SHORT).show()
                        onConfigSaved()
                    } else {
                        status = "Config save failed"
                        Toast.makeText(context, "Config save failed", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                status = "Invalid QR config format"
                Toast.makeText(context, "Invalid QR config format", Toast.LENGTH_SHORT).show()
            }
        } else {
            status = "Scan cancelled"
            Toast.makeText(context, "Scan cancelled", Toast.LENGTH_SHORT).show()
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Scan Config QR",
                style = MaterialTheme.typography.headlineMedium
            )

            ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Use a QR that contains JSON like:",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = """{"k":"mosa-pt-2026-secret"}""",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        text = "Status: $status",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            Button(
                onClick = {
                    val intent = Intent(context, QRScanActivity::class.java)
                    launcher.launch(intent)
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Open QR Scanner")
            }
        }
    }
}