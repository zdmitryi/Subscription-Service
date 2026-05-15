package com.example.subscription.mappers

import com.example.subscription.models.SubscriptionEntity
import com.example.subscription.models.Subscription
import org.springframework.stereotype.Component

@Component
class SubscriptionMapper {

    fun toDomain(entity: SubscriptionEntity): Subscription {
        return Subscription(
            id = entity.id,
            userId = entity.userId,
            serviceName = entity.serviceName,
            startDate = entity.startDate,
            endDate = entity.endDate,
            price = entity.price,
            status = entity.status,
        )
    }

    fun toEntity(domain: Subscription): SubscriptionEntity {
        return SubscriptionEntity(
            userId = domain.userId,
            serviceName = domain.serviceName,
            startDate = domain.startDate,
            endDate = domain.endDate,
            price = domain.price
        ).apply {
            id = domain.id
            status = domain.status
        }
    }

    fun updateEntity(entity: SubscriptionEntity, domain: Subscription) {
        domain.userId.let { entity.userId = it }
        domain.serviceName.let { entity.serviceName = it }
        domain.startDate.let { entity.startDate = it }
        domain.endDate.let { entity.endDate = it }
        domain.price.let { entity.price = it }
        domain.status.let { entity.status = it }
        entity.updatedAt = java.time.LocalDateTime.now()
    }

    fun toDomainList(entities: List<SubscriptionEntity>): List<Subscription> {
        return entities.map { toDomain(it) }
    }
}