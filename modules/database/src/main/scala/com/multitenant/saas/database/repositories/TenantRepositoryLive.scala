package com.multitenant.saas.database.repositories

import com.multitenant.saas.domain.ids.TenantId
import com.multitenant.saas.domain.models.Tenant
import com.multitenant.saas.domain.enums.Plan
import com.multitenant.saas.errors.AppError.{DatabaseError, InfraError}
import io.getquill.*
import io.getquill.jdbczio.Quill
import zio.*
import java.sql.Timestamp
import java.time.Instant
import java.util.UUID

/** Database row representation for tenants table */
private[repositories] final case class TenantRow(
    id: UUID,
    name: String,
    slug: String,
    plan: String,
    createdAt: Timestamp,
    updatedAt: Timestamp
)

private[repositories] object TenantRow:
  def fromDomain(t: Tenant): TenantRow =
    TenantRow(
      id = t.id.value,
      name = t.name,
      slug = t.slug,
      plan = t.plan.asString,
      createdAt = Timestamp.from(t.createdAt),
      updatedAt = Timestamp.from(t.updatedAt)
    )

  def toDomain(row: TenantRow): Either[String, Tenant] =
    Plan.fromString(row.plan).map { plan =>
      Tenant(
        id = TenantId(row.id),
        name = row.name,
        slug = row.slug,
        plan = plan,
        createdAt = row.createdAt.toInstant,
        updatedAt = row.updatedAt.toInstant
      )
    }

final case class TenantRepositoryLive(quill: Quill.Postgres[SnakeCase])
    extends TenantRepository:

  import quill.*

  private inline def tenants = quote(querySchema[TenantRow]("tenants"))

  override def insert(tenant: Tenant): IO[InfraError, Tenant] =
    val row = TenantRow.fromDomain(tenant)
    run(tenants.insertValue(lift(row)))
      .mapBoth(
        e => DatabaseError(e),
        _ => tenant
      )

  override def findById(id: TenantId): IO[InfraError, Option[Tenant]] =
    run(tenants.filter(_.id == lift(id.value)))
      .mapBoth(
        e => DatabaseError(e),
        rows =>
          rows.headOption.flatMap(row => TenantRow.toDomain(row).toOption)
      )

  override def findBySlug(slug: String): IO[InfraError, Option[Tenant]] =
    run(tenants.filter(_.slug == lift(slug)))
      .mapBoth(
        e => DatabaseError(e),
        rows =>
          rows.headOption.flatMap(row => TenantRow.toDomain(row).toOption)
      )

  override def findAll(limit: Int, offset: Int): IO[InfraError, List[Tenant]] =
    run(
      tenants
        .sortBy(_.createdAt)(Ord.desc)
        .drop(lift(offset))
        .take(lift(limit))
    ).mapBoth(
      e => DatabaseError(e),
      rows => rows.flatMap(row => TenantRow.toDomain(row).toOption)
    )

  override def update(tenant: Tenant): IO[InfraError, Tenant] =
    val row = TenantRow.fromDomain(tenant)
    run(
      tenants
        .filter(_.id == lift(row.id))
        .update(
          _.name -> lift(row.name),
          _.slug -> lift(row.slug),
          _.plan -> lift(row.plan),
          _.updatedAt -> lift(row.updatedAt)
        )
    ).mapBoth(
      e => DatabaseError(e),
      _ => tenant
    )

  override def updatePlan(id: TenantId, plan: Plan): IO[InfraError, Boolean] =
    val now = Timestamp.from(Instant.now)
    run(
      tenants
        .filter(_.id == lift(id.value))
        .update(
          _.plan -> lift(plan.asString),
          _.updatedAt -> lift(now)
        )
    ).mapBoth(
      e => DatabaseError(e),
      rowsAffected => rowsAffected > 0
    )

  override def delete(id: TenantId): IO[InfraError, Boolean] =
    run(tenants.filter(_.id == lift(id.value)).delete)
      .mapBoth(
        e => DatabaseError(e),
        rowsAffected => rowsAffected > 0
      )

  override def count: IO[InfraError, Long] =
    run(tenants.size).mapError(e => DatabaseError(e))

object TenantRepositoryLive:
  val layer: ZLayer[Quill.Postgres[SnakeCase], Nothing, TenantRepository] =
    ZLayer.fromFunction(TenantRepositoryLive.apply)
