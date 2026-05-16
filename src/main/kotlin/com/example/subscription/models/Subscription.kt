package com.example.subscription.models

import jakarta.validation.constraints.AssertTrue
import jakarta.validation.constraints.Future
import jakarta.validation.constraints.FutureOrPresent
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Positive
import jakarta.validation.constraints.PositiveOrZero
import java.math.BigDecimal
import java.time.LocalDate

data class Subscription(
    val id: Long? = null,

    @field:NotNull(message = "User ID is required")
    @field:Positive(message = "User ID must be positive")
    val userId: Long,

    @field:NotNull(message = "Service name is required")
    val serviceName: String,

    @field:NotNull(message = "Start date is required")
    @field:FutureOrPresent(message = "Start date must be today or in future")
    val startDate: LocalDate,

    @field:NotNull(message = "End date is required")
    @field:Future(message = "End date must be in future")
    val endDate: LocalDate,

    @field:NotNull(message = "Price is required")
    @PositiveOrZero
    val price: BigDecimal,

    val status: SubscriptionStatus,

) {
}

enum class SubscriptionStatus {
    ACTIVE,
    SUSPENDED,
    CANCELLED,
    EXPIRED
}