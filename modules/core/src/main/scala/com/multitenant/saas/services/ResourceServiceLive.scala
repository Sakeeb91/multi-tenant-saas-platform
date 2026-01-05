package com.multitenant.saas.services

import com.multitenant.saas.domain.ids.{ResourceId, TenantId, UserId}
import com.multitenant.saas.domain.models.Resource
import com.multitenant.saas.errors.{AppError, NotFound, ValidationError}
import com.multitenant.saas.database.repositories.ResourceRepository
import zio.*
import java.time.Instant

final case class ResourceServiceLive(repository: ResourceRepository)
    extends ResourceService:

  override def create(
      tenantId: TenantId,
      userId: UserId,
      name: String,
      data: Map[String, String]
  ): IO[AppError, Resource] =
    for
      _ <- validateName(name)
      now = Instant.now
      resource = Resource(
        id = ResourceId.generate,
        tenantId = tenantId,
        name = name.trim,
        data = data,
        createdBy = userId,
        createdAt = now,
        updatedAt = now
      )
      created <- repository.insert(resource)
    yield created

  override def get(tenantId: TenantId, id: ResourceId): IO[AppError, Resource] =
    repository.findById(id, tenantId).flatMap {
      case Some(resource) => ZIO.succeed(resource)
      case None           => ZIO.fail(NotFound("Resource", id.asString))
    }

  override def list(
      tenantId: TenantId,
      limit: Int,
      offset: Int
  ): IO[AppError, List[Resource]] =
    for
      validLimit <- ZIO.succeed(limit.max(1).min(100))
      validOffset <- ZIO.succeed(offset.max(0))
      resources <- repository.findAll(tenantId, validLimit, validOffset)
    yield resources

  override def update(
      tenantId: TenantId,
      id: ResourceId,
      name: Option[String],
      data: Option[Map[String, String]]
  ): IO[AppError, Resource] =
    for
      existing <- get(tenantId, id)
      _ <- name.map(validateName).getOrElse(ZIO.unit)
      updated = existing.copy(
        name = name.map(_.trim).getOrElse(existing.name),
        data = data.getOrElse(existing.data),
        updatedAt = Instant.now
      )
      _ <- repository.update(updated)
    yield updated

  override def delete(tenantId: TenantId, id: ResourceId): IO[AppError, Unit] =
    for
      deleted <- repository.delete(id, tenantId)
      _ <- ZIO.unless(deleted)(ZIO.fail(NotFound("Resource", id.asString)))
    yield ()

  private def validateName(name: String): IO[ValidationError, Unit] =
    val trimmed = name.trim
    val errors = List(
      Option.when(trimmed.isEmpty)("Name cannot be empty"),
      Option.when(trimmed.length > 255)("Name cannot exceed 255 characters"),
      Option.when(!trimmed.matches("^[a-zA-Z0-9\\s\\-_]+$"))(
        "Name contains invalid characters"
      )
    ).flatten

    if errors.isEmpty then ZIO.unit
    else ZIO.fail(ValidationError(errors))

object ResourceServiceLive:
  val layer: ZLayer[ResourceRepository, Nothing, ResourceService] =
    ZLayer.fromFunction(ResourceServiceLive.apply)
