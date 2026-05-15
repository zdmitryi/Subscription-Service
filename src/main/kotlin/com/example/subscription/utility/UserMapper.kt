package com.example.subscription.mappers

import com.example.subscription.models.UserEntity
import com.example.subscription.models.User
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.stereotype.Component

@Component
class UserMapper(
    private val passwordEncoder: BCryptPasswordEncoder = BCryptPasswordEncoder()
) {

    fun toDomain(entity: UserEntity): User {
        return User(
            id = entity.id,
            username = entity.username,
            email = entity.email,
            password = entity.passwordHash,
            isActive = entity.isActive
        )
    }

    fun toEntity(domain: User): UserEntity {
        return UserEntity(
            username = domain.username,
            email = domain.email,
            passwordHash = passwordEncoder.encode(domain.password)
        ).apply {
            id = domain.id
            isActive = domain.isActive
        }
    }

    fun updateEntity(entity: UserEntity, domain: User) {
        domain.username.let { entity.username = it }
        domain.email.let { entity.email = it }
        if (domain.password.isNotBlank()) {
            entity.passwordHash = passwordEncoder.encode(domain.password)
        }
        domain.isActive.let { entity.isActive = it }
        entity.updatedAt = java.time.LocalDateTime.now()
    }
}