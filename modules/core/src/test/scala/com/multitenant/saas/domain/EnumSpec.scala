package com.multitenant.saas.domain

import com.multitenant.saas.domain.enums.*
import zio.test.*

object EnumSpec extends ZIOSpecDefault:

  def spec = suite("Domain Enums")(
    suite("Plan")(
      test("fromString parses valid plans") {
        assertTrue(
          Plan.fromString("free") == Right(Plan.Free),
          Plan.fromString("starter") == Right(Plan.Starter),
          Plan.fromString("professional") == Right(Plan.Professional),
          Plan.fromString("enterprise") == Right(Plan.Enterprise)
        )
      },
      test("fromString is case insensitive") {
        assertTrue(
          Plan.fromString("FREE") == Right(Plan.Free),
          Plan.fromString("Starter") == Right(Plan.Starter)
        )
      },
      test("fromString fails on invalid plan") {
        assertTrue(Plan.fromString("invalid").isLeft)
      },
      test("asString returns lowercase string") {
        assertTrue(
          Plan.Free.asString == "free",
          Plan.Enterprise.asString == "enterprise"
        )
      }
    ),
    suite("Role")(
      test("fromString parses valid roles") {
        assertTrue(
          Role.fromString("owner") == Right(Role.Owner),
          Role.fromString("admin") == Right(Role.Admin),
          Role.fromString("member") == Right(Role.Member),
          Role.fromString("viewer") == Right(Role.Viewer)
        )
      },
      test("fromString fails on invalid role") {
        assertTrue(Role.fromString("superuser").isLeft)
      },
      test("hasPermission respects hierarchy") {
        assertTrue(
          Role.Owner.hasPermission(Role.Admin) == true,
          Role.Owner.hasPermission(Role.Viewer) == true,
          Role.Admin.hasPermission(Role.Owner) == false,
          Role.Admin.hasPermission(Role.Member) == true,
          Role.Member.hasPermission(Role.Admin) == false,
          Role.Viewer.hasPermission(Role.Member) == false,
          Role.Viewer.hasPermission(Role.Viewer) == true
        )
      }
    )
  )
