package com.example.subscription.repository

import com.example.subscription.models.SubscriptionHistoryEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface SubscriptionHistoryRepository : JpaRepository<SubscriptionHistoryEntity, Long> {

    fun findBySubscriptionIdOrderByChangedAtDesc(subscriptionId: Long): List<SubscriptionHistoryEntity>

    fun findBySubscriptionId(subscriptionId: Long): List<SubscriptionHistoryEntity>
}