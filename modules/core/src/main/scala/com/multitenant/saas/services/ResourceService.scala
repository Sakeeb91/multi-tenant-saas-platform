package com.multitenant.saas.services

import com.multitenant.saas.domain.ids.{ResourceId, TenantId, UserId}
import com.multitenant.saas.domain.models.Resource
import com.multitenant.saas.errors.AppError
import zio.*

/** Service interface for Resource business logic */
trait ResourceService:
  def create(
      tenantId: TenantId,
      userId: UserId,
      name: String,
      data: Map[String, String]
  ): IO[AppError, Resource]

  def get(tenantId: TenantId, id: ResourceId): IO[AppError, Resource]

  def list(
      tenantId: TenantId,
      limit: Int,
      offset: Int
  ): IO[AppError, List[Resource]]

  def update(
      tenantId: TenantId,
      id: ResourceId,
      name: Option[String],
      data: Option[Map[String, String]]
  ): IO[AppError, Resource]

  def delete(tenantId: TenantId, id: ResourceId): IO[AppError, Unit]

object ResourceService:
  def create(
      tenantId: TenantId,
      userId: UserId,
      name: String,
      data: Map[String, String]
  ): ZIO[ResourceService, AppError, Resource] =
    ZIO.serviceWithZIO(_.create(tenantId, userId, name, data))

  def get(
      tenantId: TenantId,
      id: ResourceId
  ): ZIO[ResourceService, AppError, Resource] =
    ZIO.serviceWithZIO(_.get(tenantId, id))

  def list(
      tenantId: TenantId,
      limit: Int,
      offset: Int
  ): ZIO[ResourceService, AppError, List[Resource]] =
    ZIO.serviceWithZIO(_.list(tenantId, limit, offset))

  def update(
      tenantId: TenantId,
      id: ResourceId,
      name: Option[String],
      data: Option[Map[String, String]]
  ): ZIO[ResourceService, AppError, Resource] =
    ZIO.serviceWithZIO(_.update(tenantId, id, name, data))

  def delete(
      tenantId: TenantId,
      id: ResourceId
  ): ZIO[ResourceService, AppError, Unit] =
    ZIO.serviceWithZIO(_.delete(tenantId, id))
