package com.multitenant.saas.domain.models

import com.multitenant.saas.domain.ids.TenantId
import com.multitenant.saas.domain.enums.Plan
import java.time.Instant

/** Represents an organization/tenant in the multi-tenant system */
final case class Tenant(
    id: TenantId,
    name: String,
    slug: String,
    plan: Plan,
    createdAt: Instant,
    updatedAt: Instant
)

object Tenant:
  def create(name: String, slug: String, plan: Plan = Plan.Free): Tenant =
    val now = Instant.now
    Tenant(
      id = TenantId.generate,
      name = name,
      slug = slug,
      plan = plan,
      createdAt = now,
      updatedAt = now
    )
