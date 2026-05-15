package com.example.subscription.utility

import com.example.subscription.models.NotificationEntity
import com.example.subscription.repository.NotificationRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.LocalDateTime

@Service
class NotificationService(
    private val notificationRepository: NotificationRepository
) {
    private val log = LoggerFactory.getLogger(NotificationService::class.java)

    fun scheduleNotification(subscriptionId: Long, daysBeforeExpiration: Int) {
        val scheduledAt = LocalDateTime.now().plusDays(daysBeforeExpiration.toLong())
        val notification = NotificationEntity().apply {
            this.subscriptionId = subscriptionId
            this.scheduledAt = scheduledAt
            this.isSent = false
        }
        notificationRepository.save(notification)
    }

    fun processPendingNotifications() {
        val pending = notificationRepository.findPendingNotifications(LocalDateTime.now())
        pending.forEach { notification ->
            log.info("Reminder: Subscription ${notification.subscriptionId} expires soon!")
            notification.isSent = true
            notificationRepository.save(notification)
        }
    }

}