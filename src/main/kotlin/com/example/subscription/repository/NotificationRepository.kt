package com.example.subscription.repository

import com.example.subscription.models.NotificationEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.LocalDateTime

interface NotificationRepository : JpaRepository<NotificationEntity, Long> {
    @Query("SELECT n FROM NotificationEntity n WHERE n.isSent = false AND n.scheduledAt <= :now")
    fun findPendingNotifications(@Param("now") now: LocalDateTime): List<NotificationEntity>
}