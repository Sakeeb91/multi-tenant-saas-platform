package com.multitenant.saas.context

import com.multitenant.saas.domain.ids.TenantId
import com.multitenant.saas.domain.models.{Tenant, User}
import com.multitenant.saas.domain.enums.Role
import zio.*

/**
 * TenantContext flows through every request in the ZIO environment.
 * This provides compile-time safety that all tenant-scoped operations
 * have access to tenant information.
 */
final case class TenantContext(
    tenantId: TenantId,
    tenant: Tenant,
    currentUser: User
):
  /** Check if user has required role */
  def hasRole(required: Role): Boolean =
    roleHierarchy(currentUser.role) >= roleHierarchy(required)

  /** Check if user can perform admin operations */
  def isAdmin: Boolean = hasRole(Role.Admin)

  /** Check if user is tenant owner */
  def isOwner: Boolean = currentUser.role == Role.Owner

  private def roleHierarchy(role: Role): Int = role match
    case Role.Viewer => 0
    case Role.Member => 1
    case Role.Admin  => 2
    case Role.Owner  => 3

object TenantContext:
  /** Get the full tenant context from the environment */
  def get: URIO[TenantContext, TenantContext] = ZIO.service[TenantContext]

  /** Get just the tenant ID from the context */
  def tenantId: URIO[TenantContext, TenantId] = get.map(_.tenantId)

  /** Get the tenant from the context */
  def tenant: URIO[TenantContext, Tenant] = get.map(_.tenant)

  /** Get the current user from the context */
  def currentUser: URIO[TenantContext, User] = get.map(_.currentUser)

  /** Run effect with tenant context in scope */
  def provide[R, E, A](ctx: TenantContext)(
      effect: ZIO[R & TenantContext, E, A]
  ): ZIO[R, E, A] =
    effect.provideSomeLayer(ZLayer.succeed(ctx))

/** Type alias for tenant-scoped effects */
type TenantIO[+E, +A] = ZIO[TenantContext, E, A]
