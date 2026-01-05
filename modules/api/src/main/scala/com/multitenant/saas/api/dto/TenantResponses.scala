package com.multitenant.saas.api.dto

import com.multitenant.saas.domain.models.Tenant
import zio.json.*

/** Response payload for a single tenant */
final case class TenantResponse(
    id: String,
    name: String,
    slug: String,
    plan: String,
    createdAt: String,
    updatedAt: String
)

object TenantResponse:
  given JsonEncoder[TenantResponse] = DeriveJsonEncoder.gen[TenantResponse]

  def from(t: Tenant): TenantResponse =
    TenantResponse(
      id = t.id.asString,
      name = t.name,
      slug = t.slug,
      plan = t.plan.asString,
      createdAt = t.createdAt.toString,
      updatedAt = t.updatedAt.toString
    )

/** Response payload for a list of tenants with pagination info */
final case class TenantListResponse(
    items: List[TenantResponse],
    count: Int,
    limit: Int,
    offset: Int
)

object TenantListResponse:
  given JsonEncoder[TenantListResponse] = DeriveJsonEncoder.gen[TenantListResponse]
