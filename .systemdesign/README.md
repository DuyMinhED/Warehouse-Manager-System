# System Design - Smart WMS

Tai lieu thiet ke he thong cho **Smart Warehouse Management System (WMS)**.

## Database

| Document | Description |
|----------|-------------|
| [Identity & Authorization](database/identity-authorization.md) | Thiet ke database cho Tenant, User, Warehouse, Role, Permission |
| [Product Domain](database/product-domain.md) | Thiet ke database/entity cho Category, Unit, Product, ProductVariant, ProductBarcode, ProductUom |
| [Product Domain MySQL Schema](database/product-domain-schema.sql) | DDL MySQL cho Product Domain |

## Pham vi hien tai

Baseline **Identity & Authorization** gom 9 bang:

1. `tenants`
2. `users`
3. `warehouses`
4. `roles`
5. `permissions`
6. `user_roles`
7. `role_permissions`
8. `user_permissions`
9. `user_warehouses`

Baseline **Product Domain** gom 6 bang:

1. `categories`
2. `units`
3. `products`
4. `product_variants`
5. `product_barcodes`
6. `product_uoms`

Trong do `product_uoms` dung composite key `(variant_id, unit_id)` thong qua `ProductUomId`.

## Khong nam trong phase hien tai

Khong tao cac domain/entity sau trong Product Domain phase:

- `inventory`
- `inventory_movements`
- `warehouse_locations`
- `inbound_receipts`
- `outbound_orders`
- `stock_transfers`
- `stock_counts`
- `audit_logs`
