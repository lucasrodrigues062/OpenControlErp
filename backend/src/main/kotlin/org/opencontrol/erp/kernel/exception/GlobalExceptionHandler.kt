package org.opencontrol.erp.kernel.exception

import org.springframework.http.HttpStatus
import org.springframework.http.ProblemDetail
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import java.net.URI
import java.time.Instant

open class OpenControlException(
    override val message: String,
    val errorCode: String,
    val status: HttpStatus = HttpStatus.BAD_REQUEST,
    override val cause: Throwable? = null,
) : RuntimeException(message, cause)

class EntityNotFoundException(
    message: String,
    errorCode: String = "ENTITY_NOT_FOUND",
    cause: Throwable? = null,
) : OpenControlException(message, errorCode, HttpStatus.NOT_FOUND, cause)

class PluginConflictException(
    message: String,
    errorCode: String = "PLUGIN_CONFLICT",
    cause: Throwable? = null,
) : OpenControlException(message, errorCode, HttpStatus.CONFLICT, cause)

class PluginValidationException(
    message: String,
    errorCode: String = "PLUGIN_VALIDATION_FAILED",
    cause: Throwable? = null,
) : OpenControlException(message, errorCode, HttpStatus.BAD_REQUEST, cause)

class PluginDependencyException(
    message: String,
    errorCode: String = "DEPENDENCY_RESOLUTION_FAILED",
    cause: Throwable? = null,
) : OpenControlException(message, errorCode, HttpStatus.BAD_REQUEST, cause)

@RestControllerAdvice
class GlobalExceptionHandler {
    @ExceptionHandler(OpenControlException::class)
    fun handleOpenControlException(ex: OpenControlException): ResponseEntity<ProblemDetail> {
        val problem =
            ProblemDetail.forStatusAndDetail(ex.status, ex.message).apply {
                title = ex.errorCode
                type = URI.create("https://opencontrol.org/errors/${ex.errorCode.lowercase()}")
                setProperty("timestamp", Instant.now())
            }
        return ResponseEntity.status(ex.status).body(problem)
    }

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidationException(ex: MethodArgumentNotValidException): ResponseEntity<ProblemDetail> {
        val errors = ex.bindingResult.fieldErrors.associate { it.field to (it.defaultMessage ?: "Invalid value") }
        val problem =
            ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Validation failed").apply {
                title = "VALIDATION_FAILED"
                type = URI.create("https://opencontrol.org/errors/validation_failed")
                setProperty("timestamp", Instant.now())
                setProperty("errors", errors)
            }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(problem)
    }

    @ExceptionHandler(org.springframework.security.access.AccessDeniedException::class)
    fun handleAccessDeniedException(ex: org.springframework.security.access.AccessDeniedException): ResponseEntity<ProblemDetail> {
        val problem =
            ProblemDetail
                .forStatusAndDetail(
                    HttpStatus.FORBIDDEN,
                    ex.message ?: "Access Denied",
                ).apply {
                    title = "ACCESS_DENIED"
                    type = URI.create("https://opencontrol.org/errors/access_denied")
                    setProperty("timestamp", Instant.now())
                }
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(problem)
    }

    @ExceptionHandler(Exception::class)
    fun handleGenericException(ex: Exception): ResponseEntity<ProblemDetail> {
        val problem =
            ProblemDetail
                .forStatusAndDetail(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    ex.message ?: "An unexpected error occurred",
                ).apply {
                    title = "INTERNAL_SERVER_ERROR"
                    type = URI.create("https://opencontrol.org/errors/internal_error")
                    setProperty("timestamp", Instant.now())
                }
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(problem)
    }
}
