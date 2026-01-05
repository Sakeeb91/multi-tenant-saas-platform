package com.multitenant.saas.database.repositories

import com.multitenant.saas.domain.ids.{ResourceId, TenantId, UserId}
import com.multitenant.saas.domain.models.Resource
import com.multitenant.saas.errors.{AppError, DatabaseError}
import com.multitenant.saas.errors.AppError.InfraError
import io.getquill.*
import io.getquill.jdbczio.Quill
import zio.*
import java.sql.Timestamp
import java.time.Instant
import java.util.UUID

/** Database row representation for resources table */
private[repositories] final case class ResourceRow(
    id: UUID,
    tenantId: UUID,
    name: String,
    data: String, // JSONB stored as String
    createdBy: UUID,
    createdAt: Timestamp,
    updatedAt: Timestamp
)

private[repositories] object ResourceRow:
  def fromDomain(r: Resource): ResourceRow =
    ResourceRow(
      id = r.id.value,
      tenantId = r.tenantId.value,
      name = r.name,
      data = mapToJson(r.data),
      createdBy = r.createdBy.value,
      createdAt = Timestamp.from(r.createdAt),
      updatedAt = Timestamp.from(r.updatedAt)
    )

  def toDomain(row: ResourceRow): Resource =
    Resource(
      id = ResourceId(row.id),
      tenantId = TenantId(row.tenantId),
      name = row.name,
      data = jsonToMap(row.data),
      createdBy = UserId(row.createdBy),
      createdAt = row.createdAt.toInstant,
      updatedAt = row.updatedAt.toInstant
    )

  private def mapToJson(map: Map[String, String]): String =
    if map.isEmpty then "{}"
    else
      map
        .map { case (k, v) => s""""$k":"$v"""" }
        .mkString("{", ",", "}")

  private def jsonToMap(json: String): Map[String, String] =
    if json == "{}" || json.isEmpty then Map.empty
    else
      // Simple JSON parsing for flat string maps
      json
        .stripPrefix("{")
        .stripSuffix("}")
        .split(",")
        .filter(_.nonEmpty)
        .map { pair =>
          val parts = pair.split(":", 2)
          val key = parts(0).trim.stripPrefix("\"").stripSuffix("\"")
          val value = parts(1).trim.stripPrefix("\"").stripSuffix("\"")
          key -> value
        }
        .toMap

final case class ResourceRepositoryLive(quill: Quill.Postgres[SnakeCase])
    extends ResourceRepository:

  import quill.*

  private inline def resources = quote(querySchema[ResourceRow]("resources"))

  override def insert(resource: Resource): IO[InfraError, Resource] =
    val row = ResourceRow.fromDomain(resource)
    run(resources.insertValue(lift(row)))
      .mapBoth(
        e => DatabaseError(e),
        _ => resource
      )

  override def findById(
      id: ResourceId,
      tenantId: TenantId
  ): IO[InfraError, Option[Resource]] =
    run(
      resources
        .filter(r => r.id == lift(id.value) && r.tenantId == lift(tenantId.value))
    ).mapBoth(
      e => DatabaseError(e),
      _.headOption.map(ResourceRow.toDomain)
    )

  override def findAll(
      tenantId: TenantId,
      limit: Int,
      offset: Int
  ): IO[InfraError, List[Resource]] =
    run(
      resources
        .filter(_.tenantId == lift(tenantId.value))
        .sortBy(_.createdAt)(Ord.desc)
        .drop(lift(offset))
        .take(lift(limit))
    ).mapBoth(
      e => DatabaseError(e),
      _.map(ResourceRow.toDomain)
    )

  override def update(resource: Resource): IO[InfraError, Resource] =
    val row = ResourceRow.fromDomain(resource)
    run(
      resources
        .filter(r =>
          r.id == lift(row.id) && r.tenantId == lift(row.tenantId)
        )
        .update(
          _.name -> lift(row.name),
          _.data -> lift(row.data),
          _.updatedAt -> lift(row.updatedAt)
        )
    ).mapBoth(
      e => DatabaseError(e),
      _ => resource
    )

  override def delete(id: ResourceId, tenantId: TenantId): IO[InfraError, Boolean] =
    run(
      resources
        .filter(r => r.id == lift(id.value) && r.tenantId == lift(tenantId.value))
        .delete
    ).mapBoth(
      e => DatabaseError(e),
      rowsAffected => rowsAffected > 0
    )

  override def count(tenantId: TenantId): IO[InfraError, Long] =
    run(
      resources.filter(_.tenantId == lift(tenantId.value)).size
    ).mapError(e => DatabaseError(e))

object ResourceRepositoryLive:
  val layer: ZLayer[Quill.Postgres[SnakeCase], Nothing, ResourceRepository] =
    ZLayer.fromFunction(ResourceRepositoryLive.apply)
