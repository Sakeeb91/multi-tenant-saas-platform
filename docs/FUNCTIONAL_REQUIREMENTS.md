# Functional Requirements (FR)

> Detailed functional requirements for the Multi-Tenant SaaS Platform

## Table of Contents

1. [Overview](#overview)
2. [User Roles](#user-roles)
3. [FR1: Tenant Management](#fr1-tenant-management)
4. [FR2: User Management](#fr2-user-management)
5. [FR3: Authentication](#fr3-authentication)
6. [FR4: Resource Management](#fr4-resource-management)
7. [FR5: Billing & Subscriptions](#fr5-billing--subscriptions)
8. [FR6: Feature Flags](#fr6-feature-flags)
9. [FR7: API Gateway](#fr7-api-gateway)
10. [API Specification](#api-specification)
11. [Traceability Matrix](#traceability-matrix)

---

## Overview

This document specifies the functional requirements for a multi-tenant SaaS platform. Each requirement is uniquely identified and traceable to implementation.

### Requirement Format

| Field | Description |
|-------|-------------|
| **ID** | Unique identifier (FR-XX-YY) |
| **Priority** | P0 (Critical), P1 (High), P2 (Medium), P3 (Low) |
| **Status** | Planned, In Progress, Implemented, Verified |
| **Phase** | Implementation phase (0-5) |

---

## User Roles

| Role | Description | Permissions |
|------|-------------|-------------|
| **Owner** | Tenant creator, full control | All operations, billing, delete tenant |
| **Admin** | Tenant administrator | Manage users, resources, settings |
| **Member** | Standard user | CRUD on resources, view settings |
| **Viewer** | Read-only access | View resources only |

---

## FR1: Tenant Management

### FR1-01: Tenant Registration

| Field | Value |
|-------|-------|
| **ID** | FR1-01 |
| **Title** | Tenant Registration |
| **Priority** | P0 |
| **Phase** | 2 |
| **Status** | Planned |

**Description:**
The system shall allow new tenants to register with a unique organization name and subdomain.

**Acceptance Criteria:**
- [ ] User can register a new tenant with name, slug, and admin email
- [ ] Slug must be unique and URL-safe (alphanumeric + hyphens)
- [ ] System creates tenant record with FREE plan by default
- [ ] System creates owner user for the registering email
- [ ] Confirmation email is sent to admin

**Input:**
```json
{
  "name": "Acme Corporation",
  "slug": "acme",
  "adminEmail": "admin@acme.com",
  "adminName": "John Doe"
}
```

**Output:**
```json
{
  "id": "uuid",
  "name": "Acme Corporation",
  "slug": "acme",
  "plan": "free",
  "createdAt": "2024-01-15T10:30:00Z"
}
```

---

### FR1-02: Tenant Subdomain Routing

| Field | Value |
|-------|-------|
| **ID** | FR1-02 |
| **Title** | Subdomain-Based Tenant Routing |
| **Priority** | P0 |
| **Phase** | 2 |
| **Status** | Planned |

**Description:**
The system shall route requests to the correct tenant based on subdomain (e.g., `acme.platform.com`).

**Acceptance Criteria:**
- [ ] Requests to `{slug}.platform.com` are routed to tenant with matching slug
- [ ] Invalid subdomains return 404 Not Found
- [ ] API clients can alternatively use `X-Tenant-ID` header

---

### FR1-03: Tenant Settings Management

| Field | Value |
|-------|-------|
| **ID** | FR1-03 |
| **Title** | Tenant Settings |
| **Priority** | P1 |
| **Phase** | 2 |
| **Status** | Planned |

**Description:**
Tenant administrators shall be able to view and update tenant settings.

**Acceptance Criteria:**
- [ ] Admins can view tenant details (name, plan, usage)
- [ ] Admins can update tenant name
- [ ] Only owners can delete tenants
- [ ] Deletion requires confirmation and is soft-delete with 30-day recovery

---

### FR1-04: Tenant Data Isolation

| Field | Value |
|-------|-------|
| **ID** | FR1-04 |
| **Title** | Data Isolation via RLS |
| **Priority** | P0 |
| **Phase** | 2 |
| **Status** | Planned |

**Description:**
The system shall ensure complete data isolation between tenants using PostgreSQL Row-Level Security.

**Acceptance Criteria:**
- [ ] All tenant-scoped tables have RLS policies enabled
- [ ] Queries automatically filter by `current_tenant_id()`
- [ ] No tenant can access another tenant's data
- [ ] RLS bypass is impossible from application code

---

## FR2: User Management

### FR2-01: User Invitation

| Field | Value |
|-------|-------|
| **ID** | FR2-01 |
| **Title** | Invite Users to Tenant |
| **Priority** | P1 |
| **Phase** | 3 |
| **Status** | Planned |

**Description:**
Tenant administrators shall be able to invite new users to their organization.

**Acceptance Criteria:**
- [ ] Admin can invite user by email with assigned role
- [ ] Invitation email contains secure one-time link
- [ ] Link expires after 7 days
- [ ] Invited user can set password or use OAuth

---

### FR2-02: User Role Management

| Field | Value |
|-------|-------|
| **ID** | FR2-02 |
| **Title** | Manage User Roles |
| **Priority** | P1 |
| **Phase** | 3 |
| **Status** | Planned |

**Description:**
Administrators shall be able to change user roles within their tenant.

**Acceptance Criteria:**
- [ ] Admin can promote/demote users (except Owner)
- [ ] Owner can transfer ownership to another Admin
- [ ] Role changes take effect immediately
- [ ] Audit log records role changes

---

### FR2-03: User Deactivation

| Field | Value |
|-------|-------|
| **ID** | FR2-03 |
| **Title** | Deactivate Users |
| **Priority** | P1 |
| **Phase** | 3 |
| **Status** | Planned |

**Description:**
Administrators shall be able to deactivate users, revoking their access.

**Acceptance Criteria:**
- [ ] Deactivated users cannot authenticate
- [ ] Active sessions are invalidated immediately
- [ ] User data is retained (not deleted)
- [ ] User can be reactivated by admin

---

## FR3: Authentication

### FR3-01: Email/Password Authentication

| Field | Value |
|-------|-------|
| **ID** | FR3-01 |
| **Title** | Email/Password Login |
| **Priority** | P0 |
| **Phase** | 3 |
| **Status** | Planned |

**Description:**
Users shall be able to authenticate using email and password.

**Acceptance Criteria:**
- [ ] Passwords are hashed using bcrypt (cost factor 12)
- [ ] Failed attempts are rate-limited (5 attempts / 15 minutes)
- [ ] Successful login returns JWT access token + refresh token
- [ ] Access token expires in 15 minutes
- [ ] Refresh token expires in 7 days

---

### FR3-02: OAuth2 Authentication (Google)

| Field | Value |
|-------|-------|
| **ID** | FR3-02 |
| **Title** | Google OAuth2 Login |
| **Priority** | P1 |
| **Phase** | 3 |
| **Status** | Planned |

**Description:**
Users shall be able to authenticate using their Google account.

**Acceptance Criteria:**
- [ ] OAuth2 flow with PKCE for security
- [ ] New users are auto-provisioned on first login
- [ ] Existing users can link Google account
- [ ] Email domain restrictions can be configured per tenant

---

### FR3-03: Token Refresh

| Field | Value |
|-------|-------|
| **ID** | FR3-03 |
| **Title** | JWT Token Refresh |
| **Priority** | P0 |
| **Phase** | 3 |
| **Status** | Planned |

**Description:**
The system shall allow users to obtain new access tokens using refresh tokens.

**Acceptance Criteria:**
- [ ] Refresh token can be exchanged for new access token
- [ ] Refresh token rotation on each use
- [ ] Old refresh tokens are invalidated
- [ ] Refresh tokens can be revoked (logout)

---

### FR3-04: Session Management

| Field | Value |
|-------|-------|
| **ID** | FR3-04 |
| **Title** | Active Session Management |
| **Priority** | P2 |
| **Phase** | 3 |
| **Status** | Planned |

**Description:**
Users shall be able to view and revoke active sessions.

**Acceptance Criteria:**
- [ ] User can list all active sessions
- [ ] Sessions show device/browser info and last activity
- [ ] User can revoke individual sessions
- [ ] User can revoke all sessions except current

---

## FR4: Resource Management

### FR4-01: Create Resource

| Field | Value |
|-------|-------|
| **ID** | FR4-01 |
| **Title** | Create Resource |
| **Priority** | P0 |
| **Phase** | 1 |
| **Status** | Implemented |

**Description:**
Authenticated users shall be able to create new resources within their tenant.

**Acceptance Criteria:**
- [x] Resource created with unique ID (UUID v4)
- [x] Resource associated with tenant and creator
- [x] Name validation (non-empty, max 255 chars)
- [x] Data stored as JSONB
- [x] Created/updated timestamps set

**API Endpoint:** `POST /api/v1/resources`

---

### FR4-02: Read Resource

| Field | Value |
|-------|-------|
| **ID** | FR4-02 |
| **Title** | Get Resource by ID |
| **Priority** | P0 |
| **Phase** | 1 |
| **Status** | Implemented |

**Description:**
Users shall be able to retrieve a specific resource by its ID.

**Acceptance Criteria:**
- [x] Returns resource if exists and belongs to tenant
- [x] Returns 404 if resource doesn't exist
- [x] RLS prevents cross-tenant access

**API Endpoint:** `GET /api/v1/resources/{id}`

---

### FR4-03: List Resources

| Field | Value |
|-------|-------|
| **ID** | FR4-03 |
| **Title** | List Resources with Pagination |
| **Priority** | P0 |
| **Phase** | 1 |
| **Status** | Implemented |

**Description:**
Users shall be able to list all resources with pagination.

**Acceptance Criteria:**
- [x] Default page size: 20
- [x] Maximum page size: 100
- [x] Sorted by createdAt DESC
- [x] Response includes total count

**API Endpoint:** `GET /api/v1/resources?limit=20&offset=0`

---

### FR4-04: Update Resource

| Field | Value |
|-------|-------|
| **ID** | FR4-04 |
| **Title** | Update Resource |
| **Priority** | P0 |
| **Phase** | 1 |
| **Status** | Implemented |

**Description:**
Users shall be able to update existing resources.

**Acceptance Criteria:**
- [x] Partial updates supported (PATCH semantics)
- [x] Only provided fields are updated
- [x] `updatedAt` timestamp is refreshed
- [x] Returns 404 if resource doesn't exist

**API Endpoint:** `PATCH /api/v1/resources/{id}`

---

### FR4-05: Delete Resource

| Field | Value |
|-------|-------|
| **ID** | FR4-05 |
| **Title** | Delete Resource |
| **Priority** | P0 |
| **Phase** | 1 |
| **Status** | Implemented |

**Description:**
Users shall be able to delete resources.

**Acceptance Criteria:**
- [x] Hard delete (no soft delete for resources)
- [x] Returns 204 No Content on success
- [x] Returns 404 if resource doesn't exist
- [x] Idempotent operation

**API Endpoint:** `DELETE /api/v1/resources/{id}`

---

## FR5: Billing & Subscriptions

### FR5-01: Subscription Plans

| Field | Value |
|-------|-------|
| **ID** | FR5-01 |
| **Title** | Subscription Plan Tiers |
| **Priority** | P0 |
| **Phase** | 4 |
| **Status** | Planned |

**Description:**
The system shall support multiple subscription plan tiers.

**Plan Comparison:**

| Feature | Free | Starter | Professional | Enterprise |
|---------|------|---------|--------------|------------|
| Users | 3 | 10 | 50 | Unlimited |
| Resources | 100 | 1,000 | 10,000 | Unlimited |
| API Calls/month | 1,000 | 10,000 | 100,000 | Unlimited |
| Support | Community | Email | Priority | Dedicated |
| SSO | No | No | Yes | Yes |
| Audit Logs | No | No | Yes | Yes |
| Price | $0 | $29/mo | $99/mo | Custom |

---

### FR5-02: Stripe Checkout

| Field | Value |
|-------|-------|
| **ID** | FR5-02 |
| **Title** | Subscription Checkout |
| **Priority** | P0 |
| **Phase** | 4 |
| **Status** | Planned |

**Description:**
Tenant owners shall be able to subscribe to paid plans via Stripe Checkout.

**Acceptance Criteria:**
- [ ] Owner initiates checkout from billing page
- [ ] Redirect to Stripe Checkout with plan selection
- [ ] Webhook updates tenant plan on successful payment
- [ ] Billing portal access for payment method management

---

### FR5-03: Usage-Based Billing

| Field | Value |
|-------|-------|
| **ID** | FR5-03 |
| **Title** | API Usage Metering |
| **Priority** | P1 |
| **Phase** | 4 |
| **Status** | Planned |

**Description:**
The system shall track and bill for API usage beyond plan limits.

**Acceptance Criteria:**
- [ ] API calls are counted per tenant per billing period
- [ ] Usage events sent to Stripe for metered billing
- [ ] Dashboard shows current usage vs. plan limits
- [ ] Alerts when approaching limits (80%, 100%)

---

### FR5-04: Subscription Management

| Field | Value |
|-------|-------|
| **ID** | FR5-04 |
| **Title** | Upgrade/Downgrade Subscription |
| **Priority** | P1 |
| **Phase** | 4 |
| **Status** | Planned |

**Description:**
Tenant owners shall be able to change their subscription plan.

**Acceptance Criteria:**
- [ ] Upgrades take effect immediately
- [ ] Upgrades are prorated
- [ ] Downgrades take effect at period end
- [ ] Downgrade validates resource limits

---

### FR5-05: Subscription Cancellation

| Field | Value |
|-------|-------|
| **ID** | FR5-05 |
| **Title** | Cancel Subscription |
| **Priority** | P1 |
| **Phase** | 4 |
| **Status** | Planned |

**Description:**
Tenant owners shall be able to cancel their subscription.

**Acceptance Criteria:**
- [ ] Cancellation takes effect at period end
- [ ] Tenant retains access until period end
- [ ] Tenant reverts to Free plan after cancellation
- [ ] Data is retained (not deleted)

---

## FR6: Feature Flags

### FR6-01: Plan-Based Feature Gating

| Field | Value |
|-------|-------|
| **ID** | FR6-01 |
| **Title** | Gate Features by Plan |
| **Priority** | P1 |
| **Phase** | 3 |
| **Status** | Planned |

**Description:**
The system shall enable/disable features based on the tenant's subscription plan.

**Acceptance Criteria:**
- [ ] Features can be configured with allowed plans
- [ ] Feature check evaluates tenant's current plan
- [ ] Upgrade prompts for gated features
- [ ] Grace period after downgrade (7 days)

---

### FR6-02: Percentage Rollout

| Field | Value |
|-------|-------|
| **ID** | FR6-02 |
| **Title** | Gradual Feature Rollout |
| **Priority** | P2 |
| **Phase** | 3 |
| **Status** | Planned |

**Description:**
The system shall support percentage-based feature rollouts.

**Acceptance Criteria:**
- [ ] Feature can be enabled for X% of tenants
- [ ] Consistent assignment (same tenant always gets same result)
- [ ] Percentage can be adjusted without resetting assignments
- [ ] Metrics track adoption

---

### FR6-03: Tenant Overrides

| Field | Value |
|-------|-------|
| **ID** | FR6-03 |
| **Title** | Per-Tenant Feature Overrides |
| **Priority** | P2 |
| **Phase** | 3 |
| **Status** | Planned |

**Description:**
Administrators shall be able to enable/disable features for specific tenants.

**Acceptance Criteria:**
- [ ] Override takes precedence over plan-based rules
- [ ] Beta tenants can access unreleased features
- [ ] Features can be disabled for problematic tenants
- [ ] Overrides are audited

---

## FR7: API Gateway

### FR7-01: Health Checks

| Field | Value |
|-------|-------|
| **ID** | FR7-01 |
| **Title** | Health and Readiness Endpoints |
| **Priority** | P0 |
| **Phase** | 0 |
| **Status** | Implemented |

**Description:**
The system shall expose health check endpoints for monitoring and orchestration.

**Acceptance Criteria:**
- [x] `/health` returns 200 if service is alive
- [x] `/ready` returns 200 if all dependencies are connected
- [x] Failed dependency check returns 503

---

### FR7-02: Request Validation

| Field | Value |
|-------|-------|
| **ID** | FR7-02 |
| **Title** | Input Validation |
| **Priority** | P0 |
| **Phase** | 1 |
| **Status** | Implemented |

**Description:**
The system shall validate all incoming request data.

**Acceptance Criteria:**
- [x] JSON parsing errors return 400 Bad Request
- [x] Validation errors include field-level details
- [x] UUID parsing errors are handled gracefully
- [x] Query parameters are validated

---

### FR7-03: Error Responses

| Field | Value |
|-------|-------|
| **ID** | FR7-03 |
| **Title** | Consistent Error Format |
| **Priority** | P0 |
| **Phase** | 1 |
| **Status** | Implemented |

**Description:**
The system shall return errors in a consistent JSON format.

**Error Response Format:**
```json
{
  "error": "not_found",
  "code": "RESOURCE_NOT_FOUND",
  "message": "Resource with id abc-123 not found",
  "details": {}
}
```

**HTTP Status Mapping:**

| Error Type | HTTP Status |
|------------|-------------|
| Validation | 400 |
| Authentication | 401 |
| Authorization | 403 |
| Not Found | 404 |
| Conflict | 409 |
| Rate Limit | 429 |
| Server Error | 500 |

---

## API Specification

### Base URL

```
Production: https://api.platform.com/api/v1
Staging: https://api.staging.platform.com/api/v1
```

### Authentication

All endpoints except `/health`, `/ready`, and `/auth/*` require authentication.

```
Authorization: Bearer <jwt_token>
```

### Endpoints Summary

| Method | Path | Description | Auth | Phase |
|--------|------|-------------|------|-------|
| GET | `/health` | Health check | No | 0 |
| GET | `/ready` | Readiness check | No | 0 |
| POST | `/auth/register` | Register tenant | No | 2 |
| POST | `/auth/login` | Login | No | 3 |
| POST | `/auth/refresh` | Refresh token | No | 3 |
| GET | `/auth/google` | OAuth initiate | No | 3 |
| GET | `/auth/callback` | OAuth callback | No | 3 |
| GET | `/api/v1/tenants/me` | Current tenant | Yes | 2 |
| PATCH | `/api/v1/tenants/me` | Update tenant | Yes | 2 |
| GET | `/api/v1/users` | List users | Yes | 3 |
| POST | `/api/v1/users/invite` | Invite user | Yes | 3 |
| PATCH | `/api/v1/users/{id}` | Update user | Yes | 3 |
| DELETE | `/api/v1/users/{id}` | Deactivate user | Yes | 3 |
| GET | `/api/v1/resources` | List resources | Yes | 1 |
| POST | `/api/v1/resources` | Create resource | Yes | 1 |
| GET | `/api/v1/resources/{id}` | Get resource | Yes | 1 |
| PATCH | `/api/v1/resources/{id}` | Update resource | Yes | 1 |
| DELETE | `/api/v1/resources/{id}` | Delete resource | Yes | 1 |
| GET | `/api/v1/billing/subscription` | Get subscription | Yes | 4 |
| POST | `/api/v1/billing/checkout` | Create checkout | Yes | 4 |
| POST | `/api/v1/billing/portal` | Billing portal | Yes | 4 |
| POST | `/webhooks/stripe` | Stripe webhooks | No* | 4 |

*Stripe webhooks are verified via signature

---

## Traceability Matrix

### Requirements to Implementation Phase

| Requirement | Phase | Module | Status |
|-------------|-------|--------|--------|
| FR1-01 | 2 | api, database | Planned |
| FR1-02 | 2 | api | Planned |
| FR1-03 | 2 | api, database | Planned |
| FR1-04 | 2 | database | Planned |
| FR2-01 | 3 | auth, api | Planned |
| FR2-02 | 3 | auth, api | Planned |
| FR2-03 | 3 | auth, api | Planned |
| FR3-01 | 3 | auth | Planned |
| FR3-02 | 3 | auth | Planned |
| FR3-03 | 3 | auth | Planned |
| FR3-04 | 3 | auth, api | Planned |
| FR4-01 | 1 | core, api | Implemented |
| FR4-02 | 1 | core, api | Implemented |
| FR4-03 | 1 | core, api | Implemented |
| FR4-04 | 1 | core, api | Implemented |
| FR4-05 | 1 | core, api | Implemented |
| FR5-01 | 4 | billing | Planned |
| FR5-02 | 4 | billing | Planned |
| FR5-03 | 4 | billing | Planned |
| FR5-04 | 4 | billing | Planned |
| FR5-05 | 4 | billing | Planned |
| FR6-01 | 3 | features | Planned |
| FR6-02 | 3 | features | Planned |
| FR6-03 | 3 | features | Planned |
| FR7-01 | 0 | api | Implemented |
| FR7-02 | 1 | api | Implemented |
| FR7-03 | 1 | api | Implemented |

---

## Revision History

| Version | Date | Author | Changes |
|---------|------|--------|---------|
| 1.0 | 2024-01-15 | - | Initial version |
