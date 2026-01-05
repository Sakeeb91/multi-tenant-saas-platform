package com.multitenant.saas.api.dto

import zio.json.*

/** Request payload for creating a new resource */
final case class CreateResourceRequest(
    name: String,
    data: Option[Map[String, String]]
)

object CreateResourceRequest:
  given JsonDecoder[CreateResourceRequest] = DeriveJsonDecoder.gen[CreateResourceRequest]

/** Request payload for updating an existing resource */
final case class UpdateResourceRequest(
    name: Option[String],
    data: Option[Map[String, String]]
)

object UpdateResourceRequest:
  given JsonDecoder[UpdateResourceRequest] = DeriveJsonDecoder.gen[UpdateResourceRequest]
