package com.arttvad.worktime.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ProfilePaymentTest {
    @Test
    fun profileWithoutOverrideUsesLegacyFallback() {
        val payment = WorkProfile(1L, "Основная работа").effectivePayment(
            fallbackHourlyRateMinor = 1_500L,
            fallbackCurrencyCode = "EUR",
        )

        assertEquals(1_500L, payment.hourlyRateMinor)
        assertEquals("EUR", payment.currencyCode)
    }

    @Test
    fun profileOverrideCanIndependentlyClearRateAndChangeCurrency() {
        val payment = WorkProfile(
            id = 2L,
            name = "Подработка",
            hourlyRateMinor = null,
            currencyCode = "USD",
        ).effectivePayment(
            fallbackHourlyRateMinor = 1_500L,
            fallbackCurrencyCode = "EUR",
        )

        assertNull(payment.hourlyRateMinor)
        assertEquals("USD", payment.currencyCode)
    }
}
