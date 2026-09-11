-- Smart WMS — Identity & Authorization Schema
-- Database: PostgreSQL
-- Document Version: 1.1

-- =============================================================================
-- 3.1 tenants
-- =============================================================================

CREATE TABLE tenants (
    id          UUID        NOT NULL DEFAULT gen_random_uuid(),
    name        VARCHAR(150) NOT NULL,
    code        VARCHAR(50) NOT NULL,
    status      VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at  TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT pk_tenants PRIMARY KEY (id),
    CONSTRAINT uq_tenants_code UNIQUE (code),
    CONSTRAINT chk_tenants_name_not_empty CHECK (length(trim(name)) > 0),
    CONSTRAINT chk_tenants_code_format CHECK (code ~ '^[A-Z0-9_-]+$'),
    CONSTRAINT chk_tenants_status CHECK (status IN ('ACTIVE', 'SUSPENDED', 'INACTIVE'))
);

-- =============================================================================
-- 3.2 users
-- =============================================================================

CREATE TABLE users (
    id              UUID        NOT NULL DEFAULT gen_random_uuid(),
    tenant_id       UUID,
    username        VARCHAR(100) NOT NULL,
    email           VARCHAR(255) NOT NULL,
    password_hash   VARCHAR(255) NOT NULL,
    full_name       VARCHAR(150) NOT NULL,
    phone           VARCHAR(20),
    status          VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    last_login_at   TIMESTAMP,
    created_at      TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT pk_users PRIMARY KEY (id),
    CONSTRAINT fk_users_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id),
    CONSTRAINT uq_users_username UNIQUE (username),
    CONSTRAINT uq_users_email UNIQUE (email),
    CONSTRAINT chk_users_full_name_not_empty CHECK (length(trim(full_name)) > 0),
    CONSTRAINT chk_users_status CHECK (status IN ('ACTIVE', 'INACTIVE', 'LOCKED', 'PENDING'))
);

-- =============================================================================
-- 3.3 warehouses
-- =============================================================================

CREATE TABLE warehouses (
    id          UUID        NOT NULL DEFAULT gen_random_uuid(),
    tenant_id   UUID        NOT NULL,
    name        VARCHAR(150) NOT NULL,
    code        VARCHAR(50) NOT NULL,
    address     VARCHAR(255),
    status      VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at  TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT pk_warehouses PRIMARY KEY (id),
    CONSTRAINT fk_warehouses_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id),
    CONSTRAINT uq_warehouses_tenant_code UNIQUE (tenant_id, code),
    CONSTRAINT chk_warehouses_name_not_empty CHECK (length(trim(name)) > 0),
    CONSTRAINT chk_warehouses_code_not_empty CHECK (length(trim(code)) > 0),
    CONSTRAINT chk_warehouses_status CHECK (status IN ('ACTIVE', 'INACTIVE', 'ARCHIVED'))
);

-- =============================================================================
-- 3.4 roles
-- =============================================================================

CREATE TABLE roles (
    id          UUID        NOT NULL DEFAULT gen_random_uuid(),
    tenant_id   UUID,
    code        VARCHAR(50) NOT NULL,
    name        VARCHAR(100) NOT NULL,
    description TEXT,
    created_at  TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT pk_roles PRIMARY KEY (id),
    CONSTRAINT fk_roles_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id)
);

CREATE UNIQUE INDEX uq_system_role_code
    ON roles(code)
    WHERE tenant_id IS NULL;

CREATE UNIQUE INDEX uq_tenant_role_code
    ON roles(tenant_id, code)
    WHERE tenant_id IS NOT NULL;

-- =============================================================================
-- 3.5 permissions
-- =============================================================================

CREATE TABLE permissions (
    id          UUID        NOT NULL DEFAULT gen_random_uuid(),
    code        VARCHAR(100) NOT NULL,
    name        VARCHAR(150) NOT NULL,
    module      VARCHAR(50) NOT NULL,
    description TEXT,
    created_at  TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT pk_permissions PRIMARY KEY (id),
    CONSTRAINT uq_permissions_code UNIQUE (code),
    CONSTRAINT chk_permissions_code_not_empty CHECK (length(trim(code)) > 0),
    CONSTRAINT chk_permissions_name_not_empty CHECK (length(trim(name)) > 0),
    CONSTRAINT chk_permissions_module_not_empty CHECK (length(trim(module)) > 0)
);

-- =============================================================================
-- 3.6 user_roles
-- =============================================================================

CREATE TABLE user_roles (
    user_id     UUID        NOT NULL,
    role_id     UUID        NOT NULL,
    created_at  TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT pk_user_roles PRIMARY KEY (user_id, role_id),
    CONSTRAINT fk_user_roles_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT fk_user_roles_role FOREIGN KEY (role_id) REFERENCES roles(id)
);

-- =============================================================================
-- 3.7 role_permissions
-- =============================================================================

CREATE TABLE role_permissions (
    role_id         UUID        NOT NULL,
    permission_id   UUID        NOT NULL,
    created_at      TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT pk_role_permissions PRIMARY KEY (role_id, permission_id),
    CONSTRAINT fk_role_permissions_role FOREIGN KEY (role_id) REFERENCES roles(id),
    CONSTRAINT fk_role_permissions_permission FOREIGN KEY (permission_id) REFERENCES permissions(id)
);

-- =============================================================================
-- 3.8 user_permissions
-- =============================================================================

CREATE TABLE user_permissions (
    user_id         UUID        NOT NULL,
    permission_id   UUID        NOT NULL,
    effect          VARCHAR(10) NOT NULL,
    created_by      UUID,
    created_at      TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT pk_user_permissions PRIMARY KEY (user_id, permission_id),
    CONSTRAINT fk_user_permissions_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT fk_user_permissions_permission FOREIGN KEY (permission_id) REFERENCES permissions(id),
    CONSTRAINT fk_user_permissions_created_by FOREIGN KEY (created_by) REFERENCES users(id),
    CONSTRAINT chk_user_permissions_effect CHECK (effect IN ('ALLOW', 'DENY'))
);

-- =============================================================================
-- 3.9 user_warehouses
-- =============================================================================

CREATE TABLE user_warehouses (
    user_id         UUID        NOT NULL,
    warehouse_id    UUID        NOT NULL,
    assigned_by     UUID,
    assigned_at     TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    status          VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',

    CONSTRAINT pk_user_warehouses PRIMARY KEY (user_id, warehouse_id),
    CONSTRAINT fk_user_warehouses_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT fk_user_warehouses_warehouse FOREIGN KEY (warehouse_id) REFERENCES warehouses(id),
    CONSTRAINT fk_user_warehouses_assigned_by FOREIGN KEY (assigned_by) REFERENCES users(id),
    CONSTRAINT chk_user_warehouses_status CHECK (status IN ('ACTIVE', 'INACTIVE'))
);

CREATE UNIQUE INDEX uq_user_active_warehouse
    ON user_warehouses(user_id)
    WHERE status = 'ACTIVE';

-- =============================================================================
-- 7. Indexing Strategy
-- =============================================================================

CREATE INDEX idx_users_tenant ON users(tenant_id);
CREATE INDEX idx_users_tenant_status ON users(tenant_id, status);

CREATE INDEX idx_warehouses_tenant ON warehouses(tenant_id);
CREATE INDEX idx_warehouses_tenant_status ON warehouses(tenant_id, status);

CREATE INDEX idx_roles_tenant ON roles(tenant_id);

CREATE INDEX idx_permissions_module ON permissions(module);

CREATE INDEX idx_user_roles_role ON user_roles(role_id);

CREATE INDEX idx_role_permissions_permission ON role_permissions(permission_id);

CREATE INDEX idx_user_permissions_permission ON user_permissions(permission_id);
CREATE INDEX idx_user_permissions_created_by ON user_permissions(created_by);

CREATE INDEX idx_user_warehouses_warehouse ON user_warehouses(warehouse_id);
CREATE INDEX idx_user_warehouses_assigned_by ON user_warehouses(assigned_by);
CREATE INDEX idx_user_warehouses_warehouse_status ON user_warehouses(warehouse_id, status);
