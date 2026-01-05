package com.multitenant.saas.domain.enums

/** User roles within a tenant with hierarchical permissions */
enum Role:
  case Owner, Admin, Member, Viewer

object Role:
  def fromString(s: String): Either[String, Role] =
    s.toLowerCase match
      case "owner"  => Right(Role.Owner)
      case "admin"  => Right(Role.Admin)
      case "member" => Right(Role.Member)
      case "viewer" => Right(Role.Viewer)
      case _        => Left(s"Invalid role: $s")

  extension (role: Role)
    def asString: String = role match
      case Role.Owner  => "owner"
      case Role.Admin  => "admin"
      case Role.Member => "member"
      case Role.Viewer => "viewer"

    /** Check if this role has at least the permissions of the required role */
    def hasPermission(required: Role): Boolean =
      (role, required) match
        case (Role.Owner, _)                    => true
        case (Role.Admin, Role.Owner)           => false
        case (Role.Admin, _)                    => true
        case (Role.Member, Role.Owner | Role.Admin) => false
        case (Role.Member, _)                   => true
        case (Role.Viewer, Role.Viewer)         => true
        case (Role.Viewer, _)                   => false
