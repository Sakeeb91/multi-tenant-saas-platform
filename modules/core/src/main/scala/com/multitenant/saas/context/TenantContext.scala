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
