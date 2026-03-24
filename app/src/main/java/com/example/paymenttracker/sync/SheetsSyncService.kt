package com.example.paymenttracker.sync

import android.content.Context
import com.example.paymenttracker.data.PaymentEntity
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

class SheetsSyncService {

    private val client = OkHttpClient()
    private val jsonMedia = "application/json; charset=utf-8".toMediaType()

    fun testConnection(
        scriptUrl: String,
        apiKey: String
    ): Boolean {
        val body = org.json.JSONObject()
            .put("action", "test")
            .put("apiKey", apiKey)

        val res = postJson(scriptUrl, body)
        return res.first
    }

    fun uploadPayment(
        p: com.example.paymenttracker.data.PaymentEntity,
        scriptUrl: String = SheetsConfig.WEB_APP_URL,
        apiKey: String = SheetsConfig.API_KEY
    ): Boolean {
        val paymentJson = org.json.JSONObject()
            .put("id", p.id)
            .put("date", p.date)
            .put("amountCents", p.amountCents)
            .put("beneficiary", p.beneficiary)
            .put("note", p.note)
            .put("bankName", p.bankName)
            .put("accountName", p.accountName)
            .put("accountNumber", p.accountNumber)
            .put("branchCode", p.branchCode)
            .put("paymentReference", p.paymentReference)
            .put("isTaxDeductible", p.isTaxDeductible)
            .put("frequency", p.frequency)

        val body = org.json.JSONObject()
            .put("action", "appendPayment")
            .put("apiKey", apiKey)
            .put("payment", paymentJson)

        val res = postJson(scriptUrl, body)
        return res.first
    }

    private fun postJson(url: String, json: JSONObject): Pair<Boolean, String> {
        return try {
            val reqBody = json.toString().toRequestBody(jsonMedia)

            val request = Request.Builder()
                .url(url)
                .post(reqBody)
                .build()

            client.newCall(request).execute().use { response ->
                val text = response.body?.string().orEmpty()
                val ok = response.isSuccessful && text.contains("\"ok\":true")
                Pair(ok, text)
            }
        } catch (e: Exception) {
            android.util.Log.e("SYNC_DEBUG", "postJson error", e)
            Pair(false, e.message ?: "Unknown error")
        }
    }
}