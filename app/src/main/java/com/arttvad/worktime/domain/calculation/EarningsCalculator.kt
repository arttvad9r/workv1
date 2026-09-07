package com.arttvad.worktime.domain.calculation

object EarningsCalculator {
    /**
     * Calculates earnings in currency minor units using half-up rounding.
     * Both inputs must be non-negative. No overtime multiplier is applied implicitly.
     */
    fun calculateMinor(workedMinutes: Int, hourlyRateMinor: Long): Long {
        require(workedMinutes >= 0) { "workedMinutes must be non-negative" }
        require(hourlyRateMinor >= 0) { "hourlyRateMinor must be non-negative" }

        val product = Math.multiplyExact(workedMinutes.toLong(), hourlyRateMinor)
        return Math.floorDiv(Math.addExact(product, 30L), 60L)
    }
}
