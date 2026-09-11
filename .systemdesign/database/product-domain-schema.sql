-- Smart WMS - Product Domain Schema
-- Database: MySQL
-- Document Version: 1.0

-- =============================================================================
-- categories
-- =============================================================================

CREATE TABLE categories (
    id          BINARY(16)    NOT NULL,
    tenant_id   BINARY(16)    NOT NULL,
    parent_id   BINARY(16)    NULL,
    code        VARCHAR(50)   NOT NULL,
    name        VARCHAR(150)  NOT NULL,
    description TEXT          NULL,
    status      VARCHAR(20)   NOT NULL DEFAULT 'ACTIVE',
    created_at  TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT pk_categories PRIMARY KEY (id),
    CONSTRAINT fk_categories_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id),
    CONSTRAINT fk_categories_parent FOREIGN KEY (parent_id) REFERENCES categories(id),
    CONSTRAINT uq_category_tenant_code UNIQUE (tenant_id, code),
    CONSTRAINT chk_categories_code_not_empty CHECK (length(trim(code)) > 0),
    CONSTRAINT chk_categories_name_not_empty CHECK (length(trim(name)) > 0),
    CONSTRAINT chk_categories_status CHECK (status IN ('ACTIVE', 'INACTIVE', 'ARCHIVED'))
);

CREATE INDEX idx_categories_tenant ON categories(tenant_id);
CREATE INDEX idx_categories_tenant_parent ON categories(tenant_id, parent_id);

-- =============================================================================
-- units
-- =============================================================================

CREATE TABLE units (
    id          BINARY(16)    NOT NULL,
    tenant_id   BINARY(16)    NOT NULL,
    code        VARCHAR(20)   NOT NULL,
    name        VARCHAR(100)  NOT NULL,
    symbol      VARCHAR(20)   NULL,
    type        VARCHAR(30)   NULL,
    status      VARCHAR(20)   NOT NULL DEFAULT 'ACTIVE',
    created_at  TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT pk_units PRIMARY KEY (id),
    CONSTRAINT fk_units_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id),
    CONSTRAINT uq_unit_tenant_code UNIQUE (tenant_id, code),
    CONSTRAINT chk_units_code_not_empty CHECK (length(trim(code)) > 0),
    CONSTRAINT chk_units_name_not_empty CHECK (length(trim(name)) > 0),
    CONSTRAINT chk_units_status CHECK (status IN ('ACTIVE', 'INACTIVE'))
);

CREATE INDEX idx_units_tenant ON units(tenant_id);

-- =============================================================================
-- products
-- =============================================================================

CREATE TABLE products (
    id          BINARY(16)    NOT NULL,
    tenant_id   BINARY(16)    NOT NULL,
    category_id BINARY(16)    NULL,
    name        VARCHAR(200)  NOT NULL,
    code        VARCHAR(50)   NOT NULL,
    description TEXT          NULL,
    brand       VARCHAR(100)  NULL,
    attributes  JSON          NULL,
    status      VARCHAR(20)   NOT NULL DEFAULT 'ACTIVE',
    created_at  TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT pk_products PRIMARY KEY (id),
    CONSTRAINT fk_products_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id),
    CONSTRAINT fk_products_category FOREIGN KEY (category_id) REFERENCES categories(id),
    CONSTRAINT uq_product_tenant_code UNIQUE (tenant_id, code),
    CONSTRAINT chk_products_name_not_empty CHECK (length(trim(name)) > 0),
    CONSTRAINT chk_products_code_not_empty CHECK (length(trim(code)) > 0),
    CONSTRAINT chk_products_status CHECK (status IN ('ACTIVE', 'INACTIVE', 'ARCHIVED'))
);

CREATE INDEX idx_products_tenant ON products(tenant_id);
CREATE INDEX idx_products_tenant_category ON products(tenant_id, category_id);
CREATE INDEX idx_products_tenant_status ON products(tenant_id, status);

-- =============================================================================
-- product_variants
-- =============================================================================

CREATE TABLE product_variants (
    id            BINARY(16)      NOT NULL,
    tenant_id     BINARY(16)      NOT NULL,
    product_id    BINARY(16)      NOT NULL,
    sku           VARCHAR(100)    NOT NULL,
    name          VARCHAR(200)    NULL,
    attributes    JSON            NULL,
    cost_price    DECIMAL(19,4)   NULL,
    selling_price DECIMAL(19,4)   NULL,
    weight        DECIMAL(19,6)   NULL,
    length        DECIMAL(19,6)   NULL,
    width         DECIMAL(19,6)   NULL,
    height        DECIMAL(19,6)   NULL,
    status        VARCHAR(20)     NOT NULL DEFAULT 'ACTIVE',
    created_at    TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT pk_product_variants PRIMARY KEY (id),
    CONSTRAINT fk_product_variants_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id),
    CONSTRAINT fk_product_variants_product FOREIGN KEY (product_id) REFERENCES products(id),
    CONSTRAINT uq_product_variant_tenant_sku UNIQUE (tenant_id, sku),
    CONSTRAINT chk_product_variants_sku_not_empty CHECK (length(trim(sku)) > 0),
    CONSTRAINT chk_product_variants_cost_price CHECK (cost_price IS NULL OR cost_price >= 0),
    CONSTRAINT chk_product_variants_selling_price CHECK (selling_price IS NULL OR selling_price >= 0),
    CONSTRAINT chk_product_variants_weight CHECK (weight IS NULL OR weight >= 0),
    CONSTRAINT chk_product_variants_length CHECK (length IS NULL OR length >= 0),
    CONSTRAINT chk_product_variants_width CHECK (width IS NULL OR width >= 0),
    CONSTRAINT chk_product_variants_height CHECK (height IS NULL OR height >= 0),
    CONSTRAINT chk_product_variants_status CHECK (status IN ('ACTIVE', 'INACTIVE', 'ARCHIVED'))
);

CREATE INDEX idx_product_variants_tenant ON product_variants(tenant_id);
CREATE INDEX idx_product_variants_product ON product_variants(product_id);
CREATE INDEX idx_product_variants_tenant_status ON product_variants(tenant_id, status);

-- =============================================================================
-- product_barcodes
-- =============================================================================

CREATE TABLE product_barcodes (
    id           BINARY(16)    NOT NULL,
    tenant_id    BINARY(16)    NOT NULL,
    variant_id   BINARY(16)    NOT NULL,
    barcode      VARCHAR(100)  NOT NULL,
    barcode_type VARCHAR(30)   NOT NULL,
    is_primary   BOOLEAN       NOT NULL DEFAULT FALSE,
    status       VARCHAR(20)   NOT NULL DEFAULT 'ACTIVE',
    created_at   TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at   TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT pk_product_barcodes PRIMARY KEY (id),
    CONSTRAINT fk_product_barcodes_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id),
    CONSTRAINT fk_product_barcodes_variant FOREIGN KEY (variant_id) REFERENCES product_variants(id),
    CONSTRAINT uq_product_barcode_tenant_barcode UNIQUE (tenant_id, barcode),
    CONSTRAINT chk_product_barcodes_barcode_not_empty CHECK (length(trim(barcode)) > 0),
    CONSTRAINT chk_product_barcodes_type CHECK (barcode_type IN ('EAN13', 'EAN8', 'UPC', 'CODE128', 'QR', 'INTERNAL')),
    CONSTRAINT chk_product_barcodes_status CHECK (status IN ('ACTIVE', 'INACTIVE'))
);

CREATE INDEX idx_product_barcodes_variant ON product_barcodes(variant_id);
CREATE INDEX idx_product_barcodes_tenant_status ON product_barcodes(tenant_id, status);

-- =============================================================================
-- product_uoms
-- =============================================================================

CREATE TABLE product_uoms (
    variant_id         BINARY(16)     NOT NULL,
    unit_id            BINARY(16)     NOT NULL,
    tenant_id          BINARY(16)     NOT NULL,
    conversion_factor  DECIMAL(19,6)  NOT NULL,
    is_base            BOOLEAN        NOT NULL DEFAULT FALSE,
    is_purchase_unit   BOOLEAN        NOT NULL DEFAULT FALSE,
    is_sales_unit      BOOLEAN        NOT NULL DEFAULT FALSE,
    is_inventory_unit  BOOLEAN        NOT NULL DEFAULT FALSE,
    status             VARCHAR(20)    NOT NULL DEFAULT 'ACTIVE',
    created_at         TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at         TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT pk_product_uoms PRIMARY KEY (variant_id, unit_id),
    CONSTRAINT fk_product_uoms_variant FOREIGN KEY (variant_id) REFERENCES product_variants(id),
    CONSTRAINT fk_product_uoms_unit FOREIGN KEY (unit_id) REFERENCES units(id),
    CONSTRAINT fk_product_uoms_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id),
    CONSTRAINT chk_product_uoms_conversion_factor CHECK (conversion_factor > 0),
    CONSTRAINT chk_product_uoms_status CHECK (status IN ('ACTIVE', 'INACTIVE'))
);

CREATE INDEX idx_product_uoms_tenant ON product_uoms(tenant_id);
CREATE INDEX idx_product_uoms_variant ON product_uoms(variant_id);

-- MySQL cannot enforce every cross-table same-tenant rule with simple CHECK
-- constraints. The application/domain layer must validate:
--
-- - products.category_id belongs to products.tenant_id
-- - product_variants.product_id belongs to product_variants.tenant_id
-- - product_barcodes.variant_id belongs to product_barcodes.tenant_id
-- - product_uoms.variant_id and product_uoms.unit_id both belong to product_uoms.tenant_id
-- - each Variant has exactly one base UOM
-- - each Variant has at most one primary Barcode
