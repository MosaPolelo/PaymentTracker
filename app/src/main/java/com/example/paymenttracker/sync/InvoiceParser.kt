package com.example.paymenttracker.sync

object InvoiceParser {

    data class ParsedInvoice(
        val amountCents: Long,
        val amountText: String,
        val reference: String,
        val beneficiary: String
    )

    fun parse(text: String): ParsedInvoice {
        // --- Amount ---
        val amountRegexes = listOf(
            Regex("""R\s?(\d{1,3}(?:[,\s]\d{3})*(?:\.\d{2})?)""", RegexOption.IGNORE_CASE),
            Regex("""(\d{1,3}(?:[,\s]\d{3})*(?:\.\d{2})?)\s?ZAR""", RegexOption.IGNORE_CASE),
            Regex("""(?:amount|total)[^\d]{0,12}(\d+(?:\.\d{2})?)""", RegexOption.IGNORE_CASE)
        )
        var amountText = ""
        for (rx in amountRegexes) {
            val m = rx.find(text)
            if (m != null) {
                amountText = m.groupValues[1].replace(Regex("[,\\s]"), "")
                break
            }
        }
        val amountCents = amountText.toDoubleOrNull()?.let { (it * 100).toLong() } ?: 0L

        // --- Reference ---
        val refRegexes = listOf(
            Regex("""reference[^\w]{0,10}([A-Z0-9\-_/]{4,})""", RegexOption.IGNORE_CASE),
            Regex("""ref[^\w]{0,10}([A-Z0-9\-_/]{4,})""", RegexOption.IGNORE_CASE),
            Regex("""invoice\s?(?:no|number)?[^\w]{0,10}([A-Z0-9\-_/]{4,})""", RegexOption.IGNORE_CASE)
        )
        var reference = ""
        for (rx in refRegexes) {
            val m = rx.find(text)
            if (m != null) { reference = m.groupValues[1]; break }
        }

        // --- Beneficiary ---
        val fromLine = Regex("""^from:\s*(.+)$""",
            setOf(RegexOption.IGNORE_CASE, RegexOption.MULTILINE)).find(text)
        val beneficiary = if (fromLine != null) {
            fromLine.groupValues[1].substringBefore("<").trim()
        } else {
            val known = listOf("FNB", "ABSA", "Nedbank", "Standard Bank", "Capitec",
                "Takealot", "Eskom", "Vodacom", "MTN", "Telkom", "SARS")
            known.firstOrNull { text.contains(it, ignoreCase = true) } ?: ""
        }

        return ParsedInvoice(amountCents, amountText, reference, beneficiary)
    }
}
