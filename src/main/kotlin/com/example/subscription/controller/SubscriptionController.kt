package com.example.subscription.controller

import com.example.subscription.models.Subscription
import com.example.subscription.models.SubscriptionStatus
import com.example.subscription.models.User
import com.example.subscription.utility.SubscriptionService
import io.swagger.v3.oas.annotations.Operation
import jakarta.validation.Valid
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@RestController
@RequestMapping("/subscription")
class SubscriptionController(
    private val subscriptionService: SubscriptionService
) {
    private val log = LoggerFactory.getLogger(SubscriptionController::class.java)

    @GetMapping("/{id}")
    fun getSubscriptionById(
        @PathVariable("id") id: Long
    ): ResponseEntity<Subscription> {
        log.info("Called getSubscriptionById: $id")
        return ResponseEntity.ok(subscriptionService.getSubscriptionById(id))
    }

    @GetMapping
    fun getAllSubscriptions(
        @RequestParam(name = "userId", required = false) userId: Long?,
        @RequestParam(name = "serviceName", required = false) serviceName: String?,
        @RequestParam(name = "status", required = false) status: SubscriptionStatus?,
        @RequestParam(name = "startDateFrom", required = false) startDateFrom: LocalDate?,
        @RequestParam(name = "startDateTo", required = false) startDateTo: LocalDate?,
        @RequestParam(name = "pageSize", required = false) pageSize: Int?,
        @RequestParam(name = "pageNumber", required = false) pageNumber: Int?
    ): ResponseEntity<List<Subscription>> {
        log.info("Called getAllSubscriptions with filters")

        val filter = SubscriptionService.SubscriptionFilter(
            userId = userId,
            serviceName = serviceName,
            status = status,
            startDateFrom = startDateFrom,
            startDateTo = startDateTo,
            pageSize = pageSize,
            pageNumber = pageNumber
        )

        val subscriptions = subscriptionService.getAllSubscriptions(filter)
        return ResponseEntity.ok(subscriptions)
    }

    @GetMapping("/user/active")
    fun getMyActiveSubscriptions(
        user: User
    ): ResponseEntity<List<Subscription>> {
        log.info("Called getMyActiveSubscriptions for user: ${user.id}")
        val subscriptions = subscriptionService.getActiveSubscriptionsByUser(user.id!!)
        return ResponseEntity.ok(subscriptions)
    }

    @PostMapping
    fun createSubscription(
        @Valid @RequestBody subscription: Subscription,
        user: User
    ): ResponseEntity<Subscription> {
        log.info("Called createSubscription for user: ${user.username}")
        val created = subscriptionService.createSubscription(subscription, user)
        return ResponseEntity.status(HttpStatus.CREATED).body(created)
    }

    @PutMapping("/{id}")
    fun updateSubscription(
        @PathVariable("id") id: Long,
        @Valid @RequestBody subscription: Subscription,
        user: User
    ): ResponseEntity<Subscription> {
        log.info("Called updateSubscription with id: $id")
        val updated = subscriptionService.updateSubscription(id, subscription, user)
        return ResponseEntity.ok(updated)
    }

    @PostMapping("/{id}/cancel")
    fun cancelSubscription(
        @PathVariable("id") id: Long,
        user: User
    ): ResponseEntity<Subscription> {
        log.info("Called cancelSubscription with id: $id")
        val cancelled = subscriptionService.cancelSubscription(id, user)
        return ResponseEntity.ok(cancelled)
    }

    @PostMapping("/{id}/suspend")
    fun suspendSubscription(
        @PathVariable("id") id: Long,
        user: User
    ): ResponseEntity<Subscription> {
        log.info("Called suspendSubscription with id: $id")
        val suspended = subscriptionService.suspendSubscription(id, user)
        return ResponseEntity.ok(suspended)
    }

    @PostMapping("/{id}/activate")
    fun activateSubscription(
        @PathVariable("id") id: Long,
        user: User
    ): ResponseEntity<Subscription> {
        log.info("Called activateSubscription with id: $id")
        val activated = subscriptionService.activateSubscription(id, user)
        return ResponseEntity.ok(activated)
    }

    @PostMapping("/{id}/renew")
    fun renewSubscription(
        @PathVariable("id") id: Long,
        user: User
    ): ResponseEntity<Subscription> {
        log.info("Called renewSubscription with id: $id")
        val renewed = subscriptionService.renewSubscription(id, user)
        return ResponseEntity.ok(renewed)
    }


    @DeleteMapping("/{id}")
    fun deleteSubscription(
        @PathVariable("id") id: Long,
        user: User
    ): ResponseEntity<Void> {
        log.info("Called deleteSubscription with id: $id")
        subscriptionService.deleteSubscriptionById(id, user)
        return ResponseEntity.noContent().build()
    }

    @GetMapping("/{id}/history")
    fun getSubscriptionHistory(
        @PathVariable("id") id: Long,
        user: User
    ): ResponseEntity<List<SubscriptionHistoryResponse>?> {
        val history = subscriptionService.getSubscriptionHistory(id, user)
        val response = history.map { entity ->
            SubscriptionHistoryResponse(
                id = entity.id!!,
                subscriptionId = entity.subscriptionId,
                oldStatus = entity.oldStatus,
                newStatus = entity.newStatus,
                changedBy = entity.changedBy,
                changeReason = entity.changeReason,
                changedAt = entity.changedAt
            )
        }

        return ResponseEntity.ok(response)
    }
    data class SubscriptionHistoryResponse(
        val id: Long,
        val subscriptionId: Long,
        val oldStatus: SubscriptionStatus?,
        val newStatus: SubscriptionStatus,
        val changedBy: String,
        val changeReason: String?,
        val changedAt: LocalDateTime
    )


    @GetMapping("/export/csv")
    fun exportSubscriptionsToCsv(
        @RequestParam(name = "userId", required = false) userId: Long?,
        @RequestParam(name = "serviceName", required = false) serviceName: String?,
        @RequestParam(name = "status", required = false) status: SubscriptionStatus?,
        @RequestParam(name = "startDateFrom", required = false) startDateFrom: LocalDate?,
        @RequestParam(name = "startDateTo", required = false) startDateTo: LocalDate?
    ): ResponseEntity<ByteArray> {
        log.info("Exporting subscriptions to CSV")

        val filter = SubscriptionService.SubscriptionFilter(
            userId = userId,
            serviceName = serviceName,
            status = status,
            startDateFrom = startDateFrom,
            startDateTo = startDateTo,
            pageSize = null,
            pageNumber = null
        )

        val csvData = subscriptionService.exportToCsv(filter)
        val timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"))
        val fileName = buildString {
            append("subscriptions")
            if (userId != null) {
                append("_user_$userId")
            }
            if (status != null) {
                append("_${status.name.lowercase()}")
            }
            if (serviceName != null && serviceName.isNotBlank()) {
                append("_${serviceName.lowercase()}")
            }
            append("_$timestamp.csv")
        }
        return ResponseEntity.ok()
            .header("Content-Type", "text/csv")
            .header("Content-Disposition", "attachment; filename=\"$fileName\"")
            .body(csvData)
    }
}