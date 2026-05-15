package com.example.subscription.models

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "users")
class UserEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null

    @Column(unique = true, nullable = false)
    var username: String = ""

    @Column(unique = true, nullable = false)
    var email: String = ""

    @Column(nullable = false)
    var passwordHash: String = ""

    @Column(name = "is_active")
    var isActive: Boolean = true

    @Column(name = "created_at")
    var createdAt: LocalDateTime = LocalDateTime.now()

    @Column(name = "updated_at")
    var updatedAt: LocalDateTime = LocalDateTime.now()

    constructor()

    constructor(username: String, email: String, passwordHash: String) {
        this.username = username
        this.email = email
        this.passwordHash = passwordHash
    }
}