package com.arttvad.worktime.domain.model

data class WorkProfile(
    val id: Long,
    val name: String,
    val hourlyRateMinor: Long? = null,
    val currencyCode: String? = null,
)
