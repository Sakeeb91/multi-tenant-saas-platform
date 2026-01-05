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
