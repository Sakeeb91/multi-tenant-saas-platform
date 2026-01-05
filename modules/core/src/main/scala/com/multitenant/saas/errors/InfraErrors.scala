package com.multitenant.saas.errors

import AppError.InfraError

/** Error when database operation fails */
final case class DatabaseError(cause: Throwable) extends InfraError:
  val code = "DATABASE_ERROR"
  val message = s"Database error: ${cause.getMessage}"

/** Error when external service call fails */
final case class ExternalServiceError(service: String, cause: Throwable)
    extends InfraError:
  val code = "EXTERNAL_SERVICE_ERROR"
  val message = s"External service '$service' error: ${cause.getMessage}"

/** Error when configuration is invalid */
final case class ConfigurationError(details: String) extends InfraError:
  val code = "CONFIGURATION_ERROR"
  val message = s"Configuration error: $details"

/** Error for unexpected internal failures */
final case class InternalError(details: String) extends InfraError:
  val code = "INTERNAL_ERROR"
  val message = s"Internal error: $details"
