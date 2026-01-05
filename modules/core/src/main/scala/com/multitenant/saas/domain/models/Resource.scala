package com.multitenant.saas.domain.models

import com.multitenant.saas.domain.ids.{TenantId, UserId, ResourceId}
import java.time.Instant

/** Generic resource entity belonging to a tenant */
final case class Resource(
    id: ResourceId,
    tenantId: TenantId,
    name: String,
    data: Map[String, String],
    createdBy: UserId,
    createdAt: Instant,
    updatedAt: Instant
)

object Resource:
  def create(
      tenantId: TenantId,
      name: String,
      data: Map[String, String],
      createdBy: UserId
  ): Resource =
    val now = Instant.now
    Resource(
      id = ResourceId.generate,
      tenantId = tenantId,
      name = name,
      data = data,
      createdBy = createdBy,
      createdAt = now,
      updatedAt = now
    )
