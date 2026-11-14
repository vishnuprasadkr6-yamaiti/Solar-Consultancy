package com.example.util

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object Formatters {
    private val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.ENGLISH)
    private val dateTimeFormat = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.ENGLISH)
    private val inputDateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH)

    fun formatDate(timestamp: Long): String = dateFormat.format(Date(timestamp))

    fun formatDateTime(timestamp: Long): String = dateTimeFormat.format(Date(timestamp))

    fun formatEditableDate(timestamp: Long?): String =
        if (timestamp == null) "" else inputDateFormat.format(Date(timestamp))

    fun parseEditableDate(value: String): Long? {
        val trimmed = value.trim()
        if (trimmed.isEmpty()) return null
        return try {
            inputDateFormat.isLenient = false
            inputDateFormat.parse(trimmed)?.time
        } catch (_: Exception) {
            null
        }
    }

    fun formatCapacityKw(kw: Double): String {
        val clean = if (kw % 1.0 == 0.0) {
            kw.toLong().toString()
        } else {
            String.format(Locale.ENGLISH, "%.2f", kw).trimEnd('0').trimEnd('.')
        }
        return "$clean kW"
    }

    fun formatCurrency(amount: Double, symbol: String = "₹", currencyCode: String = "INR"): String {
        val negative = amount < 0
        val absValue = kotlin.math.abs(amount)
        val formattedNumber = if (currencyCode.equals("INR", ignoreCase = true)) {
            formatIndianNumber(absValue)
        } else {
            val fmt = DecimalFormat("#,##0.##", DecimalFormatSymbols(Locale.ENGLISH))
            fmt.format(absValue)
        }
        return if (negative) "-$symbol$formattedNumber" else "$symbol$formattedNumber"
    }

    private fun formatIndianNumber(value: Double): String {
        val whole = value.toLong()
        val frac = value - whole
        val str = whole.toString()
        val formattedWhole = if (str.length <= 3) {
            str
        } else {
            val lastThree = str.takeLast(3)
            val rest = str.dropLast(3)
            val grouped = rest.reversed().chunked(2).joinToString(",").reversed()
            "$grouped,$lastThree"
        }
        return if (frac >= 0.005) {
            val decimals = String.format(Locale.ENGLISH, "%.2f", frac).substring(1)
            "$formattedWhole$decimals"
        } else {
            formattedWhole
        }
    }

    fun formatBytes(bytes: Long): String {
        if (bytes < 1024) return "$bytes B"
        val kb = bytes / 1024.0
        if (kb < 1024) return String.format(Locale.ENGLISH, "%.1f KB", kb)
        val mb = kb / 1024.0
        return String.format(Locale.ENGLISH, "%.2f MB", mb)
    }

    fun startOfCurrentMonthMillis(): Long {
        val cal = Calendar.getInstance()
        cal.set(Calendar.DAY_OF_MONTH, 1)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }
}
