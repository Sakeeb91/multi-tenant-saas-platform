package com.multitenant.saas.domain.models

import com.multitenant.saas.domain.ids.{TenantId, UserId}
import com.multitenant.saas.domain.enums.Role
import java.time.Instant

/** Represents a user belonging to a tenant */
final case class User(
    id: UserId,
    tenantId: TenantId,
    email: String,
    name: String,
    role: Role,
    createdAt: Instant
)

object User:
  def create(
      tenantId: TenantId,
      email: String,
      name: String,
      role: Role = Role.Member
  ): User =
    User(
      id = UserId.generate,
      tenantId = tenantId,
      email = email,
      name = name,
      role = role,
      createdAt = Instant.now
    )
