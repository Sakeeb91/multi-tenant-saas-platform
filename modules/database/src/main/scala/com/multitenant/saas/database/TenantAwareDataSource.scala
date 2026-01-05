package com.multitenant.saas.database

import com.multitenant.saas.domain.ids.TenantId
import com.multitenant.saas.errors.AppError.DatabaseError
import zio.*
import java.sql.Connection

/**
 * Wraps a DataSource to automatically set the tenant context
 * on each connection before use.
 *
 * This ensures RLS policies are properly enforced by setting
 * the app.current_tenant_id session variable.
 */
trait TenantAwareDataSource:
  /**
   * Execute a function with a connection that has tenant context set.
   * The context is automatically cleared after the function completes.
   *
   * @param tenantId The tenant to set as context
   * @param f Function to execute with the tenant-scoped connection
   * @return Result of the function or DatabaseError if something fails
   */
  def withTenantConnection[A](tenantId: TenantId)(
      f: Connection => Task[A]
  ): IO[DatabaseError, A]

  /**
   * Execute without tenant context (for cross-tenant operations).
   *
   * Use sparingly - most operations should be tenant-scoped.
   * Typical use cases:
   * - Tenant lookup by slug
   * - System-level admin operations
   * - Migrations
   *
   * @param f Function to execute with admin connection
   * @return Result of the function or DatabaseError if something fails
   */
  def withAdminConnection[A](f: Connection => Task[A]): IO[DatabaseError, A]

object TenantAwareDataSource:
  def withTenantConnection[A](tenantId: TenantId)(
      f: Connection => Task[A]
  ): ZIO[TenantAwareDataSource, DatabaseError, A] =
    ZIO.serviceWithZIO(_.withTenantConnection(tenantId)(f))

  def withAdminConnection[A](
      f: Connection => Task[A]
  ): ZIO[TenantAwareDataSource, DatabaseError, A] =
    ZIO.serviceWithZIO(_.withAdminConnection(f))
