package com.example.subscription.utility



import com.example.subscription.mappers.SubscriptionMapper
import com.example.subscription.models.Subscription
import com.example.subscription.models.SubscriptionStatus
import com.example.subscription.models.User
import com.example.subscription.repositories.SubscriptionRepository
import com.example.subscription.repositories.UserRepository
import jakarta.persistence.EntityNotFoundException
import jakarta.validation.Valid
import org.slf4j.LoggerFactory
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.validation.annotation.Validated
import java.time.LocalDate
import java.time.LocalDateTime

@Service
@Validated
class SubscriptionService(
    private val subscriptionRepository: SubscriptionRepository,
    private val userRepository: UserRepository,
    private val subscriptionMapper: SubscriptionMapper
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
        if (!isValidSubscription(subscriptionToCreate)) {
            throw IllegalArgumentException("End date must be after start date")
        }

        if (!userRepository.existsById(subscriptionToCreate.userId)) {
            throw EntityNotFoundException("User not found with id: ${subscriptionToCreate.userId}")
        }

        if (subscriptionToCreate.id != null) {
            throw IllegalArgumentException("ID should be empty")
        }

        if (subscriptionToCreate.status != SubscriptionStatus.ACTIVE) {
            throw IllegalArgumentException("Status should be empty, will be set to ACTIVE")
        }

        val subscriptionToSave = subscriptionToCreate.copy(
            status = SubscriptionStatus.ACTIVE
        )

        val entity = subscriptionMapper.toEntity(subscriptionToSave)
        val saved = subscriptionRepository.save(entity)

        log.info("New subscription created")
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

        if (!isValidSubscription(subscriptionToUpdate)) {
            throw IllegalArgumentException("End date must be after start date")
        }

        if (subscriptionToUpdate.status != existingSubscription.status) {
            throw IllegalArgumentException("Use specific endpoints to change status")
        }

        if (subscriptionToUpdate.id != null) {
            throw IllegalArgumentException("ID should be empty")
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


        if (subscription.status == SubscriptionStatus.CANCELLED) {
            throw IllegalStateException("Subscription is already cancelled")
        }

        if (subscription.status == SubscriptionStatus.EXPIRED) {
            throw IllegalStateException("Cannot cancel expired subscription")
        }

        entity.status = SubscriptionStatus.CANCELLED
        entity.canceledAt = LocalDateTime.now()
        entity.updatedAt = LocalDateTime.now()

        val updated = subscriptionRepository.save(entity)
        log.info("Subscription cancelled: $id")
        return subscriptionMapper.toDomain(updated)
    }

    fun suspendSubscription(id: Long, currentUser: User): Subscription {
        val entity = subscriptionRepository.findById(id)
            .orElseThrow { NoSuchElementException("Не существует элемента с таким id") }

        val subscription = subscriptionMapper.toDomain(entity)


        if (subscription.status != SubscriptionStatus.ACTIVE) {
            throw IllegalStateException("Only active subscriptions can be suspended")
        }

        entity.status = SubscriptionStatus.SUSPENDED
        entity.updatedAt = LocalDateTime.now()

        val updated = subscriptionRepository.save(entity)
        log.info("Subscription suspended: $id")
        return subscriptionMapper.toDomain(updated)
    }

    fun activateSubscription(id: Long, currentUser: User): Subscription {
        val entity = subscriptionRepository.findById(id)
            .orElseThrow { NoSuchElementException("Не существует элемента с таким id") }

        val subscription = subscriptionMapper.toDomain(entity)

        if (entity.endDate < LocalDate.now()) {
            throw IllegalStateException("Cannot activate expired subscription. Please renew.")
        }

        if (subscription.status != SubscriptionStatus.SUSPENDED) {
            throw IllegalStateException("Only suspended subscriptions can be activated")
        }

        entity.status = SubscriptionStatus.ACTIVE
        entity.updatedAt = LocalDateTime.now()

        val updated = subscriptionRepository.save(entity)
        log.info("Subscription activated: $id")
        return subscriptionMapper.toDomain(updated)
    }

    fun deleteSubscriptionById(id: Long, currentUser: User) {
        val entity = subscriptionRepository.findById(id)
            .orElseThrow { NoSuchElementException("Не существует элемента с таким id") }

        val subscription = subscriptionMapper.toDomain(entity)


        subscriptionRepository.deleteById(id)
        log.info("Subscription deleted: $id")
    }

    private fun isValidSubscription(subscription: Subscription): Boolean {
        return subscription.endDate.isAfter(subscription.startDate)
    }

    @Transactional
    fun expireSubscriptions(): Int {
        val now = LocalDate.now()
        val expiredSubscriptions = subscriptionRepository.findExpiredActiveSubscriptions(now)

        var count = 0
        expiredSubscriptions.forEach { entity ->
            if (entity.status == SubscriptionStatus.ACTIVE && entity.endDate < now) {
                entity.status = SubscriptionStatus.EXPIRED
                entity.updatedAt = LocalDateTime.now()
                subscriptionRepository.save(entity)
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

    @Transactional(readOnly = true)
    fun getSubscriptionsExpiringInDays(days: Int): List<Subscription> {
        val startDate = LocalDate.now()
        val endDate = startDate.plusDays(days.toLong())

        val entities = subscriptionRepository.findSubscriptionsExpiringBetween(startDate, endDate)
        return subscriptionMapper.toDomainList(entities)
    }

    data class SubscriptionStatistics(
        val totalSubscriptions: Long,
        val activeSubscriptions: Long,
        val suspendedSubscriptions: Long,
        val cancelledSubscriptions: Long,
        val expiredSubscriptions: Long,
        val totalRevenue: java.math.BigDecimal,
        val averageSubscriptionPrice: java.math.BigDecimal
    )
}