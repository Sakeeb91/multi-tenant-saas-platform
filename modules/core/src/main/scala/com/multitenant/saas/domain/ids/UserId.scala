package com.multitenant.saas.domain.ids

import java.util.UUID

/** Phantom type for user identifiers to prevent mixing up IDs at compile time */
opaque type UserId = UUID

object UserId:
  def apply(uuid: UUID): UserId = uuid

  def generate: UserId = UUID.randomUUID()

  def fromString(s: String): Either[String, UserId] =
    try Right(UUID.fromString(s))
    catch case _: IllegalArgumentException => Left(s"Invalid UserId: $s")

  def unsafeFromString(s: String): UserId = UUID.fromString(s)

  extension (id: UserId)
    def value: UUID = id
    def asString: String = id.toString
