package com.example.subscription.models

import jakarta.persistence.*
import java.math.BigDecimal


@Entity
@Table(name = "service_prices")
class ServicePriceEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null

    @Column(name = "service_name", unique = true)
    var serviceName: String = ""

    @Column(name = "monthly_price")
    var monthlyPrice: BigDecimal = BigDecimal.valueOf(0.00);
}