package com.example.subscription.utility

import com.example.subscription.mappers.SubscriptionMapper
import com.example.subscription.models.Subscription
import com.example.subscription.models.SubscriptionHistoryEntity
import com.example.subscription.models.SubscriptionStatus
import com.example.subscription.models.User
import com.example.subscription.repository.SubscriptionRepository
import com.example.subscription.repository.UserRepository
import com.example.subscription.repository.ServicePriceRepository
import com.example.subscription.repository.SubscriptionHistoryRepository
import com.opencsv.CSVWriter
import io.micrometer.core.instrument.MeterRegistry
import jakarta.annotation.PostConstruct
import jakarta.persistence.EntityNotFoundException
import jakarta.validation.Valid
import org.slf4j.LoggerFactory
import org.springframework.data.domain.PageRequest
import org.springframework.security.access.AccessDeniedException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.validation.annotation.Validated
import java.io.StringWriter
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

@Service
@Validated
class SubscriptionService(
    private val meterRegistry: MeterRegistry,
    private val subscriptionRepository: SubscriptionRepository,
    private val userRepository: UserRepository,
    private val subscriptionMapper: SubscriptionMapper,
    private val subscriptionHistoryRepository: SubscriptionHistoryRepository,
    private val servicePriceRepository: ServicePriceRepository
) {
    private val log = LoggerFactory.getLogger(SubscriptionService::class.java)

    data class SubscriptionFilter(
        val userId: Long?,
        val serviceName: String?,
        val status: SubscriptionStatus?,
        val startDateFrom: LocalDate?,
        val startDateTo: LocalDate?,
        val pageSize: Int?,
        val pageNumber: Int?
    )

    data class SubscriptionStatistics(
        val totalSubscriptions: Long,
        val activeSubscriptions: Long,
        val suspendedSubscriptions: Long,
        val cancelledSubscriptions: Long,
        val expiredSubscriptions: Long,
        val totalRevenue: BigDecimal,
        val averageSubscriptionPrice: BigDecimal
    )

    @PostConstruct
    fun initMetrics() {
        meterRegistry.gauge("subscriptions.active.count",
            subscriptionRepository.countByStatus(SubscriptionStatus.ACTIVE).toDouble()
        )

        meterRegistry.gauge("subscriptions.expired.count",
            subscriptionRepository.countByStatus(SubscriptionStatus.EXPIRED).toDouble()
        )

        meterRegistry.gauge("subscriptions.total.count",
            subscriptionRepository.count().toDouble()
        )
    }

    fun getSubscriptionById(id: Long): Subscription {
        val entity = subscriptionRepository.findById(id)
            .orElseThrow { IllegalArgumentException("Не существует subscription с таким id") }
        return subscriptionMapper.toDomain(entity)
    }

    fun getAllSubscriptions(filter: SubscriptionFilter): List<Subscription> {
        val pageSize = filter.pageSize ?: 10
        val pageNumber = filter.pageNumber ?: 0
        val pageable = PageRequest.of(pageNumber, pageSize)

        val entities = subscriptionRepository.findSubscriptionsByFilters(
            userId = filter.userId,
            serviceName = filter.serviceName,
            status = filter.status,
            startDateFrom = filter.startDateFrom,
            startDateTo = filter.startDateTo,
            pageable = pageable
        )

        return subscriptionMapper.toDomainList(entities)
    }

    fun getActiveSubscriptionsByUser(userId: Long): List<Subscription> {
        val entities = subscriptionRepository.findByUserIdAndStatus(userId, SubscriptionStatus.ACTIVE)
        return subscriptionMapper.toDomainList(entities)
    }

    fun createSubscription(
        @Valid subscriptionToCreate: Subscription,
        currentUser: User
    ): Subscription {
        // Проверяем существование пользователя
        if (!userRepository.existsById(subscriptionToCreate.userId)) {
            throw EntityNotFoundException("User not found with id: ${subscriptionToCreate.userId}")
        }

        if (subscriptionToCreate.id != null) {
            throw IllegalArgumentException("ID should be empty")
        }

        if (subscriptionToCreate.status != SubscriptionStatus.ACTIVE) {
            throw IllegalArgumentException("Status should be empty, will be set to ACTIVE")
        }

        val priceEntity = servicePriceRepository.findByServiceName(subscriptionToCreate.serviceName)
            ?: throw IllegalArgumentException("Service '${subscriptionToCreate.serviceName}' not found in catalog")

        val months = calculateMonths(subscriptionToCreate.price, priceEntity.monthlyPrice)

        val calculatedEndDate = subscriptionToCreate.startDate.plusMonths(months.toLong())

        if (subscriptionToCreate.endDate != calculatedEndDate) {
            throw IllegalArgumentException(
                "Invalid end date. For price ${subscriptionToCreate.price} " +
                        "(${priceEntity.monthlyPrice}/month) the end date should be $calculatedEndDate, " +
                        "but got ${subscriptionToCreate.endDate}"
            )
        }

        val subscriptionToSave = subscriptionToCreate.copy(
            status = SubscriptionStatus.ACTIVE
        )

        val entity = subscriptionMapper.toEntity(subscriptionToSave)
        val saved = subscriptionRepository.save(entity)

        meterRegistry.counter("subscriptions.created",
            "service", subscriptionToCreate.serviceName,
            "months", months.toString()
        ).increment()

        saveHistory(saved.id!!, null, SubscriptionStatus.ACTIVE, currentUser.username,
            "Created for $months months. Paid: ${subscriptionToCreate.price}")

        log.info("New subscription created for $months months")
        return subscriptionMapper.toDomain(saved)
    }

    fun updateSubscription(
        id: Long,
        @Valid subscriptionToUpdate: Subscription,
        currentUser: User
    ): Subscription {
        val existingEntity = subscriptionRepository.findById(id)
            .orElseThrow { EntityNotFoundException("Не существует subscription с данным id") }

        val existingSubscription = subscriptionMapper.toDomain(existingEntity)

        if (subscriptionToUpdate.status != existingSubscription.status) {
            throw IllegalArgumentException("Use specific endpoints to change status")
        }

        if (subscriptionToUpdate.id != null) {
            throw IllegalArgumentException("ID should be empty")
        }

        val priceEntity = servicePriceRepository.findByServiceName(subscriptionToUpdate.serviceName)
            ?: throw IllegalArgumentException("Service '${subscriptionToUpdate.serviceName}' not found in catalog")

        val months = calculateMonths(subscriptionToUpdate.price, priceEntity.monthlyPrice)
        val calculatedEndDate = subscriptionToUpdate.startDate.plusMonths(months.toLong())

        if (subscriptionToUpdate.endDate != calculatedEndDate) {
            throw IllegalArgumentException(
                "Invalid end date. For price ${subscriptionToUpdate.price} " +
                        "(${priceEntity.monthlyPrice}/month) the end date should be $calculatedEndDate"
            )
        }

        subscriptionMapper.updateEntity(existingEntity, subscriptionToUpdate)
        val updated = subscriptionRepository.save(existingEntity)

        log.info("Subscription updated: $id")
        return subscriptionMapper.toDomain(updated)
    }

    fun cancelSubscription(id: Long, currentUser: User): Subscription {
        val entity = subscriptionRepository.findById(id)
            .orElseThrow { NoSuchElementException("Не существует элемента с таким id") }

        val subscription = subscriptionMapper.toDomain(entity)
        val oldStatus = subscription.status

        validateStatusTransition(subscription.status, SubscriptionStatus.CANCELLED, entity.endDate)

        entity.status = SubscriptionStatus.CANCELLED
        entity.canceledAt = LocalDateTime.now()
        entity.updatedAt = LocalDateTime.now()

        val updated = subscriptionRepository.save(entity)

        meterRegistry.counter("subscriptions.cancelled").increment()

        log.info("Subscription cancelled: $id")
        saveHistory(entity.id!!, oldStatus, SubscriptionStatus.CANCELLED, currentUser.username, "User cancelled subscription")
        return subscriptionMapper.toDomain(updated)
    }

    fun suspendSubscription(id: Long, currentUser: User): Subscription {
        val entity = subscriptionRepository.findById(id)
            .orElseThrow { NoSuchElementException("Не существует элемента с таким id") }

        val subscription = subscriptionMapper.toDomain(entity)
        val oldStatus = subscription.status

        validateStatusTransition(subscription.status, SubscriptionStatus.SUSPENDED, entity.endDate)

        entity.status = SubscriptionStatus.SUSPENDED
        entity.updatedAt = LocalDateTime.now()

        val updated = subscriptionRepository.save(entity)

        meterRegistry.counter("subscriptions.suspended").increment()

        log.info("Subscription suspended: $id")
        saveHistory(entity.id!!, oldStatus, SubscriptionStatus.SUSPENDED, currentUser.username, "User suspended subscription")
        return subscriptionMapper.toDomain(updated)
    }

    fun activateSubscription(id: Long, currentUser: User): Subscription {
        val entity = subscriptionRepository.findById(id)
            .orElseThrow { NoSuchElementException("Не существует элемента с таким id") }

        val subscription = subscriptionMapper.toDomain(entity)
        val oldStatus = subscription.status

        validateStatusTransition(subscription.status, SubscriptionStatus.ACTIVE, entity.endDate)

        entity.status = SubscriptionStatus.ACTIVE
        entity.updatedAt = LocalDateTime.now()

        val updated = subscriptionRepository.save(entity)

        meterRegistry.counter("subscriptions.activated").increment()

        log.info("Subscription activated: $id")
        saveHistory(entity.id!!, oldStatus, SubscriptionStatus.ACTIVE, currentUser.username, "User activated subscription")
        return subscriptionMapper.toDomain(updated)
    }

    fun deleteSubscriptionById(id: Long, currentUser: User) {
        val entity = subscriptionRepository.findById(id)
            .orElseThrow { NoSuchElementException("Не существует элемента с таким id") }

        subscriptionRepository.deleteById(id)
        log.info("Subscription deleted: $id")
    }

    @Transactional
    fun expireSubscriptions(): Int {
        val now = LocalDate.now()
        val expiredSubscriptions = subscriptionRepository.findExpiredActiveSubscriptions(now)

        var count = 0
        expiredSubscriptions.forEach { entity ->
            if (entity.status == SubscriptionStatus.ACTIVE && entity.endDate < now) {
                val oldStatus = entity.status
                entity.status = SubscriptionStatus.EXPIRED
                entity.updatedAt = LocalDateTime.now()
                subscriptionRepository.save(entity)
                saveHistory(entity.id!!, oldStatus, SubscriptionStatus.EXPIRED, "SYSTEM", "Auto-expired by scheduler")
                meterRegistry.counter("subscriptions.expired").increment()
                count++
                log.debug("Subscription ${entity.id} expired")
            }
        }

        return count
    }

    @Transactional(readOnly = true)
    fun getStatistics(): SubscriptionStatistics {
        val total = subscriptionRepository.count()
        val active = subscriptionRepository.countByStatus(SubscriptionStatus.ACTIVE)
        val suspended = subscriptionRepository.countByStatus(SubscriptionStatus.SUSPENDED)
        val cancelled = subscriptionRepository.countByStatus(SubscriptionStatus.CANCELLED)
        val expired = subscriptionRepository.countByStatus(SubscriptionStatus.EXPIRED)

        val activeSubscriptions = subscriptionRepository.findByStatus(SubscriptionStatus.ACTIVE)
        val suspendedSubscriptions = subscriptionRepository.findByStatus(SubscriptionStatus.SUSPENDED)
        val allActiveSubscriptions = activeSubscriptions + suspendedSubscriptions

        val totalRevenue = allActiveSubscriptions.sumOf { it.price }
        val avgPrice = if (total > 0) totalRevenue / java.math.BigDecimal(total) else java.math.BigDecimal.ZERO

        return SubscriptionStatistics(
            totalSubscriptions = total,
            activeSubscriptions = active,
            suspendedSubscriptions = suspended,
            cancelledSubscriptions = cancelled,
            expiredSubscriptions = expired,
            totalRevenue = totalRevenue,
            averageSubscriptionPrice = avgPrice
        )
    }

    private fun validateStatusTransition(oldStatus: SubscriptionStatus, newStatus: SubscriptionStatus, endDate: LocalDate) {
        when {
            newStatus == SubscriptionStatus.ACTIVE && oldStatus == SubscriptionStatus.EXPIRED && endDate < LocalDate.now() ->
                throw IllegalStateException("Cannot activate expired subscription without renewal")

            oldStatus == SubscriptionStatus.CANCELLED && newStatus != oldStatus ->
                throw IllegalStateException("Cannot change cancelled subscription")

            oldStatus == SubscriptionStatus.CANCELLED && newStatus == SubscriptionStatus.CANCELLED ->
                throw IllegalStateException("Subscription is already cancelled")

            oldStatus == SubscriptionStatus.SUSPENDED && newStatus == SubscriptionStatus.SUSPENDED ->
                throw IllegalStateException("Subscription is already suspended")

            oldStatus == SubscriptionStatus.EXPIRED && newStatus == SubscriptionStatus.CANCELLED ->
                throw IllegalStateException("Cannot cancel expired subscription")
        }
    }

    @Transactional(readOnly = true)
    fun getSubscriptionsExpiringInDays(days: Int): List<Subscription> {
        val startDate = LocalDate.now()
        val endDate = startDate.plusDays(days.toLong())

        val entities = subscriptionRepository.findSubscriptionsExpiringBetween(startDate, endDate)
        return subscriptionMapper.toDomainList(entities)
    }

    @Transactional(readOnly = true)
    fun getSubscriptionHistory(subscriptionId: Long, currentUser: User): List<SubscriptionHistoryEntity> {
        val subscription = getSubscriptionById(subscriptionId)

        if (subscription.userId != currentUser.id) {
            throw AccessDeniedException("No access to history of this subscription")
        }

        return subscriptionHistoryRepository.findBySubscriptionIdOrderByChangedAtDesc(subscriptionId)
    }

    @Transactional
    fun renewSubscription(id: Long, currentUser: User, paidPrice: BigDecimal): Subscription {
        val entity = subscriptionRepository.findById(id)
            .orElseThrow { NoSuchElementException("Не существует элемента с таким id") }

        if (entity.status != SubscriptionStatus.EXPIRED) {
            throw IllegalStateException("Only expired subscriptions can be renewed")
        }
        val priceEntity = servicePriceRepository.findByServiceName(entity.serviceName)
            ?: throw IllegalArgumentException("Price not found for service: ${entity.serviceName}")
        val months = calculateMonths(paidPrice, priceEntity.monthlyPrice)
        val newEndDate = LocalDate.now().plusMonths(months.toLong())

        log.info("Renewing subscription $id for $months months. Paid: $paidPrice (${priceEntity.monthlyPrice}/month)")

        entity.endDate = newEndDate
        entity.price = paidPrice
        entity.status = SubscriptionStatus.ACTIVE
        entity.updatedAt = LocalDateTime.now()

        val updated = subscriptionRepository.save(entity)

        meterRegistry.counter("subscriptions.renewed",
            "service", entity.serviceName,
            "months", months.toString()
        ).increment()

        saveHistory(entity.id!!, SubscriptionStatus.EXPIRED, SubscriptionStatus.ACTIVE,
            currentUser.username, "Renewed for $months months. Paid: $paidPrice")

        log.info("Subscription renewed: $id, new end date: $newEndDate")
        return subscriptionMapper.toDomain(updated)
    }

    fun exportToCsv(filter: SubscriptionFilter): ByteArray {
        val subscriptions = getAllSubscriptions(filter)

        val writer = StringWriter()
        val csvWriter = CSVWriter(writer)

        csvWriter.writeNext(arrayOf(
            "ID",
            "User ID",
            "Service Name",
            "Start Date",
            "End Date",
            "Price",
            "Status",
        ))

        subscriptions.forEach { sub ->
            csvWriter.writeNext(arrayOf(
                sub.id?.toString() ?: "",
                sub.userId.toString(),
                sub.serviceName,
                sub.startDate.toString(),
                sub.endDate.toString(),
                sub.price.toString(),
                sub.status.name,
            ))
        }

        csvWriter.close()
        return writer.toString().toByteArray(Charsets.UTF_8)
    }

    private fun calculateMonths(paidPrice: BigDecimal, monthlyPrice: BigDecimal): Int {
        val months = paidPrice.divide(monthlyPrice, 10, java.math.RoundingMode.HALF_UP)
        if (months.stripTrailingZeros().scale() > 0) {
            throw IllegalArgumentException(
                "Paid price $paidPrice is not a multiple of monthly price $monthlyPrice. " +
                        "It should be ${monthlyPrice} * N where N is integer"
            )
        }

        val monthsInt = months.toInt()
        if (monthsInt <= 0) {
            throw IllegalArgumentException("Paid price must be at least monthly price $monthlyPrice")
        }

        return monthsInt
    }

    private fun saveHistory(
        subscriptionId: Long,
        oldStatus: SubscriptionStatus?,
        newStatus: SubscriptionStatus,
        changedBy: String,
        reason: String? = null
    ) {
        val history = SubscriptionHistoryEntity().apply {
            this.subscriptionId = subscriptionId
            this.oldStatus = oldStatus
            this.newStatus = newStatus
            this.changedBy = changedBy
            this.changeReason = reason
            this.changedAt = LocalDateTime.now()
        }
        subscriptionHistoryRepository.save(history)
    }
}