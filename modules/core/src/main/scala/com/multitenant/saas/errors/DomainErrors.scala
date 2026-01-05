package com.multitenant.saas.errors

import AppError.DomainError

/** Error when an entity is not found */
final case class NotFound(entityType: String, id: String) extends DomainError:
  val code = "NOT_FOUND"
  val message = s"$entityType with id $id not found"

/** Error when an entity already exists (unique constraint violation) */
final case class AlreadyExists(entityType: String, field: String, value: String)
    extends DomainError:
  val code = "ALREADY_EXISTS"
  val message = s"$entityType with $field '$value' already exists"

/** Error when validation fails */
final case class ValidationError(errors: List[String]) extends DomainError:
  val code = "VALIDATION_ERROR"
  val message = s"Validation failed: ${errors.mkString(", ")}"

object ValidationError:
  def single(error: String): ValidationError = ValidationError(List(error))
