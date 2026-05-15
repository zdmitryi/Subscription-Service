package com.example.subscription.models

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class User(
    val id: Long? = null,

    @field:NotBlank(message = "Username is required")
    @field:Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters")
    val username: String,

    @field:NotBlank(message = "Email is required")
    val email: String,

    @field:NotBlank(message = "Password is required")
    @field:Size(min = 8, message = "Password must be at least 8 characters")
    val password: String,

    val isActive: Boolean = true,
    val isAdmin: Boolean = false
)