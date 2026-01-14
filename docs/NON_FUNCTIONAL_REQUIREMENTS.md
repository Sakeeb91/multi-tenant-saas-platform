# Non-Functional Requirements (NFR)

> Quality attributes and constraints for the Multi-Tenant SaaS Platform

## Table of Contents

1. [Overview](#overview)
2. [NFR1: Performance](#nfr1-performance)
3. [NFR2: Scalability](#nfr2-scalability)
4. [NFR3: Security](#nfr3-security)
5. [NFR4: Reliability](#nfr4-reliability)
6. [NFR5: Observability](#nfr5-observability)
7. [NFR6: Maintainability](#nfr6-maintainability)
8. [NFR7: Usability](#nfr7-usability)
9. [NFR8: Compliance](#nfr8-compliance)
10. [NFR9: Operational](#nfr9-operational)
11. [Constraints](#constraints)
12. [Quality Metrics Dashboard](#quality-metrics-dashboard)

---

## Overview

This document specifies the non-functional requirements (quality attributes) for the Multi-Tenant SaaS Platform. These requirements define how the system should perform, rather than what it should do.

### Requirement Priority

| Priority | Description |
|----------|-------------|
| **P0** | Critical - System cannot launch without meeting this |
| **P1** | High - Required for production readiness |
| **P2** | Medium - Important for user experience |
| **P3** | Low - Nice to have |

---

## NFR1: Performance

### NFR1-01: API Response Time

| Field | Value |
|-------|-------|
| **ID** | NFR1-01 |
| **Category** | Performance |
| **Priority** | P0 |

**Requirement:**
API endpoints shall meet the following response time targets under normal load.

| Endpoint Type | p50 | p95 | p99 |
|---------------|-----|-----|-----|
| Health/Ready | < 10ms | < 50ms | < 100ms |
| Read (single) | < 50ms | < 150ms | < 300ms |
| Read (list) | < 100ms | < 300ms | < 500ms |
| Write (create/update) | < 100ms | < 300ms | < 500ms |
| Delete | < 50ms | < 150ms | < 300ms |
| Auth (login) | < 200ms | < 500ms | < 1s |
| Auth (OAuth callback) | < 500ms | < 1s | < 2s |

**Measurement:**
- Measured at the API gateway level
- Excludes network latency
- Monitored via OpenTelemetry + Prometheus

---

### NFR1-02: Throughput

| Field | Value |
|-------|-------|
| **ID** | NFR1-02 |
| **Category** | Performance |
| **Priority** | P1 |

**Requirement:**
The system shall support the following request throughput per instance.

| Metric | Target |
|--------|--------|
| Requests/second (sustained) | 1,000 RPS |
| Requests/second (peak) | 2,500 RPS |
| Concurrent connections | 10,000 |

**Measurement:**
- Load tested with k6 or Gatling
- Measured on standard instance (4 vCPU, 8GB RAM)

---

### NFR1-03: Database Performance

| Field | Value |
|-------|-------|
| **ID** | NFR1-03 |
| **Category** | Performance |
| **Priority** | P1 |

**Requirement:**
Database queries shall meet the following performance targets.

| Query Type | Target |
|------------|--------|
| Simple SELECT (by ID) | < 5ms |
| SELECT with RLS | < 10ms |
| INSERT single row | < 10ms |
| UPDATE single row | < 10ms |
| SELECT with pagination | < 50ms |
| Complex JOIN | < 100ms |

**Measurement:**
- Query execution time (excluding network)
- Monitored via PostgreSQL `pg_stat_statements`

---

### NFR1-04: Cache Performance

| Field | Value |
|-------|-------|
| **ID** | NFR1-04 |
| **Category** | Performance |
| **Priority** | P2 |

**Requirement:**
Redis cache operations shall complete within target times.

| Operation | Target |
|-----------|--------|
| GET (single key) | < 1ms |
| SET (single key) | < 1ms |
| MGET (10 keys) | < 5ms |
| Cache hit rate | > 90% |

---

## NFR2: Scalability

### NFR2-01: Horizontal Scaling

| Field | Value |
|-------|-------|
| **ID** | NFR2-01 |
| **Category** | Scalability |
| **Priority** | P1 |

**Requirement:**
The system shall scale horizontally without code changes.

**Targets:**
- API layer: Stateless, scales to N instances behind load balancer
- Database: Read replicas for read scaling
- Cache: Redis cluster for distributed caching

**Constraints:**
- No sticky sessions required
- No local file storage for user data
- All state stored in PostgreSQL or Redis

---

### NFR2-02: Tenant Scaling

| Field | Value |
|-------|-------|
| **ID** | NFR2-02 |
| **Category** | Scalability |
| **Priority** | P1 |

**Requirement:**
The system shall support the following tenant scale.

| Metric | Target (Year 1) | Target (Year 3) |
|--------|-----------------|-----------------|
| Total tenants | 1,000 | 10,000 |
| Users per tenant (avg) | 20 | 50 |
| Users per tenant (max) | 500 | 5,000 |
| Resources per tenant (avg) | 1,000 | 10,000 |
| Total resources | 1M | 100M |

---

### NFR2-03: Database Scaling

| Field | Value |
|-------|-------|
| **ID** | NFR2-03 |
| **Category** | Scalability |
| **Priority** | P2 |

**Requirement:**
Database shall scale to support growth targets.

**Strategy:**
1. **Year 1:** Single primary + 2 read replicas
2. **Year 2:** Add connection pooling (PgBouncer)
3. **Year 3:** Consider partitioning by tenant_id if needed

**Limits:**
- Connection pool: 100 connections per instance
- Max connections to DB: 500

---

## NFR3: Security

### NFR3-01: Data Isolation

| Field | Value |
|-------|-------|
| **ID** | NFR3-01 |
| **Category** | Security |
| **Priority** | P0 |

**Requirement:**
Complete data isolation between tenants shall be enforced at the database level.

**Implementation:**
- PostgreSQL Row-Level Security (RLS) on all tenant-scoped tables
- RLS policies evaluate `app.current_tenant_id` session variable
- No application-level filtering (defense in depth)

**Verification:**
- Integration tests verify cross-tenant access is blocked
- Security audit of RLS policies
- Penetration testing includes tenant isolation tests

---

### NFR3-02: Authentication Security

| Field | Value |
|-------|-------|
| **ID** | NFR3-02 |
| **Category** | Security |
| **Priority** | P0 |

**Requirement:**
Authentication shall follow security best practices.

| Control | Requirement |
|---------|-------------|
| Password hashing | bcrypt, cost factor 12 |
| JWT signing | RS256 (asymmetric) or HS256 (symmetric with rotation) |
| Token expiry | Access: 15 min, Refresh: 7 days |
| Failed login attempts | Rate limited: 5/15min per email |
| OAuth2 | PKCE required for authorization code flow |

---

### NFR3-03: API Security

| Field | Value |
|-------|-------|
| **ID** | NFR3-03 |
| **Category** | Security |
| **Priority** | P0 |

**Requirement:**
API shall implement security controls.

| Control | Requirement |
|---------|-------------|
| HTTPS | Required for all endpoints |
| CORS | Configured per environment |
| Rate limiting | Per-tenant, per-plan limits |
| Input validation | All inputs validated and sanitized |
| SQL injection | Prevented via parameterized queries (Quill) |
| XSS | JSON responses, proper content-type |

**Security Headers:**
```
Strict-Transport-Security: max-age=31536000; includeSubDomains
X-Content-Type-Options: nosniff
X-Frame-Options: DENY
X-XSS-Protection: 1; mode=block
Content-Security-Policy: default-src 'self'
```

---

### NFR3-04: Secrets Management

| Field | Value |
|-------|-------|
| **ID** | NFR3-04 |
| **Category** | Security |
| **Priority** | P0 |

**Requirement:**
Secrets shall be managed securely.

| Requirement | Implementation |
|-------------|----------------|
| No secrets in code | Environment variables or secret manager |
| Database credentials | Rotated quarterly |
| JWT signing keys | Rotated annually |
| API keys | Never logged, masked in UI |
| Stripe webhooks | Signature verification |

---

### NFR3-05: Audit Logging

| Field | Value |
|-------|-------|
| **ID** | NFR3-05 |
| **Category** | Security |
| **Priority** | P1 |

**Requirement:**
Security-relevant events shall be logged for audit purposes.

**Logged Events:**
- Authentication (success/failure)
- Authorization failures
- User/role changes
- Tenant setting changes
- Billing events
- Resource access patterns

**Log Retention:**
- 90 days hot storage
- 1 year archive

---

### NFR3-06: Rate Limiting

| Field | Value |
|-------|-------|
| **ID** | NFR3-06 |
| **Category** | Security |
| **Priority** | P0 |

**Requirement:**
API endpoints shall be rate limited per tenant.

| Plan | Requests/Minute | Requests/Hour | Requests/Day |
|------|-----------------|---------------|--------------|
| Free | 60 | 1,000 | 10,000 |
| Starter | 300 | 10,000 | 100,000 |
| Professional | 1,000 | 50,000 | 500,000 |
| Enterprise | Custom | Custom | Custom |

**Implementation:**
- Token bucket algorithm
- Stored in Redis
- Returns `429 Too Many Requests` with `Retry-After` header

---

## NFR4: Reliability

### NFR4-01: Availability

| Field | Value |
|-------|-------|
| **ID** | NFR4-01 |
| **Category** | Reliability |
| **Priority** | P0 |

**Requirement:**
The system shall achieve the following availability targets.

| Tier | Availability | Downtime/Month |
|------|--------------|----------------|
| Free | 99.0% | 7.3 hours |
| Starter | 99.5% | 3.65 hours |
| Professional | 99.9% | 43.8 minutes |
| Enterprise | 99.95% | 21.9 minutes |

**Excludes:**
- Scheduled maintenance (with 48h notice)
- Force majeure events

---

### NFR4-02: Fault Tolerance

| Field | Value |
|-------|-------|
| **ID** | NFR4-02 |
| **Category** | Reliability |
| **Priority** | P1 |

**Requirement:**
The system shall gracefully handle component failures.

| Component | Failure Behavior |
|-----------|------------------|
| API instance | Load balancer routes to healthy instances |
| PostgreSQL primary | Automatic failover to replica (< 30s) |
| Redis primary | Automatic failover to replica |
| Stripe API | Retry with exponential backoff, queue webhooks |
| Google OAuth | Graceful degradation (password login available) |

---

### NFR4-03: Data Durability

| Field | Value |
|-------|-------|
| **ID** | NFR4-03 |
| **Category** | Reliability |
| **Priority** | P0 |

**Requirement:**
Data shall be durable and recoverable.

| Requirement | Target |
|-------------|--------|
| RPO (Recovery Point Objective) | 1 hour |
| RTO (Recovery Time Objective) | 4 hours |
| Backup frequency | Hourly incremental, daily full |
| Backup retention | 30 days |
| Cross-region backup | Yes (for Enterprise) |

---

### NFR4-04: Graceful Degradation

| Field | Value |
|-------|-------|
| **ID** | NFR4-04 |
| **Category** | Reliability |
| **Priority** | P2 |

**Requirement:**
The system shall degrade gracefully when dependencies fail.

| Dependency | Degradation |
|------------|-------------|
| Redis unavailable | Fall back to database for sessions, disable rate limiting |
| Feature flags unavailable | Use default feature states |
| Stripe unavailable | Queue billing events, display cached subscription status |
| Metrics unavailable | Continue operation, buffer metrics |

---

## NFR5: Observability

### NFR5-01: Distributed Tracing

| Field | Value |
|-------|-------|
| **ID** | NFR5-01 |
| **Category** | Observability |
| **Priority** | P1 |

**Requirement:**
All requests shall be traced end-to-end.

**Implementation:**
- OpenTelemetry for instrumentation
- Jaeger for trace storage and visualization
- W3C Trace Context propagation

**Trace Coverage:**
- HTTP requests
- Database queries
- Redis operations
- External API calls (Stripe, OAuth)

---

### NFR5-02: Metrics

| Field | Value |
|-------|-------|
| **ID** | NFR5-02 |
| **Category** | Observability |
| **Priority** | P1 |

**Requirement:**
System metrics shall be collected and exposed.

**Key Metrics:**

| Metric | Type | Labels |
|--------|------|--------|
| `http_requests_total` | Counter | method, path, status, tenant |
| `http_request_duration_seconds` | Histogram | method, path, tenant |
| `db_query_duration_seconds` | Histogram | query_type |
| `db_connection_pool_size` | Gauge | - |
| `db_connection_pool_used` | Gauge | - |
| `redis_operations_total` | Counter | operation |
| `active_users` | Gauge | tenant, plan |
| `feature_flag_evaluations` | Counter | feature, result |

**Alerting Thresholds:**

| Metric | Warning | Critical |
|--------|---------|----------|
| p99 latency | > 500ms | > 1s |
| Error rate | > 1% | > 5% |
| DB connections | > 70% | > 90% |
| Memory usage | > 70% | > 90% |

---

### NFR5-03: Logging

| Field | Value |
|-------|-------|
| **ID** | NFR5-03 |
| **Category** | Observability |
| **Priority** | P0 |

**Requirement:**
Structured logging shall be implemented throughout.

**Log Format:**
```json
{
  "timestamp": "2024-01-15T10:30:00.000Z",
  "level": "INFO",
  "message": "Request completed",
  "service": "api",
  "trace_id": "abc123",
  "span_id": "def456",
  "tenant_id": "tenant-123",
  "user_id": "user-456",
  "method": "GET",
  "path": "/api/v1/resources",
  "status": 200,
  "duration_ms": 45
}
```

**Log Levels:**
- ERROR: System errors, exceptions
- WARN: Recoverable issues, rate limits
- INFO: Request lifecycle, business events
- DEBUG: Detailed debugging (disabled in production)

---

### NFR5-04: Health Checks

| Field | Value |
|-------|-------|
| **ID** | NFR5-04 |
| **Category** | Observability |
| **Priority** | P0 |

**Requirement:**
Health endpoints shall report component status.

**Endpoints:**

| Endpoint | Purpose | Checks |
|----------|---------|--------|
| `GET /health` | Liveness | Process is running |
| `GET /ready` | Readiness | DB connected, Redis connected |

**Response Format:**
```json
{
  "status": "healthy",
  "checks": {
    "database": { "status": "up", "latency_ms": 2 },
    "redis": { "status": "up", "latency_ms": 1 },
    "stripe": { "status": "up" }
  },
  "version": "1.2.3",
  "uptime_seconds": 3600
}
```

---

## NFR6: Maintainability

### NFR6-01: Code Quality

| Field | Value |
|-------|-------|
| **ID** | NFR6-01 |
| **Category** | Maintainability |
| **Priority** | P1 |

**Requirement:**
Code shall meet quality standards.

| Metric | Target |
|--------|--------|
| Test coverage | > 80% |
| Cyclomatic complexity | < 10 per function |
| Code duplication | < 3% |
| Scalafmt compliance | 100% |
| Scalafix warnings | 0 |

---

### NFR6-02: Documentation

| Field | Value |
|-------|-------|
| **ID** | NFR6-02 |
| **Category** | Maintainability |
| **Priority** | P2 |

**Requirement:**
System shall be well-documented.

| Document | Requirement |
|----------|-------------|
| README | Setup and running instructions |
| API docs | OpenAPI 3.0 specification |
| Architecture | C4 diagrams, decision records |
| Runbook | Operational procedures |
| Code | Scaladoc for public APIs |

---

### NFR6-03: Modularity

| Field | Value |
|-------|-------|
| **ID** | NFR6-03 |
| **Category** | Maintainability |
| **Priority** | P1 |

**Requirement:**
System shall follow modular architecture.

**Modules:**
```
modules/
├── core/          # Domain models, errors, interfaces
├── config/        # Configuration loading
├── database/      # Repository implementations
├── api/           # HTTP routes and middleware
├── auth/          # Authentication and authorization
├── billing/       # Stripe integration
├── features/      # Feature flag service
└── observability/ # Tracing, metrics, logging
```

**Module Rules:**
- Clear dependencies (no cycles)
- Interfaces in core, implementations in modules
- Each module independently testable

---

## NFR7: Usability

### NFR7-01: API Usability

| Field | Value |
|-------|-------|
| **ID** | NFR7-01 |
| **Category** | Usability |
| **Priority** | P2 |

**Requirement:**
API shall follow REST best practices.

| Principle | Implementation |
|-----------|----------------|
| Resource naming | Plural nouns (`/resources`, not `/resource`) |
| HTTP methods | GET=read, POST=create, PATCH=update, DELETE=delete |
| Status codes | Correct codes (201 Created, 204 No Content, etc.) |
| Pagination | Limit/offset with total count |
| Error messages | Actionable, include error codes |
| Versioning | URL path versioning (`/api/v1/`) |

---

### NFR7-02: Developer Experience

| Field | Value |
|-------|-------|
| **ID** | NFR7-02 |
| **Category** | Usability |
| **Priority** | P2 |

**Requirement:**
Developer experience shall be prioritized.

| Requirement | Implementation |
|-------------|----------------|
| Quick start | Working setup in < 5 minutes |
| Local development | Docker Compose for all dependencies |
| Hot reload | sbt ~reStart for development |
| IDE support | IntelliJ/VS Code configurations |
| Type safety | Compile-time errors over runtime |

---

## NFR8: Compliance

### NFR8-01: Data Privacy

| Field | Value |
|-------|-------|
| **ID** | NFR8-01 |
| **Category** | Compliance |
| **Priority** | P1 |

**Requirement:**
System shall support data privacy requirements.

| Requirement | Implementation |
|-------------|----------------|
| Data export | API for tenant data export |
| Data deletion | Tenant deletion removes all data |
| Consent tracking | Audit log of consent events |
| Data minimization | Only collect necessary data |
| Encryption at rest | PostgreSQL TDE or disk encryption |
| Encryption in transit | TLS 1.2+ for all connections |

---

### NFR8-02: GDPR Readiness

| Field | Value |
|-------|-------|
| **ID** | NFR8-02 |
| **Category** | Compliance |
| **Priority** | P2 |

**Requirement:**
System shall support GDPR compliance (for EU expansion).

| Right | Implementation |
|-------|----------------|
| Right to access | Data export endpoint |
| Right to erasure | Tenant/user deletion |
| Right to portability | Standard export format (JSON) |
| Data processing records | Audit logs |

---

## NFR9: Operational

### NFR9-01: Deployment

| Field | Value |
|-------|-------|
| **ID** | NFR9-01 |
| **Category** | Operational |
| **Priority** | P1 |

**Requirement:**
Deployments shall be automated and safe.

| Requirement | Implementation |
|-------------|----------------|
| CI/CD | GitHub Actions |
| Container | Docker images |
| Zero-downtime | Rolling deployments |
| Rollback | Immediate rollback capability |
| Database migrations | Flyway, backward compatible |
| Feature flags | Gradual rollout for risky changes |

---

### NFR9-02: Configuration

| Field | Value |
|-------|-------|
| **ID** | NFR9-02 |
| **Category** | Operational |
| **Priority** | P1 |

**Requirement:**
Configuration shall be environment-based.

| Environment | Configuration |
|-------------|---------------|
| Development | Local defaults, Docker |
| Staging | Staging secrets, test Stripe |
| Production | Production secrets, live Stripe |

**Configuration Sources:**
1. Environment variables (highest priority)
2. Config files (per environment)
3. Default values (lowest priority)

---

## Constraints

### Technical Constraints

| Constraint | Description |
|------------|-------------|
| Language | Scala 3.3.x |
| Runtime | JVM 17+ |
| Effect System | ZIO 2.x |
| Database | PostgreSQL 15+ |
| Cache | Redis 7+ |
| Container | Docker |

### Business Constraints

| Constraint | Description |
|------------|-------------|
| Multi-tenancy | Shared database with RLS |
| Billing | Stripe only |
| Authentication | JWT + Google OAuth initially |
| Hosting | Cloud-native (AWS/GCP ready) |

### Regulatory Constraints

| Constraint | Description |
|------------|-------------|
| Data residency | US initially, EU later |
| PCI compliance | Stripe handles payment data |
| SOC 2 | Target for Year 2 |

---

## Quality Metrics Dashboard

### Key Performance Indicators (KPIs)

| Category | Metric | Target | Current |
|----------|--------|--------|---------|
| Performance | p99 latency | < 500ms | TBD |
| Performance | Throughput | 1000 RPS | TBD |
| Reliability | Availability | 99.9% | TBD |
| Reliability | Error rate | < 0.1% | TBD |
| Security | RLS compliance | 100% | TBD |
| Security | Auth failures | < 1% | TBD |
| Quality | Test coverage | > 80% | TBD |
| Quality | Code duplication | < 3% | TBD |

### Monitoring Dashboards

1. **System Health**
   - Request rate, error rate, latency
   - Database connections, query performance
   - Redis operations, memory usage

2. **Business Metrics**
   - Active tenants, active users
   - Resource counts by tenant
   - Feature flag adoption

3. **Security**
   - Authentication attempts (success/failure)
   - Rate limit violations
   - Suspicious activity patterns

---

## Revision History

| Version | Date | Author | Changes |
|---------|------|--------|---------|
| 1.0 | 2024-01-15 | - | Initial version |
