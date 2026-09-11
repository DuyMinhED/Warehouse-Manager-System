# Database Design — Identity & Authorization

| | |
|---|---|
| **Document Version** | 1.1 |
| **Status** | Draft / Baseline |
| **Database** | PostgreSQL |
| **Scope** | Tenant, User, Warehouse, Role, Permission and Authorization |

---

## 1. Overview

This document describes the database design currently agreed for the WMS Identity & Authorization domain.

The design supports:

- Multi-Tenant SaaS
- Tenant-owned Warehouses
- Generic User Accounts
- Role-Based Access Control (RBAC)
- User-level Permission overrides
- Warehouse scope
- Audit information for assignments and permission changes
- Platform-level roles separated conceptually from Tenant business authorization

### 1.1 Core Model

```
Tenant
 ├── Users
 ├── Warehouses
 └── Roles

User
 ├── User Roles ──────────────> Role
 ├── User Permissions ────────> Permission
 └── User Warehouses ─────────> Warehouse

Role
 └── Role Permissions ────────> Permission
```

---

## 2. Design Principles

### 2.1 Multi-Tenancy

A **Tenant** is the primary business ownership boundary.

A Tenant may own:

- Multiple Users
- Multiple Warehouses
- Multiple Tenant-scoped Roles

Business data must not cross Tenant boundaries.

### 2.2 Generic User Account

Manager and Employee are **not** separate account entities. They are Users with corresponding Roles:

```
User + MANAGER Role  = Manager
User + EMPLOYEE Role = Employee
```

This keeps identity separate from business responsibility.

### 2.3 RBAC + User Override

The authorization model combines:

```
Role Permissions + User Permission Overrides = Effective Permissions
```

A User-level explicit permission overrides the permission inherited from a Role.

### 2.4 Permission vs Scope

| Dimension | Question |
|-----------|----------|
| **Permission** | What can the User do? |
| **Scope** | Where can the User do it? |

Example:

- Permission: `STOCK_COUNT`
- Scope: Warehouse A

Having `STOCK_COUNT` does not automatically authorize access to every Warehouse.

---

## 3. Tables

### 3.1 `tenants`

Stores organizations/business customers using the WMS platform.

| Column | Type | Null | Default | Constraints | Description |
|--------|------|------|---------|-------------|-------------|
| `id` | UUID | No | — | PK | Tenant identifier |
| `name` | VARCHAR(150) | No | — | CHECK | Tenant display name |
| `code` | VARCHAR(50) | No | — | UNIQUE, CHECK | Tenant short code |
| `status` | VARCHAR(20) | No | `ACTIVE` | CHECK | Tenant lifecycle status |
| `created_at` | TIMESTAMP | No | `CURRENT_TIMESTAMP` | — | Creation timestamp |
| `updated_at` | TIMESTAMP | No | `CURRENT_TIMESTAMP` | — | Last update timestamp |

**Constraints**

- `PRIMARY KEY (id)`
- `UNIQUE (code)`
- `CHECK (length(trim(name)) > 0)`
- `CHECK (code ~ '^[A-Z0-9_-]+$')`
- `CHECK (status IN ('ACTIVE', 'SUSPENDED', 'INACTIVE'))`

---

### 3.2 `users`

Stores generic user accounts. A User belongs to one Tenant in the current design.

`tenant_id = NULL` may be used for Platform Admin accounts if the platform authorization model adopts that approach.

| Column | Type | Null | Default | Constraints | Description |
|--------|------|------|---------|-------------|-------------|
| `id` | UUID | No | — | PK | User identifier |
| `tenant_id` | UUID | Yes | — | FK → `tenants.id` | Tenant ownership |
| `username` | VARCHAR(100) | No | — | UNIQUE | Login username |
| `email` | VARCHAR(255) | No | — | UNIQUE | User email |
| `password_hash` | VARCHAR(255) | No | — | — | Hashed password |
| `full_name` | VARCHAR(150) | No | — | CHECK | User full name |
| `phone` | VARCHAR(20) | Yes | — | — | Phone number |
| `status` | VARCHAR(20) | No | `ACTIVE` | CHECK | Account status |
| `last_login_at` | TIMESTAMP | Yes | — | — | Last successful login |
| `created_at` | TIMESTAMP | No | `CURRENT_TIMESTAMP` | — | Creation timestamp |
| `updated_at` | TIMESTAMP | No | `CURRENT_TIMESTAMP` | — | Last update timestamp |

**Constraints**

- `PRIMARY KEY (id)`
- `FOREIGN KEY (tenant_id) REFERENCES tenants(id)`
- `UNIQUE (username)`
- `UNIQUE (email)`
- `CHECK (length(trim(full_name)) > 0)`
- `CHECK (status IN ('ACTIVE', 'INACTIVE', 'LOCKED', 'PENDING'))`

**Design Note**

The initial model uses global uniqueness for username and email. For a SaaS model where the same email/username may be used independently by different Tenants, this can later be changed to Tenant-scoped uniqueness:

```sql
UNIQUE (tenant_id, username)
UNIQUE (tenant_id, email)
```

This is a product-level decision and should be finalized before production.

---

### 3.3 `warehouses`

Stores Warehouses owned by a Tenant. A Tenant can create multiple Warehouses.

| Column | Type | Null | Default | Constraints | Description |
|--------|------|------|---------|-------------|-------------|
| `id` | UUID | No | — | PK | Warehouse identifier |
| `tenant_id` | UUID | No | — | FK → `tenants.id` | Owner Tenant |
| `name` | VARCHAR(150) | No | — | CHECK | Warehouse name |
| `code` | VARCHAR(50) | No | — | UNIQUE per Tenant, CHECK | Warehouse code |
| `address` | VARCHAR(255) | Yes | — | — | Warehouse address |
| `status` | VARCHAR(20) | No | `ACTIVE` | CHECK | Warehouse status |
| `created_at` | TIMESTAMP | No | `CURRENT_TIMESTAMP` | — | Creation timestamp |
| `updated_at` | TIMESTAMP | No | `CURRENT_TIMESTAMP` | — | Last update timestamp |

**Constraints**

- `PRIMARY KEY (id)`
- `FOREIGN KEY (tenant_id) REFERENCES tenants(id)`
- `UNIQUE (tenant_id, code)`
- `CHECK (length(trim(name)) > 0)`
- `CHECK (length(trim(code)) > 0)`
- `CHECK (status IN ('ACTIVE', 'INACTIVE', 'ARCHIVED'))`

**Business Rules**

- A Warehouse belongs to exactly one Tenant
- A Tenant may own many Warehouses
- A Warehouse has at most one active Manager
- A Warehouse may temporarily have no Manager
- Tenant may directly manage a Managerless Warehouse

---

### 3.4 `roles`

Stores Roles used by the authorization system. Roles may be System-scoped or Tenant-scoped.

| Column | Type | Null | Default | Constraints | Description |
|--------|------|------|---------|-------------|-------------|
| `id` | UUID | No | — | PK | Role identifier |
| `tenant_id` | UUID | Yes | — | FK → `tenants.id` | Role owner; NULL for System Role |
| `code` | VARCHAR(50) | No | — | UNIQUE by scope | Backend role code |
| `name` | VARCHAR(100) | No | — | — | Display name |
| `description` | TEXT | Yes | — | — | Role description |
| `created_at` | TIMESTAMP | No | `CURRENT_TIMESTAMP` | — | Creation timestamp |
| `updated_at` | TIMESTAMP | No | `CURRENT_TIMESTAMP` | — | Last update timestamp |

**Constraints**

- `PRIMARY KEY (id)`
- `FOREIGN KEY (tenant_id) REFERENCES tenants(id)`

**Role Scope**

```
tenant_id IS NULL     → System Role
tenant_id IS NOT NULL → Tenant Role
```

**Recommended PostgreSQL Unique Indexes**

```sql
CREATE UNIQUE INDEX uq_system_role_code
ON roles(code)
WHERE tenant_id IS NULL;

CREATE UNIQUE INDEX uq_tenant_role_code
ON roles(tenant_id, code)
WHERE tenant_id IS NOT NULL;
```

This allows the same Role code to exist in different Tenant scopes while keeping System Role codes unique.

---

### 3.5 `permissions`

Stores atomic permissions available to the WMS platform.

**Examples:** `VIEW_INVENTORY`, `RECEIVE_STOCK`, `PICK_ORDER`, `STOCK_COUNT`, `CREATE_WAREHOUSE`, `MANAGE_USERS`, `MANAGE_ROLES`, `MANAGE_PERMISSIONS`

| Column | Type | Null | Default | Constraints | Description |
|--------|------|------|---------|-------------|-------------|
| `id` | UUID | No | — | PK | Permission identifier |
| `code` | VARCHAR(100) | No | — | UNIQUE | Atomic permission code |
| `name` | VARCHAR(150) | No | — | — | Display name |
| `module` | VARCHAR(50) | No | — | — | Functional module |
| `description` | TEXT | Yes | — | — | Permission description |
| `created_at` | TIMESTAMP | No | `CURRENT_TIMESTAMP` | — | Creation timestamp |

**Constraints**

- `PRIMARY KEY (id)`
- `UNIQUE (code)`
- `CHECK (length(trim(code)) > 0)`
- `CHECK (length(trim(name)) > 0)`
- `CHECK (length(trim(module)) > 0)`

Permission codes are shared platform vocabulary and should not be duplicated per Tenant.

---

### 3.6 `user_roles`

Associates Users with Roles. Relationship: **User N : M Role**

| Column | Type | Null | Default | Constraints | Description |
|--------|------|------|---------|-------------|-------------|
| `user_id` | UUID | No | — | FK → `users.id` | User |
| `role_id` | UUID | No | — | FK → `roles.id` | Role |
| `created_at` | TIMESTAMP | No | `CURRENT_TIMESTAMP` | — | Assignment timestamp |

**Constraints**

- `PRIMARY KEY (user_id, role_id)`
- `FOREIGN KEY (user_id) REFERENCES users(id)`
- `FOREIGN KEY (role_id) REFERENCES roles(id)`

**Business Constraint**

A User must not be assigned a Tenant Role belonging to another Tenant.

Conceptually:

```
users.tenant_id = roles.tenant_id
```

or the Role is a System Role where:

```
roles.tenant_id IS NULL
```

This cross-tenant rule should be enforced through database design where practical and additionally validated in the application/domain layer.

---

### 3.7 `role_permissions`

Associates Roles with their default Permissions. Relationship: **Role N : M Permission**

| Column | Type | Null | Default | Constraints | Description |
|--------|------|------|---------|-------------|-------------|
| `role_id` | UUID | No | — | FK → `roles.id` | Role |
| `permission_id` | UUID | No | — | FK → `permissions.id` | Permission |
| `created_at` | TIMESTAMP | No | `CURRENT_TIMESTAMP` | — | Assignment timestamp |

**Constraints**

- `PRIMARY KEY (role_id, permission_id)`
- `FOREIGN KEY (role_id) REFERENCES roles(id)`
- `FOREIGN KEY (permission_id) REFERENCES permissions(id)`

Duplicate Role-Permission assignments are not allowed.

---

### 3.8 `user_permissions`

Stores explicit User-level Permission overrides. This table stores overrides, not a copy of every inherited Role Permission.

| Column | Type | Null | Default | Constraints | Description |
|--------|------|------|---------|-------------|-------------|
| `user_id` | UUID | No | — | FK → `users.id` | User |
| `permission_id` | UUID | No | — | FK → `permissions.id` | Permission |
| `effect` | VARCHAR(10) | No | — | CHECK | `ALLOW` or `DENY` |
| `created_by` | UUID | Yes | — | FK → `users.id` | User who created override |
| `created_at` | TIMESTAMP | No | `CURRENT_TIMESTAMP` | — | Creation timestamp |

**Constraints**

- `PRIMARY KEY (user_id, permission_id)`
- `FOREIGN KEY (user_id) REFERENCES users(id)`
- `FOREIGN KEY (permission_id) REFERENCES permissions(id)`
- `FOREIGN KEY (created_by) REFERENCES users(id)`
- `CHECK (effect IN ('ALLOW', 'DENY'))`

**Authorization Semantics**

Explicit User Permission has higher priority than Role Permission.

Example:

```
Role EMPLOYEE       → STOCK_COUNT = ALLOW
User Override       → STOCK_COUNT = DENY
Effective Permission → STOCK_COUNT = DENY
```

Only one current override is stored for a User/Permission pair. Historical changes should be stored through Audit Log or a dedicated permission history table.

---

### 3.9 `user_warehouses`

Associates Users with Warehouses and stores assignment metadata.

| Column | Type | Null | Default | Constraints | Description |
|--------|------|------|---------|-------------|-------------|
| `user_id` | UUID | No | — | FK → `users.id` | Assigned User |
| `warehouse_id` | UUID | No | — | FK → `warehouses.id` | Assigned Warehouse |
| `assigned_by` | UUID | Yes | — | FK → `users.id` | User performing assignment |
| `assigned_at` | TIMESTAMP | No | `CURRENT_TIMESTAMP` | — | Assignment timestamp |
| `status` | VARCHAR(20) | No | `ACTIVE` | CHECK | Assignment status |

**Constraints**

- `PRIMARY KEY (user_id, warehouse_id)`
- `FOREIGN KEY (user_id) REFERENCES users(id)`
- `FOREIGN KEY (warehouse_id) REFERENCES warehouses(id)`
- `FOREIGN KEY (assigned_by) REFERENCES users(id)`
- `CHECK (status IN ('ACTIVE', 'INACTIVE'))`

**Important Business Rules**

- **Manager:** one active Warehouse at a time
- **Employee:** one active Warehouse at a time
- A Manager or Employee must not have multiple active Warehouse assignments

**Recommended PostgreSQL constraint**

```sql
CREATE UNIQUE INDEX uq_user_active_warehouse
ON user_warehouses(user_id)
WHERE status = 'ACTIVE';
```

This enforces one active Warehouse per User.

**Tenant Consistency**

The User and Warehouse must belong to the same Tenant:

```
users.tenant_id = warehouses.tenant_id
```

A User from Tenant A must never be assigned to a Warehouse belonging to Tenant B. This should be enforced by database-level design where practical and validated again by the application/domain layer.

---

## 4. Relationship Summary

| Parent | Child | Relationship | Rule |
|--------|-------|--------------|------|
| Tenant | Users | 1 : N | User belongs to one Tenant |
| Tenant | Warehouses | 1 : N | Tenant owns many Warehouses |
| Tenant | Roles | 1 : N | Tenant may define Roles |
| User | Role | N : M | Through `user_roles` |
| Role | Permission | N : M | Through `role_permissions` |
| User | Permission | N : M | Through `user_permissions` |
| User | Warehouse | N : M | Through `user_warehouses`, constrained to one active assignment |
| User | User Permission | 1 : N | Explicit overrides |
| User | User Role | 1 : N | Role assignments |
| User | Audit Actor | 1 : N | Via `created_by` / assignment actor |

---

## 5. Authorization Model

### 5.1 Effective Permission

The effective permission of a User is determined from:

```
                User
                 |
          +------+------+
          |             |
        Roles       Direct Overrides
          |             |
          v             v
   Role Permissions   ALLOW/DENY
          |             |
          +------+------+
                 |
                 v
       Effective Permission
```

**Recommended precedence:**

1. Explicit User DENY
2. Explicit User ALLOW
3. Role Permission
4. No Permission

### 5.2 Authorization Scope

A request must satisfy all relevant conditions:

```
Authenticated
    AND Tenant Access Valid
    AND Permission Valid
    AND Warehouse Scope Valid
```

Possessing a Permission alone does not grant access to every Warehouse.

---

## 6. Core Business Constraints

| ID | Constraint |
|----|------------|
| BC-001 | **Tenant Ownership** — Every business entity must belong to exactly one Tenant directly or indirectly |
| BC-002 | **Warehouse Ownership** — Every Warehouse belongs to exactly one Tenant |
| BC-003 | **Warehouse Manager Limit** — A Warehouse can have at most one active Manager |
| BC-004 | **Manager Warehouse Limit** — A Manager can manage only one active Warehouse at a time |
| BC-005 | **Employee Warehouse Limit** — An Employee can work in only one active Warehouse at a time |
| BC-006 | **Employee Manager Requirement** — An Employee must belong to a Warehouse that has an active Manager |
| BC-007 | **Managerless Warehouse** — A Warehouse may exist without a Manager; the Tenant can directly manage that Warehouse |
| BC-008 | **Tenant Isolation** — Users cannot access business data belonging to another Tenant |
| BC-009 | **Role Scope** — A Tenant User cannot be assigned a Role belonging to another Tenant; System Roles may be shared where explicitly supported |
| BC-010 | **Permission Scope** — Permission and Warehouse scope are separate authorization dimensions |
| BC-011 | **Permission Override** — Explicit User Permission overrides inherited Role Permission |
| BC-012 | **Employee Transfer** — When an Employee changes Warehouse: old active assignment is ended, new assignment is created, new Warehouse must satisfy the Manager requirement, Employee is managed by the Manager of the new Warehouse, permissions must be reviewed/reprocessed, and the operation must be auditable |

---

## 7. Indexing Strategy

Indexes should support the most common Tenant and authorization queries.

```sql
-- Users
CREATE INDEX idx_users_tenant ON users(tenant_id);
CREATE INDEX idx_users_tenant_status ON users(tenant_id, status);

-- Warehouses
CREATE INDEX idx_warehouses_tenant ON warehouses(tenant_id);
CREATE INDEX idx_warehouses_tenant_status ON warehouses(tenant_id, status);

-- Roles
CREATE INDEX idx_roles_tenant ON roles(tenant_id);

-- Permissions
CREATE INDEX idx_permissions_module ON permissions(module);

-- User Roles
CREATE INDEX idx_user_roles_role ON user_roles(role_id);

-- Role Permissions
CREATE INDEX idx_role_permissions_permission ON role_permissions(permission_id);

-- User Permissions
CREATE INDEX idx_user_permissions_permission ON user_permissions(permission_id);
CREATE INDEX idx_user_permissions_created_by ON user_permissions(created_by);

-- User Warehouses
CREATE INDEX idx_user_warehouses_warehouse ON user_warehouses(warehouse_id);
CREATE INDEX idx_user_warehouses_assigned_by ON user_warehouses(assigned_by);
CREATE INDEX idx_user_warehouses_warehouse_status ON user_warehouses(warehouse_id, status);
```

Some indexes may be redundant when already covered by a composite index or primary key. The final index set should be validated against actual query plans.

---

## 8. Constraint Responsibility

| Constraint | Database | Application / Domain |
|------------|----------|----------------------|
| Primary Key | Yes | — |
| Foreign Key | Yes | — |
| NOT NULL | Yes | Yes |
| UNIQUE | Yes | Yes |
| CHECK | Yes | Yes |
| User/Warehouse Tenant consistency | Preferably | Yes |
| User/Role Tenant consistency | Preferably | Yes |
| One active Warehouse per User | Yes | Yes |
| Warehouse has Manager before Employee assignment | — | Yes |
| Manager can manage Employee | — | Yes |
| Permission authorization | — | Yes |
| Warehouse scope authorization | — | Yes |
| Audit creation | — | Yes |

---

## 9. ERD

```
┌──────────────────┐
│     tenants      │
├──────────────────┤
│ PK id            │
│ name             │
│ code             │
│ status           │
│ created_at       │
│ updated_at       │
└───────┬──────────┘
        │
        ├───────────────────────────┐
        │                           │
        ▼                           ▼
┌──────────────────┐       ┌──────────────────┐
│      users       │       │    warehouses    │
├──────────────────┤       ├──────────────────┤
│ PK id            │       │ PK id            │
│ FK tenant_id     │       │ FK tenant_id     │
│ username         │       │ name             │
│ email            │       │ code             │
│ password_hash    │       │ address          │
│ full_name        │       │ status           │
│ phone            │       │ created_at       │
│ status           │       │ updated_at       │
│ last_login_at    │       └────────┬─────────┘
│ created_at       │                │
│ updated_at       │                │
└──────┬───────────┘                │
       │                            │
       ├──────────────┐             │
       │              │             │
       ▼              ▼             ▼
┌──────────────┐  ┌──────────────────────┐
│ user_roles   │  │  user_warehouses     │
├──────────────┤  ├──────────────────────┤
│ PK user_id   │  │ PK user_id           │
│ PK role_id   │  │ PK warehouse_id      │
│ created_at   │  │ assigned_by          │
└──────┬───────┘  │ assigned_at          │
       │          │ status               │
       │          └──────────────────────┘
       ▼
┌──────────────────┐
│      roles       │
├──────────────────┤
│ PK id            │
│ FK tenant_id     │
│ code             │
│ name             │
│ description      │
│ created_at       │
│ updated_at       │
└──────┬───────────┘
       │
       ▼
┌──────────────────────┐
│  role_permissions    │
├──────────────────────┤
│ PK role_id           │
│ PK permission_id     │
│ created_at           │
└──────────┬───────────┘
           │
           ▼
┌──────────────────┐
│   permissions    │
├──────────────────┤
│ PK id            │
│ code             │
│ name             │
│ module           │
│ description      │
│ created_at       │
└──────────────────┘

users
  │
  ▼
┌──────────────────────┐
│  user_permissions   │
├──────────────────────┤
│ PK user_id           │
│ PK permission_id     │
│ effect               │
│ created_by           │
│ created_at           │
└──────────────────────┘
```

---

## 10. Architecture Decision Records

| ID | Decision | Reason |
|----|----------|--------|
| ADR-DB-001 | Use UUID as primary key | Suitable for distributed/SaaS architecture and avoids exposing sequential identifiers |
| ADR-DB-002 | Use normalized Role and Permission tables | Permissions are extensible atomic capabilities and should not be represented as a wide set of Boolean columns in users |
| ADR-DB-003 | Store User Permission overrides separately | Supports explicit ALLOW / DENY behavior without duplicating inherited Role Permissions |
| ADR-DB-004 | Do not store a direct role column in users | A User may have multiple Roles and Role assignment is an authorization concern |
| ADR-DB-005 | Do not create separate Identity tables for Manager and Employee | Manager and Employee are business roles assigned to generic Users |
| ADR-DB-006 | Do not require every Warehouse to have a Manager | Tenant can directly operate a Warehouse |
| ADR-DB-007 | Keep Warehouse assignment separate from User identity | A User can change Warehouse over time and assignment metadata/history must be tracked |

---

## 11. Tables Currently Covered

**Baseline (Identity & Authorization):**

1. `tenants`
2. `users`
3. `warehouses`
4. `roles`
5. `permissions`
6. `user_roles`
7. `role_permissions`
8. `user_permissions`
9. `user_warehouses`

**Not included yet:**

- `products`
- `inventory`
- `inbound`
- `outbound`
- `stock_transfers`
- `audit_logs`

These will be designed after their business requirements and workflows are finalized.
