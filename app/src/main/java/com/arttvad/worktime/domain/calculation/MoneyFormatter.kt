package com.arttvad.worktime.domain.calculation

import java.math.BigDecimal
import java.math.RoundingMode
import java.text.NumberFormat
import java.util.Currency
import java.util.Locale

object MoneyFormatter {
    fun parseMajorToMinor(input: String, currencyCode: String): Long {
        val currency = Currency.getInstance(currencyCode.uppercase(Locale.ROOT))
        val fractionDigits = currency.defaultFractionDigits.coerceAtLeast(0)
        val normalized = input.trim().replace(" ", "").replace(',', '.')
        require(normalized.isNotEmpty()) { "Amount is empty" }

        val amount = BigDecimal(normalized)
        require(amount.signum() >= 0) { "Amount must be non-negative" }

        return amount
            .setScale(fractionDigits, RoundingMode.HALF_UP)
            .movePointRight(fractionDigits)
            .longValueExact()
    }

    fun formatRateInput(amountMinor: Long?, currencyCode: String): String {
        if (amountMinor == null) return ""
        val currency = Currency.getInstance(currencyCode.uppercase(Locale.ROOT))
        val fractionDigits = currency.defaultFractionDigits.coerceAtLeast(0)
        return BigDecimal.valueOf(amountMinor, fractionDigits)
            .stripTrailingZeros()
            .toPlainString()
    }

    fun formatCurrency(amountMinor: Long?, currencyCode: String, locale: Locale = Locale.getDefault()): String {
        if (amountMinor == null) return "—"
        val currency = Currency.getInstance(currencyCode.uppercase(Locale.ROOT))
        val fractionDigits = currency.defaultFractionDigits.coerceAtLeast(0)
        val formatter = NumberFormat.getCurrencyInstance(locale).apply { this.currency = currency }
        return formatter.format(BigDecimal.valueOf(amountMinor, fractionDigits))
    }
}
