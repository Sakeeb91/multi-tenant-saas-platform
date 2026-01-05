package com.multitenant.saas.api.middleware

import com.multitenant.saas.context.TenantContext
import com.multitenant.saas.domain.ids.{TenantId, UserId}
import com.multitenant.saas.domain.models.{Tenant, User}
import com.multitenant.saas.domain.enums.Role
import com.multitenant.saas.services.TenantService
import com.multitenant.saas.errors.AppError
import zio.*
import zio.http.*
import java.time.Instant

/**
 * Middleware for extracting tenant context from incoming requests.
 *
 * Extracts tenant identifier from (in order of priority):
 * 1. X-Tenant-ID header (for API clients)
 * 2. Subdomain (for browser clients)
 *
 * JWT-based extraction will be added in Phase 3 (Authentication).
 */
object TenantMiddleware:

  /** Header name for explicit tenant identification */
  val TenantIdHeader = "X-Tenant-ID"

  /** Reserved subdomain prefixes that should not be treated as tenant slugs */
  val ReservedPrefixes: Set[String] = Set("www", "api", "app", "admin", "static")

  /**
   * Extract tenant identifier from the request.
   *
   * @param request The incoming HTTP request
   * @return Optional tenant identifier string
   */
  def extractTenantIdentifier(request: Request): Option[String] =
    // Try header first (API clients)
    request.headers
      .get(TenantIdHeader)
      .map(_.toString)
      .filter(_.nonEmpty)
      // Then try subdomain
      .orElse(extractFromSubdomain(request))

  /**
   * Extract tenant identifier from subdomain.
   *
   * Parses "acme.yourapp.com" -> "acme"
   * Skips reserved prefixes like "www", "api", "app"
   */
  private def extractFromSubdomain(request: Request): Option[String] =
    request.headers
      .get(Header.Host)
      .map(_.toString)
      .flatMap { host =>
        val hostWithoutPort = host.split(":").head
        val parts = hostWithoutPort.split("\\.")
        // Need at least 3 parts (subdomain.domain.tld)
        if parts.length >= 3 then
          val subdomain = parts.head.toLowerCase
          if !ReservedPrefixes.contains(subdomain) then Some(subdomain)
          else None
        else None
      }

  /**
   * Resolve tenant context from an identifier.
   *
   * Creates a placeholder user until auth is implemented in Phase 3.
   */
  def resolveTenantContext(
      identifier: String
  ): ZIO[TenantService, AppError, TenantContext] =
    for tenant <- TenantService.getBySlugOrId(identifier)
    yield
      // Placeholder user until authentication is implemented
      val placeholderUser = User(
        id = UserId.generate,
        tenantId = tenant.id,
        email = "system@placeholder.local",
        name = "System User",
        role = Role.Admin,
        createdAt = Instant.now
      )
      TenantContext(tenant.id, tenant, placeholderUser)

  /**
   * Create error response for missing tenant context.
   */
  def missingTenantResponse: Response =
    Response
      .json("""{"error":"bad_request","code":"MISSING_TENANT","message":"Missing tenant identifier. Provide X-Tenant-ID header or use subdomain."}""")
      .status(Status.BadRequest)

  /**
   * Create error response for invalid tenant.
   */
  def invalidTenantResponse: Response =
    Response
      .json("""{"error":"unauthorized","code":"INVALID_TENANT","message":"Invalid or unknown tenant"}""")
      .status(Status.Unauthorized)
