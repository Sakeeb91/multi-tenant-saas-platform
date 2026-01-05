package com.multitenant.saas.services

import com.multitenant.saas.domain.ids.TenantId
import com.multitenant.saas.domain.models.Tenant
import com.multitenant.saas.domain.enums.Plan
import com.multitenant.saas.errors.AppError
import zio.*

/**
 * Service interface for Tenant business logic.
 *
 * Handles tenant lifecycle operations including creation,
 * lookup by various identifiers, and plan management.
 */
trait TenantService:
  /** Create a new tenant with the given name and slug */
  def create(name: String, slug: String): IO[AppError, Tenant]

  /** Get a tenant by its unique ID */
  def getById(id: TenantId): IO[AppError, Tenant]

  /** Get a tenant by its URL-friendly slug */
  def getBySlug(slug: String): IO[AppError, Tenant]

  /**
   * Get a tenant by either slug or ID string.
   * Useful for middleware that needs to resolve tenant from various sources.
   */
  def getBySlugOrId(identifier: String): IO[AppError, Tenant]

  /** Update a tenant's subscription plan */
  def updatePlan(id: TenantId, plan: Plan): IO[AppError, Tenant]

  /** List all tenants with pagination */
  def list(limit: Int, offset: Int): IO[AppError, List[Tenant]]

object TenantService:
  def create(name: String, slug: String): ZIO[TenantService, AppError, Tenant] =
    ZIO.serviceWithZIO(_.create(name, slug))

  def getById(id: TenantId): ZIO[TenantService, AppError, Tenant] =
    ZIO.serviceWithZIO(_.getById(id))

  def getBySlug(slug: String): ZIO[TenantService, AppError, Tenant] =
    ZIO.serviceWithZIO(_.getBySlug(slug))

  def getBySlugOrId(identifier: String): ZIO[TenantService, AppError, Tenant] =
    ZIO.serviceWithZIO(_.getBySlugOrId(identifier))

  def updatePlan(id: TenantId, plan: Plan): ZIO[TenantService, AppError, Tenant] =
    ZIO.serviceWithZIO(_.updatePlan(id, plan))

  def list(limit: Int, offset: Int): ZIO[TenantService, AppError, List[Tenant]] =
    ZIO.serviceWithZIO(_.list(limit, offset))
