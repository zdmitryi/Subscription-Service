package com.example.subscription.models

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "subscription_history")
class SubscriptionHistoryEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null

    @Column(name = "subscription_id", nullable = false)
    var subscriptionId: Long = 0

    @Enumerated(EnumType.STRING)
    @Column(name = "old_status", nullable = false)
    var oldStatus: SubscriptionStatus? = null

    @Enumerated(EnumType.STRING)
    @Column(name = "new_status", nullable = false)
    var newStatus: SubscriptionStatus = SubscriptionStatus.ACTIVE

    @Column(name = "changed_by", nullable = false)
    var changedBy: String = ""

    @Column(name = "change_reason")
    var changeReason: String? = null

    @Column(name = "changed_at", nullable = false)
    var changedAt: LocalDateTime = LocalDateTime.now()
}