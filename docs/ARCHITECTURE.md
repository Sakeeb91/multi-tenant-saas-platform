# Architecture Documentation

> UML diagrams and architectural views for the Multi-Tenant SaaS Platform

## Table of Contents

1. [System Context](#system-context)
2. [Container Diagram](#container-diagram)
3. [Component Diagram](#component-diagram)
4. [Domain Model](#domain-model)
5. [Database Schema](#database-schema)
6. [Sequence Diagrams](#sequence-diagrams)
7. [Deployment Architecture](#deployment-architecture)

---

## System Context

High-level view showing the system and its external dependencies.

```mermaid
C4Context
    title System Context Diagram - Multi-Tenant SaaS Platform

    Person(user, "Tenant User", "End user of a tenant organization")
    Person(admin, "Tenant Admin", "Administrator of a tenant organization")

    System(saas, "Multi-Tenant SaaS Platform", "Provides multi-tenant API services with billing and feature management")

    System_Ext(stripe, "Stripe", "Payment processing and subscription management")
    System_Ext(google, "Google OAuth", "Social authentication provider")
    System_Ext(email, "Email Service", "Transactional email delivery")

    Rel(user, saas, "Uses API", "HTTPS/JSON")
    Rel(admin, saas, "Manages tenant", "HTTPS/JSON")
    Rel(saas, stripe, "Processes payments", "HTTPS")
    Rel(saas, google, "Authenticates users", "OAuth2")
    Rel(saas, email, "Sends notifications", "SMTP/API")
```

### Simplified System Context

```mermaid
flowchart TB
    subgraph External["External Systems"]
        Stripe["Stripe<br/>Billing & Payments"]
        Google["Google OAuth<br/>Authentication"]
    end

    subgraph Users["Users"]
        TenantUser["Tenant User"]
        TenantAdmin["Tenant Admin"]
    end

    subgraph Platform["Multi-Tenant SaaS Platform"]
        API["API Gateway"]
        Core["Core Services"]
    end

    TenantUser -->|HTTPS| API
    TenantAdmin -->|HTTPS| API
    API --> Core
    Core -->|Payments| Stripe
    Core -->|OAuth2| Google
```

---

## Container Diagram

Shows the high-level technology choices and how containers communicate.

```mermaid
flowchart TB
    subgraph Users["Users"]
        Client["API Client<br/><i>HTTP/JSON</i>"]
    end

    subgraph Platform["Multi-Tenant SaaS Platform"]
        subgraph API["API Layer"]
            Gateway["API Gateway<br/><i>zio-http</i>"]
        end

        subgraph Services["Service Layer"]
            AuthSvc["Auth Service<br/><i>JWT + OAuth2</i>"]
            TenantSvc["Tenant Service<br/><i>ZIO</i>"]
            ResourceSvc["Resource Service<br/><i>ZIO</i>"]
            BillingSvc["Billing Service<br/><i>Stripe SDK</i>"]
            FeatureSvc["Feature Flag Service<br/><i>ZIO</i>"]
        end

        subgraph Data["Data Layer"]
            Postgres[("PostgreSQL 15+<br/><i>Row-Level Security</i>")]
            Redis[("Redis 7+<br/><i>Sessions & Cache</i>")]
        end

        subgraph Observability["Observability"]
            Tracing["OpenTelemetry<br/><i>Jaeger</i>"]
            Metrics["Prometheus<br/><i>Metrics</i>"]
        end
    end

    subgraph External["External Services"]
        Stripe["Stripe API"]
        GoogleOAuth["Google OAuth"]
    end

    Client --> Gateway
    Gateway --> AuthSvc
    Gateway --> TenantSvc
    Gateway --> ResourceSvc
    Gateway --> BillingSvc
    Gateway --> FeatureSvc

    AuthSvc --> Postgres
    AuthSvc --> Redis
    AuthSvc --> GoogleOAuth
    TenantSvc --> Postgres
    ResourceSvc --> Postgres
    BillingSvc --> Stripe
    BillingSvc --> Postgres
    FeatureSvc --> Redis

    Gateway --> Tracing
    Gateway --> Metrics
```

---

## Component Diagram

Detailed view of the API layer components.

```mermaid
flowchart TB
    subgraph API["API Layer (zio-http)"]
        subgraph Middleware["Middleware Pipeline"]
            AuthFilter["Auth Filter<br/><i>JWT Validation</i>"]
            TenantRouter["Tenant Router<br/><i>Subdomain/Header</i>"]
            RateLimiter["Rate Limiter<br/><i>Per-Tenant</i>"]
            RequestLogger["Request Logger<br/><i>Correlation ID</i>"]
            SecurityHeaders["Security Headers<br/><i>CORS, CSP</i>"]
        end

        subgraph Routes["Route Handlers"]
            HealthRoutes["Health Routes<br/><i>/health, /ready</i>"]
            AuthRoutes["Auth Routes<br/><i>/auth/*</i>"]
            TenantRoutes["Tenant Routes<br/><i>/api/v1/tenants</i>"]
            ResourceRoutes["Resource Routes<br/><i>/api/v1/resources</i>"]
            BillingRoutes["Billing Routes<br/><i>/api/v1/billing</i>"]
            WebhookRoutes["Webhook Routes<br/><i>/webhooks/*</i>"]
        end
    end

    subgraph Services["Service Layer"]
        AuthService["AuthService"]
        TenantService["TenantService"]
        ResourceService["ResourceService"]
        StripeService["StripeService"]
        FeatureFlagService["FeatureFlagService"]
    end

    AuthFilter --> TenantRouter --> RateLimiter --> RequestLogger --> SecurityHeaders

    SecurityHeaders --> HealthRoutes
    SecurityHeaders --> AuthRoutes
    SecurityHeaders --> TenantRoutes
    SecurityHeaders --> ResourceRoutes
    SecurityHeaders --> BillingRoutes
    SecurityHeaders --> WebhookRoutes

    AuthRoutes --> AuthService
    TenantRoutes --> TenantService
    ResourceRoutes --> ResourceService
    BillingRoutes --> StripeService
    ResourceRoutes --> FeatureFlagService
```

---

## Domain Model

Class diagram showing the core domain entities.

```mermaid
classDiagram
    class TenantId {
        <<opaque type>>
        +UUID value
        +generate() TenantId
        +fromString(s: String) Either~String, TenantId~
    }

    class UserId {
        <<opaque type>>
        +UUID value
        +generate() UserId
        +fromString(s: String) Either~String, UserId~
    }

    class ResourceId {
        <<opaque type>>
        +UUID value
        +generate() ResourceId
        +fromString(s: String) Either~String, ResourceId~
    }

    class Plan {
        <<enumeration>>
        Free
        Starter
        Professional
        Enterprise
    }

    class Role {
        <<enumeration>>
        Owner
        Admin
        Member
        Viewer
    }

    class Tenant {
        +TenantId id
        +String name
        +String slug
        +Plan plan
        +Instant createdAt
        +Instant updatedAt
    }

    class User {
        +UserId id
        +TenantId tenantId
        +String email
        +String name
        +Role role
        +Instant createdAt
    }

    class Resource {
        +ResourceId id
        +TenantId tenantId
        +String name
        +Map~String,String~ data
        +UserId createdBy
        +Instant createdAt
        +Instant updatedAt
    }

    class Subscription {
        +String id
        +TenantId tenantId
        +Plan plan
        +SubscriptionStatus status
        +Instant currentPeriodStart
        +Instant currentPeriodEnd
        +Boolean cancelAtPeriodEnd
    }

    class SubscriptionStatus {
        <<enumeration>>
        Active
        PastDue
        Canceled
        Incomplete
        Trialing
    }

    class Feature {
        +String key
        +Boolean defaultEnabled
        +String description
        +Set~Plan~ enabledForPlans
        +Set~TenantId~ enabledForTenants
        +Option~Int~ percentageRollout
    }

    Tenant "1" --> "*" User : has
    Tenant "1" --> "*" Resource : owns
    Tenant "1" --> "0..1" Subscription : subscribes
    User "1" --> "*" Resource : creates
    Tenant --> Plan : has
    User --> Role : has
    Subscription --> SubscriptionStatus : has
    Subscription --> Plan : for
    Feature --> Plan : targets
```

---

## Database Schema

Entity-Relationship diagram for the PostgreSQL database.

```mermaid
erDiagram
    TENANTS {
        uuid id PK
        varchar name
        varchar slug UK
        varchar plan
        timestamp created_at
        timestamp updated_at
    }

    USERS {
        uuid id PK
        uuid tenant_id FK
        varchar email
        varchar name
        varchar role
        varchar password_hash
        timestamp created_at
    }

    RESOURCES {
        uuid id PK
        uuid tenant_id FK
        varchar name
        jsonb data
        uuid created_by FK
        timestamp created_at
        timestamp updated_at
    }

    STRIPE_CUSTOMERS {
        uuid tenant_id PK,FK
        varchar customer_id UK
        timestamp created_at
    }

    SUBSCRIPTIONS {
        varchar id PK
        uuid tenant_id FK
        varchar plan
        varchar status
        timestamp current_period_start
        timestamp current_period_end
        boolean cancel_at_period_end
        timestamp created_at
        timestamp updated_at
    }

    FEATURE_OVERRIDES {
        uuid id PK
        varchar feature_key
        uuid tenant_id FK
        uuid user_id FK
        boolean enabled
        timestamp created_at
    }

    SESSIONS {
        varchar id PK
        uuid user_id FK
        uuid tenant_id FK
        varchar refresh_token
        timestamp expires_at
        timestamp created_at
    }

    TENANTS ||--o{ USERS : "has"
    TENANTS ||--o{ RESOURCES : "owns"
    TENANTS ||--o| STRIPE_CUSTOMERS : "has"
    TENANTS ||--o| SUBSCRIPTIONS : "has"
    TENANTS ||--o{ FEATURE_OVERRIDES : "has"
    USERS ||--o{ RESOURCES : "creates"
    USERS ||--o{ SESSIONS : "has"
    USERS ||--o{ FEATURE_OVERRIDES : "has"
```

---

## Sequence Diagrams

### Authentication Flow (JWT + OAuth2)

```mermaid
sequenceDiagram
    autonumber
    participant Client
    participant API as API Gateway
    participant Auth as Auth Service
    participant Google as Google OAuth
    participant Redis
    participant DB as PostgreSQL

    Client->>API: GET /auth/google
    API->>Auth: Initiate OAuth flow
    Auth->>Client: Redirect to Google

    Client->>Google: Authenticate
    Google->>Client: Authorization code
    Client->>API: GET /auth/callback?code=xxx

    API->>Auth: Exchange code
    Auth->>Google: Token exchange
    Google->>Auth: Access token + ID token

    Auth->>DB: Find or create user
    DB->>Auth: User record

    Auth->>Auth: Generate JWT
    Auth->>Redis: Store session
    Auth->>Client: JWT + Refresh token

    Note over Client,Redis: Subsequent requests

    Client->>API: GET /api/v1/resources<br/>Authorization: Bearer <jwt>
    API->>Auth: Validate JWT
    Auth->>API: User context
    API->>DB: Query with RLS
    DB->>API: Results
    API->>Client: Response
```

### Multi-Tenant Request Flow

```mermaid
sequenceDiagram
    autonumber
    participant Client
    participant Gateway as API Gateway
    participant Middleware
    participant Service as Resource Service
    participant Repo as Repository
    participant DB as PostgreSQL

    Client->>Gateway: GET /api/v1/resources<br/>X-Tenant-ID: tenant-123<br/>Authorization: Bearer jwt

    Gateway->>Middleware: Process request

    Note over Middleware: Auth Filter
    Middleware->>Middleware: Validate JWT
    Middleware->>Middleware: Extract user claims

    Note over Middleware: Tenant Router
    Middleware->>Middleware: Extract tenant from header/subdomain
    Middleware->>Middleware: Validate tenant access

    Note over Middleware: Rate Limiter
    Middleware->>Middleware: Check rate limit (Redis)

    Middleware->>Service: list(tenantId, limit, offset)
    Service->>Repo: findAll(tenantId, limit, offset)

    Repo->>DB: SET app.current_tenant_id = 'tenant-123'
    Repo->>DB: SELECT * FROM resources<br/>(RLS applied automatically)
    DB->>Repo: Filtered results

    Repo->>Service: List[Resource]
    Service->>Gateway: Response
    Gateway->>Client: 200 OK + JSON
```

### Stripe Subscription Flow

```mermaid
sequenceDiagram
    autonumber
    participant Admin as Tenant Admin
    participant API
    participant Billing as Billing Service
    participant Stripe
    participant DB as PostgreSQL
    participant Webhook as Webhook Handler

    Admin->>API: POST /api/v1/billing/checkout<br/>{plan: "professional"}
    API->>Billing: Create checkout session
    Billing->>Stripe: Create Checkout Session
    Stripe->>Billing: Session URL
    Billing->>Admin: Redirect to Stripe Checkout

    Admin->>Stripe: Complete payment
    Stripe->>Admin: Success redirect

    Note over Stripe,Webhook: Async webhook

    Stripe->>API: POST /webhooks/stripe<br/>checkout.session.completed
    API->>Webhook: Handle event
    Webhook->>Webhook: Verify signature
    Webhook->>DB: Update tenant plan
    Webhook->>DB: Store subscription
    Webhook->>API: 200 OK

    Admin->>API: GET /api/v1/billing/subscription
    API->>Billing: Get subscription
    Billing->>DB: Query subscription
    DB->>Billing: Subscription record
    Billing->>Admin: Subscription details
```

### Feature Flag Evaluation

```mermaid
sequenceDiagram
    autonumber
    participant Service
    participant FeatureFlags as Feature Flag Service
    participant Redis
    participant Context as Tenant Context

    Service->>FeatureFlags: isEnabled("beta_dashboard")
    FeatureFlags->>Context: Get tenant & user
    Context->>FeatureFlags: TenantContext

    FeatureFlags->>Redis: GET feature:beta_dashboard
    Redis->>FeatureFlags: Feature config

    alt Feature has tenant override
        FeatureFlags->>FeatureFlags: Check tenant in enabledForTenants
        FeatureFlags->>Service: true/false
    else Feature has plan targeting
        FeatureFlags->>FeatureFlags: Check tenant.plan in enabledForPlans
        FeatureFlags->>Service: true/false
    else Feature has percentage rollout
        FeatureFlags->>FeatureFlags: Hash(tenantId) % 100 < percentage
        FeatureFlags->>Service: true/false
    else Default
        FeatureFlags->>Service: defaultEnabled
    end
```

---

## Deployment Architecture

```mermaid
flowchart TB
    subgraph Internet
        Users["Users"]
    end

    subgraph CloudProvider["Cloud Infrastructure"]
        subgraph LB["Load Balancer"]
            ALB["Application<br/>Load Balancer"]
        end

        subgraph Compute["Compute Layer"]
            API1["API Instance 1"]
            API2["API Instance 2"]
            API3["API Instance N"]
        end

        subgraph Data["Data Layer"]
            subgraph PG["PostgreSQL Cluster"]
                Primary["Primary"]
                Replica1["Read Replica 1"]
                Replica2["Read Replica 2"]
            end

            subgraph RedisCluster["Redis Cluster"]
                Redis1["Redis Primary"]
                Redis2["Redis Replica"]
            end
        end

        subgraph Observability["Observability"]
            Jaeger["Jaeger<br/>Tracing"]
            Prometheus["Prometheus<br/>Metrics"]
            Grafana["Grafana<br/>Dashboards"]
        end
    end

    subgraph External["External Services"]
        Stripe["Stripe"]
        Google["Google OAuth"]
    end

    Users --> ALB
    ALB --> API1
    ALB --> API2
    ALB --> API3

    API1 --> Primary
    API2 --> Primary
    API3 --> Primary
    API1 -.-> Replica1
    API2 -.-> Replica2

    Primary --> Replica1
    Primary --> Replica2

    API1 --> Redis1
    API2 --> Redis1
    API3 --> Redis1
    Redis1 --> Redis2

    API1 --> Jaeger
    API1 --> Prometheus
    Prometheus --> Grafana

    API1 --> Stripe
    API1 --> Google
```

---

## Data Flow: Row-Level Security

```mermaid
flowchart LR
    subgraph Request["Incoming Request"]
        JWT["JWT Token<br/>tenant_id: abc-123"]
    end

    subgraph Middleware["Middleware"]
        Extract["Extract<br/>Tenant ID"]
    end

    subgraph Connection["DB Connection"]
        SetVar["SET app.current_tenant_id<br/>= 'abc-123'"]
    end

    subgraph PostgreSQL["PostgreSQL"]
        RLS["RLS Policy Check<br/>tenant_id = current_tenant_id()"]
        Data["Filtered Data<br/>Only tenant abc-123"]
    end

    JWT --> Extract --> SetVar --> RLS --> Data
```

---

## Technology Stack Summary

| Layer | Technology | Purpose |
|-------|------------|---------|
| Language | Scala 3.3.x | Type-safe, functional |
| Effect System | ZIO 2.x | Async, resource management |
| HTTP Server | zio-http 3.x | Native ZIO integration |
| Database | PostgreSQL 15+ | RLS, JSONB |
| Cache | Redis 7+ | Sessions, rate limiting |
| Auth | JWT + OAuth2 | Stateless authentication |
| Billing | Stripe | Subscriptions, usage billing |
| Tracing | OpenTelemetry | Distributed tracing |
| Metrics | Prometheus | Monitoring |
| Containerization | Docker | Deployment |

---

## References

- [C4 Model](https://c4model.com/) - Software architecture diagrams
- [Mermaid](https://mermaid.js.org/) - Diagram syntax
- [PostgreSQL RLS](https://www.postgresql.org/docs/current/ddl-rowsecurity.html) - Row-Level Security
- [ZIO Documentation](https://zio.dev/) - Effect system
