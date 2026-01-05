package com.multitenant.saas.database.repositories

import com.multitenant.saas.domain.ids.TenantId
import com.multitenant.saas.domain.models.Tenant
import com.multitenant.saas.domain.enums.Plan
import com.multitenant.saas.errors.AppError.InfraError
import zio.*

/**
 * Repository interface for Tenant persistence operations.
 *
 * Note: Tenants table is NOT subject to RLS since tenant lookup
 * happens before tenant context is established.
 */
trait TenantRepository:
  def insert(tenant: Tenant): IO[InfraError, Tenant]
  def findById(id: TenantId): IO[InfraError, Option[Tenant]]
  def findBySlug(slug: String): IO[InfraError, Option[Tenant]]
  def findAll(limit: Int, offset: Int): IO[InfraError, List[Tenant]]
  def update(tenant: Tenant): IO[InfraError, Tenant]
  def updatePlan(id: TenantId, plan: Plan): IO[InfraError, Boolean]
  def delete(id: TenantId): IO[InfraError, Boolean]
  def count: IO[InfraError, Long]

object TenantRepository:
  def insert(tenant: Tenant): ZIO[TenantRepository, InfraError, Tenant] =
    ZIO.serviceWithZIO(_.insert(tenant))

  def findById(id: TenantId): ZIO[TenantRepository, InfraError, Option[Tenant]] =
    ZIO.serviceWithZIO(_.findById(id))

  def findBySlug(slug: String): ZIO[TenantRepository, InfraError, Option[Tenant]] =
    ZIO.serviceWithZIO(_.findBySlug(slug))

  def findAll(
      limit: Int,
      offset: Int
  ): ZIO[TenantRepository, InfraError, List[Tenant]] =
    ZIO.serviceWithZIO(_.findAll(limit, offset))

  def update(tenant: Tenant): ZIO[TenantRepository, InfraError, Tenant] =
    ZIO.serviceWithZIO(_.update(tenant))

  def updatePlan(
      id: TenantId,
      plan: Plan
  ): ZIO[TenantRepository, InfraError, Boolean] =
    ZIO.serviceWithZIO(_.updatePlan(id, plan))

  def delete(id: TenantId): ZIO[TenantRepository, InfraError, Boolean] =
    ZIO.serviceWithZIO(_.delete(id))

  def count: ZIO[TenantRepository, InfraError, Long] =
    ZIO.serviceWithZIO(_.count)
