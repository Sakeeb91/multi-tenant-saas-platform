package com.multitenant.saas.services

import com.multitenant.saas.domain.ids.*
import com.multitenant.saas.domain.models.Resource
import com.multitenant.saas.errors.AppError
import com.multitenant.saas.errors.AppError.{InfraError, NotFound}
import com.multitenant.saas.database.repositories.ResourceRepository
import zio.*
import zio.test.*
import java.time.Instant

object ResourceServiceSpec extends ZIOSpecDefault:

  // In-memory repository for testing
  private class TestRepository extends ResourceRepository:
    private val resources = scala.collection.mutable.Map[String, Resource]()

    override def insert(resource: Resource): IO[InfraError, Resource] =
      ZIO.succeed {
        resources.put(resource.id.asString, resource)
        resource
      }

    override def findById(
        id: ResourceId,
        tenantId: TenantId
    ): IO[InfraError, Option[Resource]] =
      ZIO.succeed(
        resources.get(id.asString).filter(_.tenantId.asString == tenantId.asString)
      )

    override def findAll(
        tenantId: TenantId,
        limit: Int,
        offset: Int
    ): IO[InfraError, List[Resource]] =
      ZIO.succeed(
        resources.values
          .filter(_.tenantId.asString == tenantId.asString)
          .toList
          .sortBy(_.createdAt)(Ordering[Instant].reverse)
          .drop(offset)
          .take(limit)
      )

    override def update(resource: Resource): IO[InfraError, Resource] =
      ZIO.succeed {
        resources.put(resource.id.asString, resource)
        resource
      }

    override def delete(id: ResourceId, tenantId: TenantId): IO[InfraError, Boolean] =
      ZIO.succeed {
        val key = id.asString
        if resources.contains(key) && resources(key).tenantId.asString == tenantId.asString
        then
          resources.remove(key)
          true
        else false
      }

    override def count(tenantId: TenantId): IO[InfraError, Long] =
      ZIO.succeed(
        resources.values.count(_.tenantId.asString == tenantId.asString).toLong
      )

  private val testLayer =
    ZLayer.succeed(new TestRepository: ResourceRepository) >>> ResourceServiceLive.layer

  def spec = suite("ResourceService")(
    suite("create")(
      test("creates resource with valid name") {
        val tenantId = TenantId.generate
        val userId = UserId.generate
        for
          resource <- ResourceService.create(tenantId, userId, "test-resource", Map.empty)
        yield assertTrue(
          resource.name == "test-resource",
          resource.tenantId.asString == tenantId.asString
        )
      }.provide(testLayer),
      test("fails with empty name") {
        val tenantId = TenantId.generate
        val userId = UserId.generate
        for
          result <- ResourceService.create(tenantId, userId, "", Map.empty).either
        yield assertTrue(result.isLeft)
      }.provide(testLayer),
      test("fails with name exceeding 255 characters") {
        val tenantId = TenantId.generate
        val userId = UserId.generate
        val longName = "a" * 256
        for
          result <- ResourceService.create(tenantId, userId, longName, Map.empty).either
        yield assertTrue(result.isLeft)
      }.provide(testLayer),
      test("fails with invalid characters in name") {
        val tenantId = TenantId.generate
        val userId = UserId.generate
        for
          result <- ResourceService.create(tenantId, userId, "test@resource!", Map.empty).either
        yield assertTrue(result.isLeft)
      }.provide(testLayer)
    ),
    suite("get")(
      test("returns resource when found") {
        val tenantId = TenantId.generate
        val userId = UserId.generate
        for
          created <- ResourceService.create(tenantId, userId, "my-resource", Map.empty)
          found <- ResourceService.get(tenantId, created.id)
        yield assertTrue(found.id.asString == created.id.asString)
      }.provide(testLayer),
      test("fails when resource not found") {
        val tenantId = TenantId.generate
        val resourceId = ResourceId.generate
        for
          result <- ResourceService.get(tenantId, resourceId).either
        yield assertTrue(
          result.isLeft,
          result.left.exists(_.isInstanceOf[NotFound])
        )
      }.provide(testLayer)
    ),
    suite("list")(
      test("enforces pagination limits") {
        val tenantId = TenantId.generate
        val userId = UserId.generate
        for
          _ <- ResourceService.create(tenantId, userId, "resource1", Map.empty)
          _ <- ResourceService.create(tenantId, userId, "resource2", Map.empty)
          result <- ResourceService.list(tenantId, limit = 1, offset = 0)
        yield assertTrue(result.size == 1)
      }.provide(testLayer)
    ),
    suite("update")(
      test("updates resource name") {
        val tenantId = TenantId.generate
        val userId = UserId.generate
        for
          created <- ResourceService.create(tenantId, userId, "original", Map.empty)
          updated <- ResourceService.update(tenantId, created.id, Some("updated"), None)
        yield assertTrue(updated.name == "updated")
      }.provide(testLayer)
    ),
    suite("delete")(
      test("deletes existing resource") {
        val tenantId = TenantId.generate
        val userId = UserId.generate
        for
          created <- ResourceService.create(tenantId, userId, "to-delete", Map.empty)
          _ <- ResourceService.delete(tenantId, created.id)
          result <- ResourceService.get(tenantId, created.id).either
        yield assertTrue(result.isLeft)
      }.provide(testLayer)
    )
  )
