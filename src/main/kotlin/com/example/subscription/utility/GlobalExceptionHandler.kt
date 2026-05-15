package com.example.subscription.utility

import jakarta.persistence.EntityNotFoundException
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.*

import java.time.LocalDateTime
import java.util.NoSuchElementException


@ControllerAdvice
class GlobalExceptionHandler {
    private val log = LoggerFactory.getLogger(GlobalExceptionHandler::class.java)

    @ExceptionHandler(Exception::class)
    fun handleGenericException(e: Exception): ResponseEntity<ErrorResponseDto> {
        log.error("Handle exception", e)
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ErrorResponseDto(
            "Internal server error", e.message ?: "Unknown error", LocalDateTime.now()
        ))
    }

    @ExceptionHandler(EntityNotFoundException::class)
    fun handleEntityNotFoundException(e: EntityNotFoundException): ResponseEntity<ErrorResponseDto> {
        log.error("Handle entity not found exception", e)
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ErrorResponseDto(
            "Entity not found", e.message ?: "Entity not found", LocalDateTime.now()
        ))
    }

    @ExceptionHandler(exception = [
        IllegalArgumentException::class,
        IllegalStateException::class,
        MethodArgumentNotValidException::class
    ])
    fun handleBadRequestException(e: Exception): ResponseEntity<ErrorResponseDto> {
        log.error("Handle bad request exception", e)
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ErrorResponseDto(
            "Bad request", e.message ?: "Invalid request parameters", LocalDateTime.now()
        ))
    }

    @ExceptionHandler(NoSuchElementException::class)
    fun handleNoSuchElementException(e: NoSuchElementException): ResponseEntity<ErrorResponseDto> {
        log.error("Handle no such element exception", e)
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ErrorResponseDto(
            "No such element", e.message ?: "Element not found", LocalDateTime.now()
        ))
    }

    @ExceptionHandler(AccessDeniedException::class)
    fun handleAccessDeniedException(e: AccessDeniedException): ResponseEntity<ErrorResponseDto> {
        log.error("Handle access denied exception", e)
        return ResponseEntity.status(HttpStatus.NOT_ACCEPTABLE).body(ErrorResponseDto(
            "Permission denied", e.message ?: "Access denied", LocalDateTime.now()
        ))
    }

    data class ErrorResponseDto(
        val message: String,
        val detailedMessage: String,
        val errorTime: LocalDateTime
    )
}