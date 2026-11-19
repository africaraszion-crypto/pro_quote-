package com.example.ui.util

import com.example.data.model.Quote
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Currency
import java.util.Date
import java.util.Locale
import java.util.TimeZone

object Formatters {

    val supportedCurrencies = listOf("TZS", "USD", "KES", "UGX", "EUR", "GBP", "ZAR")

    fun formatCurrency(amount: Double, currencyCode: String?): String {
        val code = currencyCode?.trim()?.uppercase(Locale.US)?.ifBlank { "TZS" } ?: "TZS"
        return try {
            val format = NumberFormat.getCurrencyInstance(Locale.US)
            val currency = Currency.getInstance(code)
            format.currency = currency
            if (code in setOf("TZS", "KES", "UGX")) {
                format.maximumFractionDigits = 0
                format.minimumFractionDigits = 0
            } else {
                format.maximumFractionDigits = 2
                format.minimumFractionDigits = 2
            }
            format.format(amount)
        } catch (_: Exception) {
            "$code ${String.format(Locale.US, "%,.2f", amount)}"
        }
    }

    fun formatDisplayDate(raw: String?): String {
        if (raw.isNullOrBlank()) return "—"
        val patterns = listOf(
            "yyyy-MM-dd'T'HH:mm:ss.SSSSSSX",
            "yyyy-MM-dd'T'HH:mm:ss.SSSX",
            "yyyy-MM-dd'T'HH:mm:ssX",
            "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
            "yyyy-MM-dd'T'HH:mm:ss'Z'",
            "yyyy-MM-dd'T'HH:mm:ss",
            "yyyy-MM-dd"
        )
        for (pattern in patterns) {
            try {
                val parser = SimpleDateFormat(pattern, Locale.US).apply {
                    if (pattern.contains("Z") || pattern.contains("X")) {
                        timeZone = TimeZone.getTimeZone("UTC")
                    }
                }
                val date = parser.parse(raw.trim())
                if (date != null) {
                    return SimpleDateFormat("MMM d, yyyy", Locale.US).format(date)
                }
            } catch (_: Exception) {
            }
        }
        return raw.take(10)
    }

    fun formatDisplayDateTime(raw: String?): String {
        if (raw.isNullOrBlank()) return "—"
        val patterns = listOf(
            "yyyy-MM-dd'T'HH:mm:ss.SSSSSSX",
            "yyyy-MM-dd'T'HH:mm:ss.SSSX",
            "yyyy-MM-dd'T'HH:mm:ssX",
            "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
            "yyyy-MM-dd'T'HH:mm:ss'Z'",
            "yyyy-MM-dd'T'HH:mm:ss"
        )
        for (pattern in patterns) {
            try {
                val parser = SimpleDateFormat(pattern, Locale.US).apply {
                    timeZone = TimeZone.getTimeZone("UTC")
                }
                val date = parser.parse(raw.trim())
                if (date != null) {
                    return SimpleDateFormat("MMM d, yyyy 'at' h:mm a", Locale.US).format(date)
                }
            } catch (_: Exception) {
            }
        }
        return formatDisplayDate(raw)
    }

    fun todayIsoDate(): String {
        return SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
    }

    fun daysFromNowIsoDate(days: Int = 14): String {
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, days)
        return SimpleDateFormat("yyyy-MM-dd", Locale.US).format(cal.time)
    }

    fun millisToIsoDate(millis: Long): String {
        val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
        return formatter.format(Date(millis))
    }

    fun nextQuoteNumber(existingQuotes: List<Quote>): String {
        val regex = Regex("QT-(\\d+)", RegexOption.IGNORE_CASE)
        val maxNum = existingQuotes.mapNotNull { quote ->
            regex.find(quote.quoteNumber)?.groupValues?.getOrNull(1)?.toIntOrNull()
        }.maxOrNull() ?: 1000
        return "QT-${maxNum + 1}"
    }

    fun buildMagicLink(quoteId: String): String {
        return "proquote://q/$quoteId"
    }

    fun buildWebPathPreview(quoteId: String): String {
        return "/q/$quoteId"
    }

    fun extractQuoteIdFromInput(input: String): String {
        val trimmed = input.trim()
        return when {
            trimmed.contains("/q/") -> trimmed.substringAfterLast("/q/").substringBefore("?").trim()
            trimmed.startsWith("proquote://q/") -> trimmed.removePrefix("proquote://q/").trim()
            else -> trimmed
        }
    }
}
