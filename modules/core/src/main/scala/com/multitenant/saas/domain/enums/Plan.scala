package com.multitenant.saas.domain.enums

/** Subscription plan tiers available to tenants */
enum Plan:
  case Free, Starter, Professional, Enterprise

object Plan:
  def fromString(s: String): Either[String, Plan] =
    s.toLowerCase match
      case "free"         => Right(Plan.Free)
      case "starter"      => Right(Plan.Starter)
      case "professional" => Right(Plan.Professional)
      case "enterprise"   => Right(Plan.Enterprise)
      case _              => Left(s"Invalid plan: $s")

  extension (plan: Plan)
    def asString: String = plan match
      case Plan.Free         => "free"
      case Plan.Starter      => "starter"
      case Plan.Professional => "professional"
      case Plan.Enterprise   => "enterprise"
