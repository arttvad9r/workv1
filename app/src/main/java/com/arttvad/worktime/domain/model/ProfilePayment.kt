package com.arttvad.worktime.domain.model

data class EffectiveProfilePayment(
    val hourlyRateMinor: Long?,
    val currencyCode: String,
)

fun WorkProfile?.effectivePayment(
    fallbackHourlyRateMinor: Long?,
    fallbackCurrencyCode: String,
): EffectiveProfilePayment = if (this?.currencyCode != null) {
    EffectiveProfilePayment(
        hourlyRateMinor = hourlyRateMinor,
        currencyCode = currencyCode,
    )
} else {
    EffectiveProfilePayment(
        hourlyRateMinor = fallbackHourlyRateMinor,
        currencyCode = fallbackCurrencyCode,
    )
}
