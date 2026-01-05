package com.multitenant.saas.database

import com.multitenant.saas.domain.ids.TenantId
import com.multitenant.saas.errors.AppError.DatabaseError
import zio.*
import javax.sql.DataSource
import java.sql.Connection

/**
 * Live implementation of TenantAwareDataSource.
 *
 * Sets PostgreSQL session variable 'app.current_tenant_id' before
 * executing queries to enable Row-Level Security policies.
 */
final case class TenantAwareDataSourceLive(dataSource: DataSource)
    extends TenantAwareDataSource:

  override def withTenantConnection[A](tenantId: TenantId)(
      f: Connection => Task[A]
  ): IO[DatabaseError, A] =
    ZIO.scoped {
      for
        conn <- acquireConnection
        _ <- setTenantContext(conn, tenantId)
        result <- f(conn).mapError(e => DatabaseError(e))
        _ <- clearTenantContext(conn)
      yield result
    }

  override def withAdminConnection[A](
      f: Connection => Task[A]
  ): IO[DatabaseError, A] =
    ZIO.scoped {
      for
        conn <- acquireConnection
        result <- f(conn).mapError(e => DatabaseError(e))
      yield result
    }

  private def acquireConnection: ZIO[Scope, DatabaseError, Connection] =
    ZIO.acquireRelease(
      ZIO
        .attemptBlocking(dataSource.getConnection)
        .mapError(e => DatabaseError(e))
    )(conn => ZIO.attemptBlocking(conn.close()).orDie)

  private def setTenantContext(
      conn: Connection,
      tenantId: TenantId
  ): IO[DatabaseError, Unit] =
    ZIO
      .attemptBlocking {
        val stmt = conn.createStatement()
        try
          // Use SET LOCAL so it's scoped to the current transaction
          // The UUID is already validated by TenantId type
          val _ = stmt.execute(
            s"SET LOCAL app.current_tenant_id = '${tenantId.value}'"
          )
        finally stmt.close()
      }
      .mapError(e => DatabaseError(e))

  private def clearTenantContext(conn: Connection): UIO[Unit] =
    ZIO
      .attemptBlocking {
        val stmt = conn.createStatement()
        try
          val _ = stmt.execute("RESET app.current_tenant_id")
        finally stmt.close()
      }
      .orDie

object TenantAwareDataSourceLive:
  val layer: ZLayer[DataSource, Nothing, TenantAwareDataSource] =
    ZLayer.fromFunction(TenantAwareDataSourceLive.apply)
