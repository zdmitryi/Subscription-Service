package com.example.subscription.repository

import com.example.subscription.models.ServicePriceEntity
import org.springframework.data.jpa.repository.JpaRepository

interface ServicePriceRepository : JpaRepository<ServicePriceEntity, Long> {
    fun findByServiceName(serviceName : String): ServicePriceEntity?
}