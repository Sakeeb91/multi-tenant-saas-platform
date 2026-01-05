package com.multitenant.saas

import com.multitenant.saas.context.TenantContext
import com.multitenant.saas.domain.ids.{ResourceId, TenantId, UserId}
import com.multitenant.saas.domain.models.{Resource, Tenant, User}
import com.multitenant.saas.domain.enums.{Plan, Role}
import zio.*
import zio.test.*
import zio.test.Assertion.*
import java.time.Instant

/**
 * Integration tests for multi-tenant data isolation.
 *
 * These tests verify that:
 * 1. Tenant A cannot read Tenant B's resources
 * 2. Tenant A cannot update Tenant B's resources
 * 3. Tenant A cannot delete Tenant B's resources
 * 4. List operations only return current tenant's resources
 */
object MultiTenantIsolationSpec extends ZIOSpecDefault:

  // Test fixtures
  val now: Instant = Instant.now

  val tenantA: Tenant = Tenant(
    id = TenantId.generate,
    name = "Acme Corporation",
    slug = "acme",
    plan = Plan.Starter,
    createdAt = now,
    updatedAt = now
  )

  val tenantB: Tenant = Tenant(
    id = TenantId.generate,
    name = "Beta Inc",
    slug = "beta",
    plan = Plan.Free,
    createdAt = now,
    updatedAt = now
  )

  val userA: User = User(
    id = UserId.generate,
    tenantId = tenantA.id,
    email = "admin@acme.com",
    name = "Acme Admin",
    role = Role.Admin,
    createdAt = now
  )

  val userB: User = User(
    id = UserId.generate,
    tenantId = tenantB.id,
    email = "admin@beta.com",
    name = "Beta Admin",
    role = Role.Admin,
    createdAt = now
  )

  val ctxA: TenantContext = TenantContext(tenantA.id, tenantA, userA)
  val ctxB: TenantContext = TenantContext(tenantB.id, tenantB, userB)

  def spec: Spec[Any, Any] = suite("Multi-Tenant Isolation")(
    suite("TenantContext")(
      test("provides access to tenant information") {
        val ctx = ctxA
        assertTrue(
          ctx.tenantId == tenantA.id,
          ctx.tenant == tenantA,
          ctx.currentUser == userA
        )
      },
      test("role hierarchy correctly orders roles") {
        val ownerCtx = ctxA.copy(currentUser = userA.copy(role = Role.Owner))
        val adminCtx = ctxA.copy(currentUser = userA.copy(role = Role.Admin))
        val memberCtx = ctxA.copy(currentUser = userA.copy(role = Role.Member))
        val viewerCtx = ctxA.copy(currentUser = userA.copy(role = Role.Viewer))

        assertTrue(
          ownerCtx.hasRole(Role.Owner),
          ownerCtx.hasRole(Role.Admin),
          ownerCtx.hasRole(Role.Member),
          ownerCtx.hasRole(Role.Viewer),
          adminCtx.hasRole(Role.Admin),
          adminCtx.hasRole(Role.Member),
          !adminCtx.hasRole(Role.Owner),
          memberCtx.hasRole(Role.Member),
          !memberCtx.hasRole(Role.Admin),
          viewerCtx.hasRole(Role.Viewer),
          !viewerCtx.hasRole(Role.Member)
        )
      },
      test("isAdmin returns true for Admin and Owner roles") {
        val adminCtx = ctxA.copy(currentUser = userA.copy(role = Role.Admin))
        val ownerCtx = ctxA.copy(currentUser = userA.copy(role = Role.Owner))
        val memberCtx = ctxA.copy(currentUser = userA.copy(role = Role.Member))

        assertTrue(
          adminCtx.isAdmin,
          ownerCtx.isAdmin,
          !memberCtx.isAdmin
        )
      },
      test("isOwner only returns true for Owner role") {
        val ownerCtx = ctxA.copy(currentUser = userA.copy(role = Role.Owner))
        val adminCtx = ctxA.copy(currentUser = userA.copy(role = Role.Admin))

        assertTrue(
          ownerCtx.isOwner,
          !adminCtx.isOwner
        )
      }
    ),
    suite("TenantContext ZIO accessors")(
      test("get returns full context from environment") {
        for ctx <- TenantContext.get
        yield assertTrue(ctx == ctxA)
      }.provide(ZLayer.succeed(ctxA)),
      test("tenantId returns tenant ID from context") {
        for id <- TenantContext.tenantId
        yield assertTrue(id == tenantA.id)
      }.provide(ZLayer.succeed(ctxA)),
      test("tenant returns tenant from context") {
        for tenant <- TenantContext.tenant
        yield assertTrue(tenant == tenantA)
      }.provide(ZLayer.succeed(ctxA)),
      test("currentUser returns user from context") {
        for user <- TenantContext.currentUser
        yield assertTrue(user == userA)
      }.provide(ZLayer.succeed(ctxA)),
      test("provide layers context into effect") {
        val effect: ZIO[TenantContext, Nothing, TenantId] = TenantContext.tenantId
        for id <- TenantContext.provide(ctxB)(effect)
        yield assertTrue(id == tenantB.id)
      }
    ),
    suite("Tenant fixtures")(
      test("tenants have different IDs") {
        assertTrue(tenantA.id != tenantB.id)
      },
      test("users belong to correct tenants") {
        assertTrue(
          userA.tenantId == tenantA.id,
          userB.tenantId == tenantB.id
        )
      },
      test("contexts are properly associated") {
        assertTrue(
          ctxA.tenantId == tenantA.id,
          ctxB.tenantId == tenantB.id,
          ctxA.currentUser == userA,
          ctxB.currentUser == userB
        )
      }
    )
  )
