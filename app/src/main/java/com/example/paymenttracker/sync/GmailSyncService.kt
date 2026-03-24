package com.example.paymenttracker.sync

import android.util.Log
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.security.KeyStore
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManagerFactory
import javax.net.ssl.X509TrustManager

data class GmailFinanceEmail(
    val id: String,
    val threadId: String,
    val subject: String,
    val from: String,
    val date: String,
    val snippet: String,
    val type: String,
    val confidence: Int,
    val amountText: String,
    val amountCents: Long,
    val reference: String,
    val vendor: String,
    val hasAttachments: Boolean
)

class GmailSyncService {

    private val client: OkHttpClient = run {
        val tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm())
        tmf.init(null as KeyStore?)
        val trustManagers = tmf.trustManagers
        val sslContext = SSLContext.getInstance("TLS")
        sslContext.init(null, trustManagers, null)
        OkHttpClient.Builder()
            .sslSocketFactory(sslContext.socketFactory, trustManagers[0] as X509TrustManager)
            .build()
    }

    private val jsonMedia = "application/json; charset=utf-8".toMediaType()

    fun fetchFinanceEmails(
        scriptUrl: String,
        apiKey: String
    ): Result<List<GmailFinanceEmail>> {
        return try {
            val fmt = SimpleDateFormat("yyyy/MM/dd", Locale.getDefault())
            val cal = Calendar.getInstance()
            cal.set(Calendar.DAY_OF_MONTH, 1)
            val afterDate = fmt.format(cal.time)
            cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH))
            val beforeDate = fmt.format(cal.time)

            val body = JSONObject()
                .put("action", "scanGmailFinance")
                .put("apiKey", apiKey)
                .put("afterDate", afterDate)
                .put("beforeDate", beforeDate)

            val requestBody = body.toString().toRequestBody(jsonMedia)

            val request = Request.Builder()
                .url(scriptUrl)
                .post(requestBody)
                .build()

            client.newCall(request).execute().use { response ->
                val rawText = response.body?.string().orEmpty()

                Log.d("GMAIL_SYNC", "HTTP ${response.code}")
                Log.d("GMAIL_SYNC", "RAW RESPONSE: $rawText")

                if (!response.isSuccessful) {
                    return Result.failure(
                        Exception("HTTP ${response.code}: $rawText")
                    )
                }

                val json = JSONObject(rawText)

                val ok = json.optBoolean("ok", false)
                if (!ok) {
                    val message = json.optString("message", "Unknown Gmail sync error")
                    return Result.failure(Exception(message))
                }

                val emailsArray = json.optJSONArray("emails") ?: JSONArray()
                val items = mutableListOf<GmailFinanceEmail>()

                for (i in 0 until emailsArray.length()) {
                    val item = emailsArray.getJSONObject(i)

                    items.add(
                        GmailFinanceEmail(
                            id = item.optString("id"),
                            threadId = item.optString("threadId"),
                            subject = item.optString("subject"),
                            from = item.optString("from"),
                            date = item.optString("date"),
                            snippet = item.optString("snippet"),
                            type = item.optString("type"),
                            confidence = item.optInt("confidence", 0),
                            amountText = item.optString("amountText"),
                            amountCents = item.optLong("amountCents", 0L),
                            reference = item.optString("reference"),
                            vendor = item.optString("vendor"),
                            hasAttachments = item.optBoolean("hasAttachments", false)
                        )
                    )
                }

                Result.success(items)
            }
        } catch (e: Exception) {
            Log.e("GMAIL_SYNC", "fetchFinanceEmails failed", e)
            Result.failure(Exception(e.message ?: "Unknown Gmail sync error"))
        }
    }
}
