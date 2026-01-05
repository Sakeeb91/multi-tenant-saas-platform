package com.multitenant.saas.database.repositories

import com.multitenant.saas.domain.ids.{ResourceId, TenantId}
import com.multitenant.saas.domain.models.Resource
import com.multitenant.saas.errors.AppError.InfraError
import zio.*

/** Repository interface for Resource persistence operations */
trait ResourceRepository:
  def insert(resource: Resource): IO[InfraError, Resource]
  def findById(id: ResourceId, tenantId: TenantId): IO[InfraError, Option[Resource]]
  def findAll(tenantId: TenantId, limit: Int, offset: Int): IO[InfraError, List[Resource]]
  def update(resource: Resource): IO[InfraError, Resource]
  def delete(id: ResourceId, tenantId: TenantId): IO[InfraError, Boolean]
  def count(tenantId: TenantId): IO[InfraError, Long]

object ResourceRepository:
  def insert(resource: Resource): ZIO[ResourceRepository, InfraError, Resource] =
    ZIO.serviceWithZIO(_.insert(resource))

  def findById(
      id: ResourceId,
      tenantId: TenantId
  ): ZIO[ResourceRepository, InfraError, Option[Resource]] =
    ZIO.serviceWithZIO(_.findById(id, tenantId))

  def findAll(
      tenantId: TenantId,
      limit: Int,
      offset: Int
  ): ZIO[ResourceRepository, InfraError, List[Resource]] =
    ZIO.serviceWithZIO(_.findAll(tenantId, limit, offset))

  def update(resource: Resource): ZIO[ResourceRepository, InfraError, Resource] =
    ZIO.serviceWithZIO(_.update(resource))

  def delete(
      id: ResourceId,
      tenantId: TenantId
  ): ZIO[ResourceRepository, InfraError, Boolean] =
    ZIO.serviceWithZIO(_.delete(id, tenantId))

  def count(tenantId: TenantId): ZIO[ResourceRepository, InfraError, Long] =
    ZIO.serviceWithZIO(_.count(tenantId))
