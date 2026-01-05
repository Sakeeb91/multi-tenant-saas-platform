package com.multitenant.saas.api.dto

import com.multitenant.saas.domain.models.Resource
import zio.json.*

/** Response payload for a single resource */
final case class ResourceResponse(
    id: String,
    name: String,
    data: Map[String, String],
    createdBy: String,
    createdAt: String,
    updatedAt: String
)

object ResourceResponse:
  given JsonEncoder[ResourceResponse] = DeriveJsonEncoder.gen[ResourceResponse]

  def from(r: Resource): ResourceResponse =
    ResourceResponse(
      id = r.id.asString,
      name = r.name,
      data = r.data,
      createdBy = r.createdBy.asString,
      createdAt = r.createdAt.toString,
      updatedAt = r.updatedAt.toString
    )

/** Response payload for a list of resources with pagination info */
final case class ResourceListResponse(
    items: List[ResourceResponse],
    count: Int,
    limit: Int,
    offset: Int
)

object ResourceListResponse:
  given JsonEncoder[ResourceListResponse] = DeriveJsonEncoder.gen[ResourceListResponse]

/** Standard error response format */
final case class ErrorResponse(
    error: String,
    code: String,
    message: String
)

object ErrorResponse:
  given JsonEncoder[ErrorResponse] = DeriveJsonEncoder.gen[ErrorResponse]
