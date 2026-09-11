# Database Design - Product Domain

| | |
|---|---|
| **Document Version** | 1.0 |
| **Status** | Draft / Baseline |
| **Database** | MySQL |
| **Scope** | Category, Unit, Product, ProductVariant, ProductBarcode, ProductUom |

---

## 1. Overview

This document describes the agreed Product Domain design for the Smart Warehouse Management System.

The Product Domain is tenant-scoped and is the foundation for the future Inventory Domain. Product data is shared at Tenant level, not Warehouse level.

Core ownership model:

```text
Tenant
  -> Category
  -> Unit
  -> Product
      -> ProductVariant / SKU
          -> ProductBarcode
          -> ProductUom
```

Future inventory flow:

```text
Tenant
  -> Product
      -> ProductVariant / SKU
          -> Inventory
              -> Warehouse
```

Important decisions:

- Product does not belong directly to Warehouse.
- Product does not store quantity.
- ProductVariant is the SKU-level item referenced by future inventory and warehouse transactions.
- Flexible product attributes are stored as MySQL JSON.
- Product is not moved to MongoDB for flexible attributes.

---

## 2. Tables

### 2.1 `categories`

Stores tenant-owned product categories. Categories may form a tree.

| Column | Type | Null | Default | Constraints | Description |
|--------|------|------|---------|-------------|-------------|
| `id` | UUID | No | - | PK | Category identifier |
| `tenant_id` | UUID | No | - | FK -> `tenants.id` | Owner Tenant |
| `parent_id` | UUID | Yes | - | FK -> `categories.id` | Parent Category |
| `code` | VARCHAR(50) | No | - | UNIQUE per Tenant | Category code |
| `name` | VARCHAR(150) | No | - | CHECK | Category name |
| `description` | TEXT | Yes | - | - | Description |
| `status` | VARCHAR(20) | No | `ACTIVE` | CHECK | `ACTIVE`, `INACTIVE`, `ARCHIVED` |
| `created_at` | TIMESTAMP | No | current timestamp | - | Creation timestamp |
| `updated_at` | TIMESTAMP | No | current timestamp | - | Last update timestamp |

Constraints:

- `PRIMARY KEY (id)`
- `FOREIGN KEY (tenant_id) REFERENCES tenants(id)`
- `FOREIGN KEY (parent_id) REFERENCES categories(id)`
- `UNIQUE (tenant_id, code)`
- `CHECK (length(trim(code)) > 0)`
- `CHECK (length(trim(name)) > 0)`
- `CHECK (status IN ('ACTIVE', 'INACTIVE', 'ARCHIVED'))`

Business rules:

- Parent Category must belong to the same Tenant.
- Category cannot be its own parent.
- Category tree must not contain cycles.
- Do not physically delete a Category that is used by Product.

### 2.2 `units`

Stores tenant-owned units of measure.

| Column | Type | Null | Default | Constraints | Description |
|--------|------|------|---------|-------------|-------------|
| `id` | UUID | No | - | PK | Unit identifier |
| `tenant_id` | UUID | No | - | FK -> `tenants.id` | Owner Tenant |
| `code` | VARCHAR(20) | No | - | UNIQUE per Tenant | Unit code |
| `name` | VARCHAR(100) | No | - | CHECK | Unit name |
| `symbol` | VARCHAR(20) | Yes | - | - | Display symbol |
| `type` | VARCHAR(30) | Yes | - | - | Unit type |
| `status` | VARCHAR(20) | No | `ACTIVE` | CHECK | `ACTIVE`, `INACTIVE` |
| `created_at` | TIMESTAMP | No | current timestamp | - | Creation timestamp |
| `updated_at` | TIMESTAMP | No | current timestamp | - | Last update timestamp |

Constraints:

- `PRIMARY KEY (id)`
- `FOREIGN KEY (tenant_id) REFERENCES tenants(id)`
- `UNIQUE (tenant_id, code)`
- `CHECK (length(trim(code)) > 0)`
- `CHECK (length(trim(name)) > 0)`
- `CHECK (status IN ('ACTIVE', 'INACTIVE'))`

Business rule:

- Do not physically delete a Unit that is used by ProductUom or historical transactions.

### 2.3 `products`

Stores tenant-owned product masters.

| Column | Type | Null | Default | Constraints | Description |
|--------|------|------|---------|-------------|-------------|
| `id` | UUID | No | - | PK | Product identifier |
| `tenant_id` | UUID | No | - | FK -> `tenants.id` | Owner Tenant |
| `category_id` | UUID | Yes | - | FK -> `categories.id` | Optional Category |
| `name` | VARCHAR(200) | No | - | CHECK | Product name |
| `code` | VARCHAR(50) | No | - | UNIQUE per Tenant | Product code |
| `description` | TEXT | Yes | - | - | Description |
| `brand` | VARCHAR(100) | Yes | - | - | Brand |
| `attributes` | JSON | Yes | - | - | Flexible product-level attributes |
| `status` | VARCHAR(20) | No | `ACTIVE` | CHECK | `ACTIVE`, `INACTIVE`, `ARCHIVED` |
| `created_at` | TIMESTAMP | No | current timestamp | - | Creation timestamp |
| `updated_at` | TIMESTAMP | No | current timestamp | - | Last update timestamp |

Constraints:

- `PRIMARY KEY (id)`
- `FOREIGN KEY (tenant_id) REFERENCES tenants(id)`
- `FOREIGN KEY (category_id) REFERENCES categories(id)`
- `UNIQUE (tenant_id, code)`
- `CHECK (length(trim(name)) > 0)`
- `CHECK (length(trim(code)) > 0)`
- `CHECK (status IN ('ACTIVE', 'INACTIVE', 'ARCHIVED'))`

Business rules:

- Category must belong to the same Tenant as Product.
- Product should have at least one ProductVariant.
- Product must not store inventory quantity.
- Product must not store `warehouse_id`.
- Product that has participated in transactions should not be physically deleted.

### 2.4 `product_variants`

Stores SKU-level product variants. This is the entity future inventory and warehouse transactions should reference.

| Column | Type | Null | Default | Constraints | Description |
|--------|------|------|---------|-------------|-------------|
| `id` | UUID | No | - | PK | Variant identifier |
| `tenant_id` | UUID | No | - | FK -> `tenants.id` | Owner Tenant |
| `product_id` | UUID | No | - | FK -> `products.id` | Parent Product |
| `sku` | VARCHAR(100) | No | - | UNIQUE per Tenant | SKU |
| `name` | VARCHAR(200) | Yes | - | - | Variant display name |
| `attributes` | JSON | Yes | - | - | Flexible variant-level attributes |
| `cost_price` | DECIMAL(19,4) | Yes | - | CHECK >= 0 | Cost price |
| `selling_price` | DECIMAL(19,4) | Yes | - | CHECK >= 0 | Selling price |
| `weight` | DECIMAL(19,6) | Yes | - | CHECK >= 0 | Weight |
| `length` | DECIMAL(19,6) | Yes | - | CHECK >= 0 | Length |
| `width` | DECIMAL(19,6) | Yes | - | CHECK >= 0 | Width |
| `height` | DECIMAL(19,6) | Yes | - | CHECK >= 0 | Height |
| `status` | VARCHAR(20) | No | `ACTIVE` | CHECK | `ACTIVE`, `INACTIVE`, `ARCHIVED` |
| `created_at` | TIMESTAMP | No | current timestamp | - | Creation timestamp |
| `updated_at` | TIMESTAMP | No | current timestamp | - | Last update timestamp |

Constraints:

- `PRIMARY KEY (id)`
- `FOREIGN KEY (tenant_id) REFERENCES tenants(id)`
- `FOREIGN KEY (product_id) REFERENCES products(id)`
- `UNIQUE (tenant_id, sku)`
- `CHECK (length(trim(sku)) > 0)`
- `CHECK (cost_price IS NULL OR cost_price >= 0)`
- `CHECK (selling_price IS NULL OR selling_price >= 0)`
- `CHECK (weight IS NULL OR weight >= 0)`
- `CHECK (length IS NULL OR length >= 0)`
- `CHECK (width IS NULL OR width >= 0)`
- `CHECK (height IS NULL OR height >= 0)`
- `CHECK (status IN ('ACTIVE', 'INACTIVE', 'ARCHIVED'))`

Business rules:

- Product and ProductVariant must belong to the same Tenant.
- Variant that has participated in transactions should not be physically deleted.
- Inactive Variant must not be used for new transactions.
- Measurement unit semantics are not hard-coded in this phase.

### 2.5 `product_barcodes`

Stores barcodes for product variants.

| Column | Type | Null | Default | Constraints | Description |
|--------|------|------|---------|-------------|-------------|
| `id` | UUID | No | - | PK | Barcode identifier |
| `tenant_id` | UUID | No | - | FK -> `tenants.id` | Owner Tenant |
| `variant_id` | UUID | No | - | FK -> `product_variants.id` | Variant |
| `barcode` | VARCHAR(100) | No | - | UNIQUE per Tenant | Barcode value |
| `barcode_type` | VARCHAR(30) | No | - | CHECK | `EAN13`, `EAN8`, `UPC`, `CODE128`, `QR`, `INTERNAL` |
| `is_primary` | BOOLEAN | No | false | - | Primary barcode marker |
| `status` | VARCHAR(20) | No | `ACTIVE` | CHECK | `ACTIVE`, `INACTIVE` |
| `created_at` | TIMESTAMP | No | current timestamp | - | Creation timestamp |
| `updated_at` | TIMESTAMP | No | current timestamp | - | Last update timestamp |

Constraints:

- `PRIMARY KEY (id)`
- `FOREIGN KEY (tenant_id) REFERENCES tenants(id)`
- `FOREIGN KEY (variant_id) REFERENCES product_variants(id)`
- `UNIQUE (tenant_id, barcode)`
- `CHECK (length(trim(barcode)) > 0)`
- `CHECK (barcode_type IN ('EAN13', 'EAN8', 'UPC', 'CODE128', 'QR', 'INTERNAL'))`
- `CHECK (status IN ('ACTIVE', 'INACTIVE'))`

Business rules:

- ProductBarcode and ProductVariant must belong to the same Tenant.
- A Variant may have multiple Barcodes.
- A Variant should have at most one primary Barcode.
- Inactive Barcode must not be used for new transactions.

### 2.6 `product_uoms`

Stores available units of measure for a Variant.

`product_uoms` uses a composite identifier represented in Java by `ProductUomId`.

| Column | Type | Null | Default | Constraints | Description |
|--------|------|------|---------|-------------|-------------|
| `variant_id` | UUID | No | - | PK, FK -> `product_variants.id` | Variant |
| `unit_id` | UUID | No | - | PK, FK -> `units.id` | Unit |
| `tenant_id` | UUID | No | - | FK -> `tenants.id` | Owner Tenant |
| `conversion_factor` | DECIMAL(19,6) | No | - | CHECK > 0 | Conversion factor to base unit |
| `is_base` | BOOLEAN | No | false | - | Base unit marker |
| `is_purchase_unit` | BOOLEAN | No | false | - | Purchase unit marker |
| `is_sales_unit` | BOOLEAN | No | false | - | Sales unit marker |
| `is_inventory_unit` | BOOLEAN | No | false | - | Inventory unit marker |
| `status` | VARCHAR(20) | No | `ACTIVE` | CHECK | `ACTIVE`, `INACTIVE` |
| `created_at` | TIMESTAMP | No | current timestamp | - | Creation timestamp |
| `updated_at` | TIMESTAMP | No | current timestamp | - | Last update timestamp |

Constraints:

- `PRIMARY KEY (variant_id, unit_id)`
- `FOREIGN KEY (variant_id) REFERENCES product_variants(id)`
- `FOREIGN KEY (unit_id) REFERENCES units(id)`
- `FOREIGN KEY (tenant_id) REFERENCES tenants(id)`
- `CHECK (conversion_factor > 0)`
- `CHECK (status IN ('ACTIVE', 'INACTIVE'))`

Business rules:

- Variant and Unit must belong to the same Tenant.
- `tenant_id` must match both Variant Tenant and Unit Tenant.
- Each Variant must have exactly one base Unit.
- If `is_base = true`, `conversion_factor` must equal `1`.
- Each Variant should have only one `is_base = true` row.
- Do not change `conversion_factor` after it has been used in historical transactions.

---

## 3. Relationship Summary

| Parent | Child | Relationship | Rule |
|--------|-------|--------------|------|
| Tenant | Category | 1 : N | Category belongs to one Tenant |
| Tenant | Unit | 1 : N | Unit belongs to one Tenant |
| Tenant | Product | 1 : N | Product belongs to one Tenant |
| Category | Category | 1 : N | Category tree through `parent_id` |
| Category | Product | 1 : N | Product may belong to a Category |
| Product | ProductVariant | 1 : N | Product has SKU variants |
| ProductVariant | ProductBarcode | 1 : N | Variant may have many Barcodes |
| ProductVariant | ProductUom | 1 : N | Variant may have many Units of measure |
| Unit | ProductUom | 1 : N | Unit may be used by many Variant UOM rows |

JPA mapping guidance:

- Use `FetchType.LAZY` for `ManyToOne` relationships unless a use case proves otherwise.
- Do not add bidirectional collections unless required by the domain or query workflow.
- Do not use Lombok `@Data` on JPA entities.
- Use explicit `@EqualsAndHashCode(onlyExplicitlyIncluded = true)` and avoid relationships/collections in equality.
- Use explicit `@ToString(onlyExplicitlyIncluded = true)` and avoid lazy relationships/sensitive fields.

---

## 4. Tenant Isolation

Tenant isolation is mandatory.

Invalid relationships:

- Tenant A Product -> Tenant B Category
- Tenant A ProductVariant -> Tenant B Product
- Tenant A ProductBarcode -> Tenant B ProductVariant
- Tenant A ProductUom -> Tenant B ProductVariant
- Tenant A ProductUom -> Tenant B Unit

Constraint responsibility:

| Rule | Database | Application / Domain |
|------|----------|----------------------|
| Direct FK existence | Yes | - |
| Tenant-scoped unique constraints | Yes | Yes |
| Product/Category same Tenant | Preferably | Yes |
| ProductVariant/Product same Tenant | Preferably | Yes |
| ProductBarcode/Variant same Tenant | Preferably | Yes |
| ProductUom/Variant/Unit same Tenant | Preferably | Yes |
| One primary Barcode per Variant | Preferably | Yes |
| One base Unit per Variant | Preferably | Yes |
| Category tree cycle prevention | - | Yes |

Do not rely on UI filtering for tenant isolation.

---

## 5. Indexing Strategy

Recommended indexes:

```sql
CREATE INDEX idx_categories_tenant ON categories(tenant_id);
CREATE INDEX idx_categories_tenant_parent ON categories(tenant_id, parent_id);

CREATE INDEX idx_units_tenant ON units(tenant_id);

CREATE INDEX idx_products_tenant ON products(tenant_id);
CREATE INDEX idx_products_tenant_category ON products(tenant_id, category_id);
CREATE INDEX idx_products_tenant_status ON products(tenant_id, status);

CREATE INDEX idx_product_variants_tenant ON product_variants(tenant_id);
CREATE INDEX idx_product_variants_product ON product_variants(product_id);
CREATE INDEX idx_product_variants_tenant_status ON product_variants(tenant_id, status);

CREATE INDEX idx_product_barcodes_variant ON product_barcodes(variant_id);
CREATE INDEX idx_product_barcodes_tenant_status ON product_barcodes(tenant_id, status);

CREATE INDEX idx_product_uoms_tenant ON product_uoms(tenant_id);
CREATE INDEX idx_product_uoms_variant ON product_uoms(variant_id);
```

Unique constraints:

```sql
UNIQUE (tenant_id, code)      -- categories
UNIQUE (tenant_id, code)      -- units
UNIQUE (tenant_id, code)      -- products
UNIQUE (tenant_id, sku)       -- product_variants
UNIQUE (tenant_id, barcode)   -- product_barcodes
PRIMARY KEY (variant_id, unit_id) -- product_uoms
```

---

## 6. Architecture Decision Records

| ID | Decision | Reason |
|----|----------|--------|
| ADR-PD-001 | Product belongs to Tenant, not Warehouse | Product catalog is shared tenant-level master data; stock state belongs to Inventory |
| ADR-PD-002 | Product does not store quantity | Quantity is warehouse/inventory state, not product master data |
| ADR-PD-003 | ProductVariant is the SKU-level transaction target | Future Inventory, Inbound, Outbound, Transfer, and Count items should reference Variant/SKU |
| ADR-PD-004 | Use MySQL JSON for flexible attributes | Supports custom attributes without over-normalizing or moving Product to MongoDB |
| ADR-PD-005 | Use BigDecimal for money and measurement | Avoids floating point precision errors |
| ADR-PD-006 | Use enum string persistence | Avoids ordinal persistence risks when enum order changes |
| ADR-PD-007 | Use composite key for ProductUom | Database identity is `(variant_id, unit_id)`, represented by `ProductUomId` |

---

## 7. Implementation Notes

Current Java entity alignment:

- `ProductUomId` exists under `com.duyminh.wms.entity.id`.
- `ProductUom` uses `@EmbeddedId` and `@MapsId` for `variant` and `unit`.
- Product and ProductVariant use Hibernate JSON mapping through `@JdbcTypeCode(SqlTypes.JSON)`.
- Money fields use `BigDecimal` with `DECIMAL(19,4)`.
- Measurement fields use `BigDecimal` with `DECIMAL(19,6)`.
- Product Domain enums are persisted with `@Enumerated(EnumType.STRING)`.
- Entity equality and string rendering avoid relationship traversal.

Open implementation items:

- Add Product Domain services/repositories.
- Enforce same-tenant rules in service/domain layer.
- Enforce one base UOM per Variant.
- Enforce at most one primary Barcode per Variant.
- Define delete/archive behavior before transaction domains are introduced.
