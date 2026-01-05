package com.multitenant.saas.errors

import AppError.AuthError

/** Error when authentication token is invalid or expired */
case object InvalidToken extends AuthError:
  val code = "INVALID_TOKEN"
  val message = "Invalid or expired token"

/** Error when user lacks required permissions */
case object InsufficientPermissions extends AuthError:
  val code = "FORBIDDEN"
  val message = "Insufficient permissions for this operation"

/** Error when credentials are invalid */
case object InvalidCredentials extends AuthError:
  val code = "INVALID_CREDENTIALS"
  val message = "Invalid email or password"

/** Error when authentication is required but not provided */
case object Unauthenticated extends AuthError:
  val code = "UNAUTHENTICATED"
  val message = "Authentication required"
