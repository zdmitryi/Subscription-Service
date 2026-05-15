package com.example.subscription.controller

import com.example.subscription.models.User
import com.example.subscription.utility.UserService
import jakarta.validation.Valid
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/auth")
class UserController(
    private val userService: UserService
) {
    private val log = LoggerFactory.getLogger(UserController::class.java)

    @PostMapping("/register")
    fun register(
        @Valid @RequestBody user: User
    ): ResponseEntity<User> {
        log.info("Called register for user: ${user.username}")
        val registered = userService.register(user)
        return ResponseEntity.status(HttpStatus.CREATED).body(registered)
    }

    @GetMapping("/{id}")
    fun getUserById(
        @PathVariable("id") id: Long
    ): ResponseEntity<User> {
        log.info("Called getUserById: $id")
        val user = userService.getUserById(id)
        return ResponseEntity.ok(user)
    }

    @GetMapping("/username/{username}")
    fun getUserByUsername(
        @PathVariable("username") username: String
    ): ResponseEntity<User> {
        log.info("Called getUserByUsername: $username")
        val user = userService.getUserByUsername(username)
        return ResponseEntity.ok(user)
    }
}