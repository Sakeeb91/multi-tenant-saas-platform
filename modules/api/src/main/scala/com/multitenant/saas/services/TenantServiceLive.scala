package com.multitenant.saas.services

import com.multitenant.saas.domain.ids.TenantId
import com.multitenant.saas.domain.models.Tenant
import com.multitenant.saas.domain.enums.Plan
import com.multitenant.saas.errors.AppError
import com.multitenant.saas.errors.AppError.*
import com.multitenant.saas.database.repositories.TenantRepository
import zio.*
import java.time.Instant
import java.util.UUID

/**
 * Live implementation of TenantService.
 *
 * Handles tenant lifecycle with validation and slug uniqueness enforcement.
 */
final case class TenantServiceLive(repository: TenantRepository)
    extends TenantService:

  override def create(name: String, slug: String): IO[AppError, Tenant] =
    for
      _ <- validateName(name)
      _ <- validateSlug(slug)
      _ <- checkSlugAvailable(slug)
      now = Instant.now
      tenant = Tenant(
        id = TenantId.generate,
        name = name.trim,
        slug = slug.toLowerCase.trim,
        plan = Plan.Free,
        createdAt = now,
        updatedAt = now
      )
      created <- repository.insert(tenant)
    yield created

  override def getById(id: TenantId): IO[AppError, Tenant] =
    repository.findById(id).flatMap {
      case Some(tenant) => ZIO.succeed(tenant)
      case None         => ZIO.fail(NotFound("Tenant", id.asString))
    }

  override def getBySlug(slug: String): IO[AppError, Tenant] =
    repository.findBySlug(slug.toLowerCase).flatMap {
      case Some(tenant) => ZIO.succeed(tenant)
      case None         => ZIO.fail(NotFound("Tenant", slug))
    }

  override def getBySlugOrId(identifier: String): IO[AppError, Tenant] =
    // Try parsing as UUID first, then fall back to slug lookup
    TenantId.fromString(identifier) match
      case Right(id) => getById(id)
      case Left(_)   => getBySlug(identifier)

  override def updatePlan(id: TenantId, plan: Plan): IO[AppError, Tenant] =
    for
      existing <- getById(id)
      _ <- repository.updatePlan(id, plan)
      updated = existing.copy(plan = plan, updatedAt = Instant.now)
    yield updated

  override def list(limit: Int, offset: Int): IO[AppError, List[Tenant]] =
    for
      validLimit <- ZIO.succeed(limit.max(1).min(100))
      validOffset <- ZIO.succeed(offset.max(0))
      tenants <- repository.findAll(validLimit, validOffset)
    yield tenants

  private def validateName(name: String): IO[ValidationError, Unit] =
    val trimmed = name.trim
    val errors = List(
      Option.when(trimmed.isEmpty)("Name cannot be empty"),
      Option.when(trimmed.length > 255)("Name cannot exceed 255 characters")
    ).flatten
    if errors.isEmpty then ZIO.unit
    else ZIO.fail(ValidationError(errors))

  private def validateSlug(slug: String): IO[ValidationError, Unit] =
    val trimmed = slug.toLowerCase.trim
    val errors = List(
      Option.when(trimmed.isEmpty)("Slug cannot be empty"),
      Option.when(trimmed.length < 3)("Slug must be at least 3 characters"),
      Option.when(trimmed.length > 100)("Slug cannot exceed 100 characters"),
      Option.when(!trimmed.matches("^[a-z0-9][a-z0-9-]*[a-z0-9]$"))(
        "Slug must contain only lowercase letters, numbers, and hyphens"
      )
    ).flatten
    if errors.isEmpty then ZIO.unit
    else ZIO.fail(ValidationError(errors))

  private def checkSlugAvailable(slug: String): IO[AppError, Unit] =
    repository.findBySlug(slug.toLowerCase).flatMap {
      case Some(_) => ZIO.fail(AlreadyExists("Tenant", "slug", slug))
      case None    => ZIO.unit
    }

object TenantServiceLive:
  val layer: ZLayer[TenantRepository, Nothing, TenantService] =
    ZLayer.fromFunction(TenantServiceLive.apply)
