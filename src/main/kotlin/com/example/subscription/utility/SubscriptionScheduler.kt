package com.example.subscription.utility

import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
class SubscriptionScheduler(
    private val subscriptionService: SubscriptionService,
    private val notificationService: NotificationService  // ← Добавить
) {
    private val log = LoggerFactory.getLogger(SubscriptionScheduler::class.java)

    @Scheduled(cron = "0 0 1 * * *")
    fun expireOldSubscriptions() {
        log.info("expireOldSubscriptions:")
        try {
            val expiredCount = subscriptionService.expireSubscriptions()
            if (expiredCount > 0) {
                log.info("Expired $expiredCount subscriptions")
            } else {
                log.info("No expired subscriptions found")
            }
        } catch (e: Exception) {
            log.error("Error while expiring subscriptions", e)
        }
    }

    @Scheduled(cron = "0 0 1 * * *")
    fun logSubscriptionStatistics() {
        log.info("logSubscriptionStatistics:")
        try {
            val stats = subscriptionService.getStatistics()
            log.info("""
                Subscription Statistics:
                - Total: ${stats.totalSubscriptions}
                - Active: ${stats.activeSubscriptions}
                - Suspended: ${stats.suspendedSubscriptions}
                - Cancelled: ${stats.cancelledSubscriptions}
                - Expired: ${stats.expiredSubscriptions}
            """.trimIndent())
        } catch (e: Exception) {
            log.error("Error while logging statistics", e)
        }
    }

    @Scheduled(cron = "0 0 1 * * *")
    fun checkExpiringSoonSubscriptions() {
        log.info("checkExpiringSoonSubscriptions:")
        try {
            val expiringSubscriptions = subscriptionService.getSubscriptionsExpiringInDays(3)

            if (expiringSubscriptions.isNotEmpty()) {
                log.info("Found ${expiringSubscriptions.size} subscriptions expiring in 3 days")

                expiringSubscriptions.forEach { subscription ->
                    log.info("   -Subscription ${subscription.id} for user ${subscription.userId} expires on ${subscription.endDate}")

                    notificationService.scheduleNotification(subscription.id!!, 3)
                }
            } else {
                log.info("No subscriptions expiring in 3 days")
            }
        } catch (e: Exception) {
            log.error("Error while checking expiring subscriptions", e)
        }
    }

    @Scheduled(cron = "0 */30 * * * *")
    fun processPendingNotifications() {
        log.info("processPendingNotifications:")
        try {
            notificationService.processPendingNotifications()
        } catch (e: Exception) {
            log.error("Error while processing pending notifications", e)
        }
    }
}