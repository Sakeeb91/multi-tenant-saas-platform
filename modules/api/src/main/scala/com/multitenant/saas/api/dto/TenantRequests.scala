package com.multitenant.saas.api.dto

import zio.json.*

/** Request payload for creating a new tenant */
final case class CreateTenantRequest(
    name: String,
    slug: String
)

object CreateTenantRequest:
  given JsonDecoder[CreateTenantRequest] = DeriveJsonDecoder.gen[CreateTenantRequest]

/** Request payload for updating a tenant's plan */
final case class UpdateTenantPlanRequest(
    plan: String
)

object UpdateTenantPlanRequest:
  given JsonDecoder[UpdateTenantPlanRequest] = DeriveJsonDecoder.gen[UpdateTenantPlanRequest]

/** Request payload for updating tenant details */
final case class UpdateTenantRequest(
    name: Option[String],
    slug: Option[String]
)

object UpdateTenantRequest:
  given JsonDecoder[UpdateTenantRequest] = DeriveJsonDecoder.gen[UpdateTenantRequest]
