package com.multitenant.saas.api.routes

import com.multitenant.saas.api.dto.*
import com.multitenant.saas.domain.ids.{ResourceId, TenantId, UserId}
import com.multitenant.saas.services.ResourceService
import com.multitenant.saas.errors.AppError
import com.multitenant.saas.errors.AppError.*
import zio.*
import zio.http.*
import zio.json.*

object ResourceRoutes:

  def routes: Routes[ResourceService, Nothing] =
    Routes(
      // POST /api/v1/resources
      Method.POST / "api" / "v1" / "resources" -> handler { (req: Request) =>
        handleCreate(req)
      },
      // GET /api/v1/resources/:id
      Method.GET / "api" / "v1" / "resources" / string("id") -> handler {
        (id: String, _: Request) =>
          handleGet(id)
      },
      // GET /api/v1/resources
      Method.GET / "api" / "v1" / "resources" -> handler { (req: Request) =>
        handleList(req)
      },
      // PATCH /api/v1/resources/:id
      Method.PATCH / "api" / "v1" / "resources" / string("id") -> handler {
        (id: String, req: Request) =>
          handleUpdate(id, req)
      },
      // DELETE /api/v1/resources/:id
      Method.DELETE / "api" / "v1" / "resources" / string("id") -> handler {
        (id: String, _: Request) =>
          handleDelete(id)
      }
    )

  private def handleCreate(
      req: Request
  ): ZIO[ResourceService, Nothing, Response] =
    (for
      body <- req.body.asString.orElseFail(badRequest("Invalid request body"))
      parsed <- ZIO
        .fromEither(body.fromJson[CreateResourceRequest])
        .mapError(err => badRequest(s"Invalid JSON: $err"))
      // TODO: Extract from auth context
      tenantId = TenantId.generate
      userId = UserId.generate
      resource <- ResourceService
        .create(tenantId, userId, parsed.name, parsed.data.getOrElse(Map.empty))
        .mapError(toResponse)
      response = ResourceResponse.from(resource)
    yield Response
      .json(response.toJson)
      .status(Status.Created)).merge

  private def handleGet(id: String): ZIO[ResourceService, Nothing, Response] =
    (for
      resourceId <- parseResourceId(id)
      // TODO: Extract from auth context
      tenantId = TenantId.generate
      resource <- ResourceService.get(tenantId, resourceId).mapError(toResponse)
      response = ResourceResponse.from(resource)
    yield Response.json(response.toJson)).merge

  private def handleList(
      req: Request
  ): ZIO[ResourceService, Nothing, Response] =
    (for
      limit <- ZIO.succeed(
        req.url.queryParams.getAll("limit").headOption.flatMap(_.toIntOption).getOrElse(20)
      )
      offset <- ZIO.succeed(
        req.url.queryParams.getAll("offset").headOption.flatMap(_.toIntOption).getOrElse(0)
      )
      // TODO: Extract from auth context
      tenantId = TenantId.generate
      resources <- ResourceService
        .list(tenantId, limit, offset)
        .mapError(toResponse)
      response = ResourceListResponse(
        items = resources.map(ResourceResponse.from),
        count = resources.size,
        limit = limit,
        offset = offset
      )
    yield Response.json(response.toJson)).merge

  private def handleUpdate(
      id: String,
      req: Request
  ): ZIO[ResourceService, Nothing, Response] =
    (for
      resourceId <- parseResourceId(id)
      body <- req.body.asString.orElseFail(badRequest("Invalid request body"))
      parsed <- ZIO
        .fromEither(body.fromJson[UpdateResourceRequest])
        .mapError(err => badRequest(s"Invalid JSON: $err"))
      // TODO: Extract from auth context
      tenantId = TenantId.generate
      resource <- ResourceService
        .update(tenantId, resourceId, parsed.name, parsed.data)
        .mapError(toResponse)
      response = ResourceResponse.from(resource)
    yield Response.json(response.toJson)).merge

  private def handleDelete(
      id: String
  ): ZIO[ResourceService, Nothing, Response] =
    (for
      resourceId <- parseResourceId(id)
      // TODO: Extract from auth context
      tenantId = TenantId.generate
      _ <- ResourceService.delete(tenantId, resourceId).mapError(toResponse)
    yield Response.status(Status.NoContent)).merge

  private def parseResourceId(id: String): IO[Response, ResourceId] =
    ZIO
      .fromEither(ResourceId.fromString(id))
      .mapError(err => badRequest(err))

  private def toResponse(error: AppError): Response =
    val (status, errorResponse) = error match
      case e: NotFound =>
        (Status.NotFound, ErrorResponse("not_found", e.code, e.message))
      case e: AlreadyExists =>
        (Status.Conflict, ErrorResponse("conflict", e.code, e.message))
      case e: ValidationError =>
        (Status.BadRequest, ErrorResponse("validation_error", e.code, e.message))
      case InvalidToken =>
        (Status.Unauthorized, ErrorResponse("unauthorized", InvalidToken.code, InvalidToken.message))
      case InsufficientPermissions =>
        (
          Status.Forbidden,
          ErrorResponse("forbidden", InsufficientPermissions.code, InsufficientPermissions.message)
        )
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
