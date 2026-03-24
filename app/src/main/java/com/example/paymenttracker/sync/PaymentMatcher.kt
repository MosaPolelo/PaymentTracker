package com.example.paymenttracker.sync

import com.example.paymenttracker.data.PaymentEntity

data class MatchResult(
    val email: GmailFinanceEmail,
    val payment: PaymentEntity?,
    val matchType: String   // "reference", "name", "none"
)

object PaymentMatcher {

    fun match(
        emails: List<GmailFinanceEmail>,
        payments: List<PaymentEntity>
    ): List<MatchResult> {
        return emails.map { email -> findBestMatch(email, payments) }
    }

    private fun findBestMatch(
        email: GmailFinanceEmail,
        payments: List<PaymentEntity>
    ): MatchResult {
        // 1. Try reference exact match first (strongest signal)
        if (email.reference.isNotBlank()) {
            val refMatch = payments.firstOrNull { payment ->
                payment.paymentReference.trim().equals(
                    email.reference.trim(), ignoreCase = true
                )
            }
            if (refMatch != null) {
                return MatchResult(email, refMatch, "reference")
            }
        }

        // 2. Try fuzzy name match on beneficiary vs vendor and sender
        val nameMatch = payments.firstOrNull { payment ->
            fuzzyNameMatch(payment.beneficiary, email.vendor) ||
                    fuzzyNameMatch(payment.beneficiary, extractSenderName(email.from))
        }
        if (nameMatch != null) {
            return MatchResult(email, nameMatch, "name")
        }

        return MatchResult(email, null, "none")
    }

    // Returns true if either string contains the other (case-insensitive, trimmed)
    private fun fuzzyNameMatch(a: String, b: String): Boolean {
        if (a.isBlank() || b.isBlank()) return false
        val cleanA = a.trim().lowercase()
        val cleanB = b.trim().lowercase()
        return cleanA.contains(cleanB) || cleanB.contains(cleanA)
    }

    // Extracts "John Smith" from "John Smith <john@example.com>"
    private fun extractSenderName(from: String): String {
        return from.substringBefore("<").trim()
    }
}