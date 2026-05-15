package com.example.subscription.utility

import com.example.subscription.models.User
import com.example.subscription.repository.UserRepository
import jakarta.persistence.EntityNotFoundException
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service

@Service
class UserService(
    private val userRepository: UserRepository,
    private val userMapper: UserMapper,
    private val passwordEncoder: PasswordEncoder
) : UserDetailsService {

    fun register(userToRegister: User): User {
        if (userRepository.existsByUsername(userToRegister.username)) {
            throw IllegalArgumentException("User with username ${userToRegister.username} already exists")
        }

        if (userRepository.existsByEmail(userToRegister.email)) {
            throw IllegalArgumentException("User with email ${userToRegister.email} already exists")
        }

        if (userToRegister.id != null) {
            throw IllegalArgumentException("ID should be empty for new user")
        }

        val entity = userMapper.toEntity(userToRegister)
        val saved = userRepository.save(entity)

        return userMapper.toDomain(saved)
    }

    fun getUserById(id: Long): User {
        val entity = userRepository.findById(id)
            .orElseThrow { EntityNotFoundException("User not found with id: $id") }
        return userMapper.toDomain(entity)
    }

    fun getUserByUsername(username: String): User {
        val entity = userRepository.findByUsername(username)
            .orElseThrow { EntityNotFoundException("User not found with username: $username") }
        return userMapper.toDomain(entity)
    }

    fun existsByUsername(username: String): Boolean {
        return userRepository.existsByUsername(username)
    }

    override fun loadUserByUsername(username: String): UserDetails {
        val entity = userRepository.findByUsername(username)
            .orElseThrow { NoSuchElementException("User not found with username: $username") }

        return org.springframework.security.core.userdetails.User
            .withUsername(entity.username)
            .password(entity.passwordHash)
            .authorities("USER")
            .build()
    }
}