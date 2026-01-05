package com.multitenant.saas.domain.ids

import java.util.UUID

/** Phantom type for resource identifiers to prevent mixing up IDs at compile time */
opaque type ResourceId = UUID

object ResourceId:
  def apply(uuid: UUID): ResourceId = uuid

  def generate: ResourceId = UUID.randomUUID()

  def fromString(s: String): Either[String, ResourceId] =
    try Right(UUID.fromString(s))
    catch case _: IllegalArgumentException => Left(s"Invalid ResourceId: $s")

  def unsafeFromString(s: String): ResourceId = UUID.fromString(s)

  extension (id: ResourceId)
    def value: UUID = id
    def asString: String = id.toString
