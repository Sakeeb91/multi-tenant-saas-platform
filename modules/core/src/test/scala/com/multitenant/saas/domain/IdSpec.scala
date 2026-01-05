package com.multitenant.saas.domain

import com.multitenant.saas.domain.ids.*
import zio.test.*
import java.util.UUID

object IdSpec extends ZIOSpecDefault:

  def spec = suite("Phantom Type IDs")(
    suite("TenantId")(
      test("generate creates unique IDs") {
        val id1 = TenantId.generate
        val id2 = TenantId.generate
        assertTrue(id1.asString != id2.asString)
      },
      test("fromString parses valid UUID") {
        val uuid = UUID.randomUUID()
        val result = TenantId.fromString(uuid.toString)
        assertTrue(result.isRight)
      },
      test("fromString fails on invalid string") {
        val result = TenantId.fromString("not-a-uuid")
        assertTrue(result.isLeft)
      },
      test("value returns underlying UUID") {
        val uuid = UUID.randomUUID()
        val id = TenantId(uuid)
        assertTrue(id.value == uuid)
      },
      test("asString returns string representation") {
        val uuid = UUID.randomUUID()
        val id = TenantId(uuid)
        assertTrue(id.asString == uuid.toString)
      }
    ),
    suite("UserId")(
      test("generate creates unique IDs") {
        val id1 = UserId.generate
        val id2 = UserId.generate
        assertTrue(id1.asString != id2.asString)
      },
      test("fromString parses valid UUID") {
        val uuid = UUID.randomUUID()
        val result = UserId.fromString(uuid.toString)
        assertTrue(result.isRight)
      },
      test("fromString fails on invalid string") {
        val result = UserId.fromString("invalid")
        assertTrue(result.isLeft)
      }
    ),
    suite("ResourceId")(
      test("generate creates unique IDs") {
        val id1 = ResourceId.generate
        val id2 = ResourceId.generate
        assertTrue(id1.asString != id2.asString)
      },
      test("fromString parses valid UUID") {
        val uuid = UUID.randomUUID()
        val result = ResourceId.fromString(uuid.toString)
        assertTrue(result.isRight)
      },
      test("fromString fails on invalid string") {
        val result = ResourceId.fromString("bad-id")
        assertTrue(result.isLeft)
      }
    )
  )
