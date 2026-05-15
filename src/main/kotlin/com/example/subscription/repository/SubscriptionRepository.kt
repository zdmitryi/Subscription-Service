package com.example.subscription.repository

import com.example.subscription.models.SubscriptionEntity
import com.example.subscription.models.SubscriptionStatus
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.LocalDate

interface SubscriptionRepository : JpaRepository<SubscriptionEntity, Long> {

    fun findByUserId(userId: Long): List<SubscriptionEntity>

    fun findByUserIdAndStatus(userId: Long, status: SubscriptionStatus): List<SubscriptionEntity>

    fun findByStatus(status: SubscriptionStatus): List<SubscriptionEntity>

    @Query("""
    SELECT s FROM SubscriptionEntity s 
    WHERE (:userId IS NULL OR s.userId = :userId)
    AND (:serviceName IS NULL OR s.serviceName = :serviceName)
    AND (:status IS NULL OR s.status = :status)
    AND (:startDateFrom IS NULL OR s.startDate >= :startDateFrom)
    AND (:startDateTo IS NULL OR s.startDate <= :startDateTo)
""")
    fun findSubscriptionsByFilters(
        @Param("userId") userId: Long?,
        @Param("serviceName") serviceName: String?,
        @Param("status") status: SubscriptionStatus?,
        @Param("startDateFrom") startDateFrom: LocalDate?,
        @Param("startDateTo") startDateTo: LocalDate?,
        pageable: Pageable
    ): List<SubscriptionEntity>

    @Query("SELECT s FROM SubscriptionEntity s WHERE s.endDate < :currentDate AND s.status = 'ACTIVE'")
    fun findExpiredActiveSubscriptions(@Param("currentDate") currentDate: LocalDate): List<SubscriptionEntity>

    @Query("SELECT s FROM SubscriptionEntity s WHERE s.endDate BETWEEN :startDate AND :endDate AND s.status = 'ACTIVE'")
    fun findSubscriptionsExpiringBetween(
        @Param("startDate") startDate: LocalDate,
        @Param("endDate") endDate: LocalDate
    ): List<SubscriptionEntity>

    fun countByStatus(status: SubscriptionStatus): Long
}