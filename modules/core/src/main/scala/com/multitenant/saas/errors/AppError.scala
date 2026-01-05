package com.multitenant.saas.errors

/** Base sealed trait for all application errors.
  *
  * Using a sealed hierarchy enables exhaustive pattern matching
  * and provides type-safe error handling across the application.
  */
sealed trait AppError extends Throwable:
  def message: String
  def code: String
  override def getMessage: String = message

object AppError:
  /** Domain errors represent business logic failures (400-level responses) */
  sealed trait DomainError extends AppError

  /** Auth errors represent authentication/authorization failures (401/403 responses) */
  sealed trait AuthError extends AppError

  /** Infrastructure errors represent system-level failures (500-level responses) */
  sealed trait InfraError extends AppError

  // Domain Errors
  final case class NotFound(entityType: String, id: String) extends DomainError:
    val code = "NOT_FOUND"
    val message = s"$entityType with id $id not found"

  final case class AlreadyExists(entityType: String, field: String, value: String)
      extends DomainError:
    val code = "ALREADY_EXISTS"
    val message = s"$entityType with $field '$value' already exists"

  final case class ValidationError(errors: List[String]) extends DomainError:
    val code = "VALIDATION_ERROR"
    val message = s"Validation failed: ${errors.mkString(", ")}"

  object ValidationError:
    def single(error: String): ValidationError = ValidationError(List(error))

  // Auth Errors
  case object InvalidToken extends AuthError:
    val code = "INVALID_TOKEN"
    val message = "Invalid or expired token"

  case object InsufficientPermissions extends AuthError:
    val code = "FORBIDDEN"
    val message = "Insufficient permissions for this operation"

  case object InvalidCredentials extends AuthError:
    val code = "INVALID_CREDENTIALS"
    val message = "Invalid email or password"

  case object Unauthenticated extends AuthError:
    val code = "UNAUTHENTICATED"
    val message = "Authentication required"

  // Infrastructure Errors
  final case class DatabaseError(cause: Throwable) extends InfraError:
    val code = "DATABASE_ERROR"
    val message = s"Database error: ${cause.getMessage}"

  final case class ExternalServiceError(service: String, cause: Throwable)
      extends InfraError:
    val code = "EXTERNAL_SERVICE_ERROR"
    val message = s"External service '$service' error: ${cause.getMessage}"

  final case class ConfigurationError(details: String) extends InfraError:
    val code = "CONFIGURATION_ERROR"
    val message = s"Configuration error: $details"

  final case class InternalError(details: String) extends InfraError:
    val code = "INTERNAL_ERROR"
    val message = s"Internal error: $details"
