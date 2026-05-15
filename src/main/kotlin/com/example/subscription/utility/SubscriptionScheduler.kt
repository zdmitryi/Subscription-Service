package com.example.subscription.utility

import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
class SubscriptionScheduler(
    private val subscriptionService: SubscriptionService
) {
    private val log = LoggerFactory.getLogger(SubscriptionScheduler::class.java)

    @Scheduled(cron = "*/30 * * * * *")
    fun expireOldSubscriptions() {
        log.info("expireOldSubscriptions")
        try {
            val expiredCount = subscriptionService.expireSubscriptions()
            if (expiredCount > 0) {
                log.info("Expired $expiredCount subscriptions")
            } else {
                log.info("ℹNo expired subscriptions found")
            }
        } catch (e: Exception) {
            log.error("Error while expiring subscriptions", e)
        }
    }

    @Scheduled(cron = "*/30 * * * * *")
    fun logSubscriptionStatistics() {
        log.info("=== RUNNING: logSubscriptionStatistics ===")
        try {
            val stats = subscriptionService.getStatistics()
            log.info("""
                Subscription Statistics:
                - Total: ${stats.totalSubscriptions}
                - Active: ${stats.activeSubscriptions}
                - Suspended: ${stats.suspendedSubscriptions}
                - Cancelled: ${stats.cancelledSubscriptions}
                - Expired: ${stats.expiredSubscriptions}
                - Revenue: ${stats.totalRevenue}
            """.trimIndent())
        } catch (e: Exception) {
            log.error("Error while logging statistics", e)
        }
    }
    @Scheduled(cron = "*/30 * * * * *")
    fun checkExpiringSoonSubscriptions() {
        log.info("checkExpiringSoonSubscriptions")
        try {
            val expiringSubscriptions = subscriptionService.getSubscriptionsExpiringInDays(3)
            if (expiringSubscriptions.isNotEmpty()) {
                log.info("⚠️ Found ${expiringSubscriptions.size} subscriptions expiring in 3 days")
                expiringSubscriptions.forEach { subscription ->
                    log.info("   - Subscription ${subscription.id} for user ${subscription.userId} expires on ${subscription.endDate}")
                }
            } else {
                log.info("No subscriptions expiring in 3 days")
            }
        } catch (e: Exception) {
            log.error("Error while checking expiring subscriptions", e)
        }
    }
}