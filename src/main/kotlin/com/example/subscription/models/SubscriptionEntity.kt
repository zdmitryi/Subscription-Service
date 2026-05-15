package com.example.subscription.models

import jakarta.persistence.*
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

@Entity
@Table(name = "subscriptions")
class SubscriptionEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null

    @Column(name = "user_id", nullable = false)
    var userId: Long = 0

    @Column(name = "service_name", nullable = false)
    var serviceName: String = ""

    @Column(name = "start_date", nullable = false)
    var startDate: LocalDate = LocalDate.now()

    @Column(name = "end_date", nullable = false)
    var endDate: LocalDate = LocalDate.now()

    @Column(nullable = false, precision = 10, scale = 2)
    var price: BigDecimal = BigDecimal.ZERO

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var status: SubscriptionStatus = SubscriptionStatus.ACTIVE


    @Column(name = "canceled_at")
    var canceledAt: LocalDateTime? = null

    @Column(name = "created_at")
    var createdAt: LocalDateTime = LocalDateTime.now()

    @Column(name = "updated_at")
    var updatedAt: LocalDateTime = LocalDateTime.now()

    constructor()

    constructor(userId: Long, serviceName: String, startDate: LocalDate, endDate: LocalDate, price: BigDecimal) {
        this.userId = userId
        this.serviceName = serviceName
        this.startDate = startDate
        this.endDate = endDate
        this.price = price
    }
}