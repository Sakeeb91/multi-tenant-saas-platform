package com.multitenant.saas.domain.ids

import java.util.UUID

/** Phantom type for tenant identifiers to prevent mixing up IDs at compile time */
opaque type TenantId = UUID

object TenantId:
  def apply(uuid: UUID): TenantId = uuid

  def generate: TenantId = UUID.randomUUID()

  def fromString(s: String): Either[String, TenantId] =
    try Right(UUID.fromString(s))
    catch case _: IllegalArgumentException => Left(s"Invalid TenantId: $s")

  def unsafeFromString(s: String): TenantId = UUID.fromString(s)

  extension (id: TenantId)
    def value: UUID = id
    def asString: String = id.toString
