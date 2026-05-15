package com.example.subscription.models

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "notifications")
class NotificationEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null

    @Column(name = "subscription_id", nullable = false)
    var subscriptionId: Long = 0

    @Column(name = "scheduled_at", nullable = false)
    var scheduledAt: LocalDateTime = LocalDateTime.now()

    @Column(name = "is_sent")
    var isSent: Boolean = false
}