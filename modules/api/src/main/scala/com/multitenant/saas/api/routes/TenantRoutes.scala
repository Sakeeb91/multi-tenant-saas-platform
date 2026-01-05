package com.multitenant.saas.api.routes

import com.multitenant.saas.api.dto.*
import com.multitenant.saas.domain.ids.TenantId
import com.multitenant.saas.domain.enums.Plan
import com.multitenant.saas.services.TenantService
import com.multitenant.saas.errors.AppError
import com.multitenant.saas.errors.AppError.*
import zio.*
import zio.http.*
import zio.json.*

/**
 * HTTP routes for tenant management.
 *
 * These endpoints are typically admin-only and operate outside
 * the normal tenant context (cross-tenant operations).
 */
object TenantRoutes:

  def routes: Routes[TenantService, Nothing] =
    Routes(
      // POST /api/v1/tenants
      Method.POST / "api" / "v1" / "tenants" -> handler { (req: Request) =>
        handleCreate(req)
      },
      // GET /api/v1/tenants/:id
      Method.GET / "api" / "v1" / "tenants" / string("id") -> handler {
        (id: String, _: Request) =>
          handleGet(id)
      },
      // GET /api/v1/tenants
      Method.GET / "api" / "v1" / "tenants" -> handler { (req: Request) =>
        handleList(req)
      },
      // PATCH /api/v1/tenants/:id/plan
      Method.PATCH / "api" / "v1" / "tenants" / string("id") / "plan" -> handler {
        (id: String, req: Request) =>
          handleUpdatePlan(id, req)
      }
    )

  private def handleCreate(
      req: Request
  ): ZIO[TenantService, Nothing, Response] =
    (for
      body <- req.body.asString.orElseFail(badRequest("Invalid request body"))
      parsed <- ZIO
        .fromEither(body.fromJson[CreateTenantRequest])
        .mapError(err => badRequest(s"Invalid JSON: $err"))
      tenant <- TenantService
        .create(parsed.name, parsed.slug)
        .mapError(toResponse)
      response = TenantResponse.from(tenant)
    yield Response.json(response.toJson).status(Status.Created)).merge

  private def handleGet(id: String): ZIO[TenantService, Nothing, Response] =
    (for
      tenant <- TenantService.getBySlugOrId(id).mapError(toResponse)
      response = TenantResponse.from(tenant)
    yield Response.json(response.toJson)).merge

  private def handleList(
      req: Request
  ): ZIO[TenantService, Nothing, Response] =
    (for
      limit <- ZIO.succeed(
        req.url.queryParams
          .get("limit")
          .flatMap(_.headOption)
          .flatMap(_.toIntOption)
          .getOrElse(20)
      )
      offset <- ZIO.succeed(
        req.url.queryParams
          .get("offset")
          .flatMap(_.headOption)
          .flatMap(_.toIntOption)
          .getOrElse(0)
      )
      tenants <- TenantService.list(limit, offset).mapError(toResponse)
      response = TenantListResponse(
        items = tenants.map(TenantResponse.from),
        count = tenants.size,
        limit = limit,
        offset = offset
      )
    yield Response.json(response.toJson)).merge

  private def handleUpdatePlan(
      id: String,
      req: Request
  ): ZIO[TenantService, Nothing, Response] =
    (for
      tenantId <- parseTenantId(id)
      body <- req.body.asString.orElseFail(badRequest("Invalid request body"))
      parsed <- ZIO
        .fromEither(body.fromJson[UpdateTenantPlanRequest])
        .mapError(err => badRequest(s"Invalid JSON: $err"))
      plan <- ZIO
        .fromEither(Plan.fromString(parsed.plan))
        .mapError(err => badRequest(err))
      tenant <- TenantService.updatePlan(tenantId, plan).mapError(toResponse)
      response = TenantResponse.from(tenant)
    yield Response.json(response.toJson)).merge

  private def parseTenantId(id: String): IO[Response, TenantId] =
    ZIO
      .fromEither(TenantId.fromString(id))
      .mapError(err => badRequest(err))

  private def toResponse(error: AppError): Response =
    val (status, errorResponse) = error match
      case e: NotFound =>
        (Status.NotFound, ErrorResponse("not_found", e.code, e.message))
      case e: AlreadyExists =>
        (Status.Conflict, ErrorResponse("conflict", e.code, e.message))
      case e: ValidationError =>
        (Status.BadRequest, ErrorResponse("validation_error", e.code, e.message))
      case e: DatabaseError =>
        (
          Status.InternalServerError,
          ErrorResponse("internal_error", e.code, "An internal error occurred")
        )
      case e =>
        (
          Status.InternalServerError,
          ErrorResponse("internal_error", e.code, "An internal error occurred")
        )
    Response.json(errorResponse.toJson).status(status)

  private def badRequest(message: String): Response =
    Response
      .json(ErrorResponse("bad_request", "BAD_REQUEST", message).toJson)
      .status(Status.BadRequest)
