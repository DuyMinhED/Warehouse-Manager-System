# WMS Technology Constraints

**Project:** Smart Warehouse Management System (WMS)  
**Purpose:** Quy định các ràng buộc công nghệ bắt buộc khi thiết kế, phát triển, review và mở rộng hệ thống.

---

## 1. Nguyên tắc chung

WMS là hệ thống SaaS multi-tenant. Mọi quyết định kỹ thuật phải ưu tiên:

1. Tenant isolation.
2. Tính đúng đắn của inventory.
3. Referential integrity.
4. Transaction consistency.
5. Khả năng audit.
6. Khả năng mở rộng theo domain.
7. Không phá vỡ historical data.

Không được thay đổi stack hoặc kiến trúc cốt lõi chỉ vì một thư viện/framework khác “tiện hơn” nếu chưa có quyết định kiến trúc chính thức.

## 2. Backend bắt buộc

Backend sử dụng:

- Java.
- Spring Boot.
- Spring Data JPA.
- Hibernate.
- Maven hoặc build tool hiện tại của project.

Ràng buộc:

- Không thay Spring Boot bằng framework backend khác.
- Không bypass JPA/Hibernate cho core business domain nếu chưa có lý do kỹ thuật được phê duyệt.
- Không thêm framework lớn chỉ để giải quyết một use case nhỏ.
- Mọi dependency mới phải có mục đích rõ ràng và không trùng chức năng với dependency hiện có.

Phiên bản cụ thể của Java, Spring Boot và Hibernate phải theo cấu hình build hiện tại của project. Không tự ý nâng major version trong một task không liên quan.

## 3. Database bắt buộc

Database chính:

**MySQL**

Không sử dụng PostgreSQL-specific feature trong schema/migration.

Đặc biệt không dùng:

```sql
CREATE UNIQUE INDEX ... WHERE ...
```

vì MySQL không hỗ trợ PostgreSQL partial unique index syntax.

Cho conditional uniqueness, ưu tiên theo thứ tự:

1. Service/domain validation.
2. Transactional locking.
3. MySQL generated column / functional unique strategy nếu thực sự cần.
4. Không giả lập PostgreSQL syntax.

## 4. Không sử dụng MongoDB cho core domain

Các domain sau phải nằm trong relational database MySQL:

- Identity & Authorization.
- Warehouse.
- Product.
- Inventory.
- Supplier / Inbound.
- Outbound.
- Reservation.
- Stock Transfer.
- Stock Count.
- Audit metadata cốt lõi.

Product và ProductVariant có thể dùng MySQL JSON cho flexible attributes.

Không chuyển Product hoặc Inventory sang MongoDB chỉ vì schema có trường động.

MongoDB chỉ được cân nhắc trong tương lai cho use case độc lập như PIM/catalog metadata cực kỳ động, sau khi có ADR riêng.

## 5. Multi-tenant

Mọi business entity thuộc Tenant phải có tenant boundary rõ ràng.

Không được cho phép cross-tenant relation, ví dụ:

- User Tenant A → Warehouse Tenant B.
- Product Tenant A → Category Tenant B.
- Inventory Tenant A → Variant Tenant B.
- Receipt Tenant A → Supplier Tenant B.
- Transfer Tenant A → Warehouse Tenant B.

Database FK không đủ bảo vệ toàn bộ tenant consistency.

Bắt buộc có validation ở Service/Domain Layer.

Admin platform không mặc nhiên có quyền đọc business data của Tenant.

## 6. UUID

Identifier của các entity chính sử dụng UUID.

MySQL không có native UUID type như PostgreSQL.

Phải giữ UUID storage strategy đã được project chọn, ví dụ:

- `CHAR(36)`, hoặc
- `BINARY(16)`, hoặc
- Hibernate-supported UUID mapping.

Không tự ý migrate UUID storage toàn project trong một feature task.

Mọi thay đổi UUID strategy phải có migration plan.

## 7. Numeric type

### Quantity / Measurement

Java:

```java
BigDecimal
```

Database:

```sql
DECIMAL(19,6)
```

Áp dụng cho:

- stock quantity.
- received quantity.
- fulfilled quantity.
- transfer quantity.
- conversion factor.
- weight.
- dimensions.

### Money

Java:

```java
BigDecimal
```

Database mặc định:

```sql
DECIMAL(19,4)
```

Không dùng:

- `float`
- `double`

cho money hoặc inventory quantity.

## 8. Enum

Enum bắt buộc persist bằng tên:

```java
@Enumerated(EnumType.STRING)
```

Không sử dụng:

```java
EnumType.ORDINAL
```

## 9. JSON

MySQL JSON được phép dùng cho:

- `Product.attributes`
- `ProductVariant.attributes`
- `AuditLog.beforeData`
- `AuditLog.afterData`
- `AuditLog.metadata`

Ưu tiên mapping JSON native mà Hibernate version hiện tại hỗ trợ.

Không dùng JSON để thay thế quan hệ relational quan trọng.

## 10. JPA relationship

Mặc định quan hệ reference sử dụng:

```java
@ManyToOne(fetch = FetchType.LAZY)
```

Không dùng `EAGER` nếu không có use case rõ ràng.

Không tạo bidirectional relationship chỉ để “đủ hai chiều”.

Tránh object graph lớn có thể gây:

- N+1 query.
- accidental lazy loading.
- serialization recursion.
- memory overhead.

## 11. Cascade

Không sử dụng:

```java
CascadeType.ALL
```

một cách mặc định.

Đặc biệt không cascade remove historical data từ:

- Tenant.
- Warehouse.
- Product.
- ProductVariant.
- User.
- Supplier.
- InventoryLot.
- InventorySerial.

## 12. Historical data

Không physical-delete trong normal business flow:

- InventoryMovement.
- Completed InboundReceipt.
- InboundReceiving.
- Completed OutboundOrder.
- OutboundFulfillment.
- StockTransfer history.
- Completed StockCount.
- AuditLog.
- Lot/Serial đã tham gia transaction.

Nếu cần hủy nghiệp vụ đã post stock, dùng reversal/adjustment thay vì sửa/xóa lịch sử.

## 13. Lombok

Không sử dụng `@Data` mù quáng trên JPA Entity.

Phải tránh:

- relationship trong `equals/hashCode`.
- lazy loading trong `toString`.
- recursive `toString`.
- sensitive data trong `toString`.

`passwordHash` tuyệt đối không được xuất hiện trong log/toString.

## 14. Bean Validation

Có thể sử dụng:

- `@NotNull`
- `@NotBlank`
- `@Size`
- `@Email`
- `@Positive`
- `@PositiveOrZero`

Nhưng Bean Validation không thay thế Database Constraint hoặc Business Rule.

## 15. Inventory

Product không phải Inventory.

Không được thêm vào `Product` hoặc `ProductVariant`:

- `quantity`
- `stockQuantity`
- `availableQuantity`
- `reservedQuantity`
- `warehouseId`

Inventory target là `ProductVariant / SKU`.

Inventory scope:

```text
Tenant + Warehouse + ProductVariant
```

Hiện tại không có:

- WarehouseLocation.
- Zone.
- Rack.
- Shelf.
- Bin.

Warehouse chỉ lưu `address`.

## 16. Negative inventory

Negative inventory bị cấm.

Bắt buộc:

```text
InventoryBalance.onHandQuantity >= 0
InventoryLot.onHandQuantity >= 0
```

Outbound và Transfer Dispatch phải kiểm tra available stock trước khi cập nhật.

Không được dựa duy nhất vào CHECK constraint.

## 17. Inventory consistency

Mọi thay đổi stock phải cập nhật trong cùng business transaction:

```text
InventoryBalance
+
InventoryLot / InventorySerial nếu có
+
InventoryMovement
```

Không được update balance mà không ghi movement.

## 18. InventoryMovement

`InventoryMovement` là stock ledger và phải được coi là immutable sau khi post.

Ví dụ:

```text
INBOUND       +100
OUTBOUND       -20
TRANSFER_IN    +10
TRANSFER_OUT   -10
```

## 19. Reservation

Không lưu trực tiếp `availableQuantity` hoặc `reservedQuantity` trong InventoryBalance ở baseline.

Available quantity:

```text
available = onHand - ACTIVE reservations
```

Reservation với serial: `quantity = 1`.

## 20. UOM

Inventory balance lưu normalized/base quantity.

Transaction phải snapshot:

- entered quantity.
- UOM.
- conversion factor.
- base quantity.

Nếu conversion factor thay đổi sau này, historical transaction không được tính lại.

## 21. ProductUom

Thiết kế bắt buộc:

```text
ProductUom
- id UUID PK
- tenant_id
- variant_id
- unit_id
- conversion_factor
...
```

Unique:

```text
(variant_id, unit_id)
```

Không dùng composite PK cho ProductUom.

Không tạo `ProductUomId`.

## 22. Lot / Batch / Serial

Hệ thống hỗ trợ tracking:

```text
NONE
LOT
SERIAL
LOT_AND_SERIAL
```

Serial đại diện cho đúng một physical unit và không có quantity.

Expiry date chủ yếu thuộc Lot/Batch.

## 23. Inbound

Inbound hỗ trợ partial receiving.

Một ReceiptItem có thể có nhiều Receiving.

Mỗi Receiving phải snapshot:

- quantity.
- UOM.
- conversion factor.
- base quantity.
- unit cost nếu có.

## 24. Outbound

Outbound hỗ trợ partial fulfillment.

Outbound phải kiểm tra reservation/available stock.

Fulfillment phải cùng transaction với inventory update và movement.

## 25. Stock Transfer

Transfer có trạng thái `IN_TRANSIT`.

Dispatch giảm source inventory và tạo `TRANSFER_OUT`.

Receive tăng destination inventory và tạo `TRANSFER_IN`.

Không cộng destination stock ngay khi mới dispatch.

Lot/Serial giữ identity cũ khi chuyển kho.

## 26. Stock Count

Stock Count phải snapshot system quantity.

Variance:

```text
countedQuantity - systemQuantity
```

Khi apply:

- variance > 0 → `STOCK_COUNT_IN`
- variance < 0 → `STOCK_COUNT_OUT`

## 27. Audit

`AuditLog` khác `InventoryMovement`.

AuditLog là user/system action ledger và append-only trong normal flow.

Tenant Audit là Tenant business data.

## 28. Transaction

Các nghiệp vụ bắt buộc dùng transaction:

- Inbound Receiving.
- Outbound Fulfillment.
- Reservation creation/consume/release khi ảnh hưởng stock availability.
- Stock Transfer Dispatch.
- Stock Transfer Receiving.
- Stock Count Apply.
- Inventory Adjustment.

## 29. Concurrency

Các operation cạnh tranh trên stock cần concurrency control.

Có thể dùng:

- optimistic locking (`@Version`), hoặc
- pessimistic locking (`PESSIMISTIC_WRITE`), hoặc
- atomic conditional update.

Strategy cuối cùng phải thống nhất ở Service/Repository Layer.

## 30. Security

Mọi business request phải kiểm tra:

```text
Authentication
+
Tenant ownership
+
Warehouse scope
+
Role / Permission
```

Manager và Employee là Role của generic User, không phải entity riêng.

Permission model:

```text
Role baseline + User ALLOW/DENY override
```

Precedence:

```text
Explicit DENY > Explicit ALLOW > Role Permission > DENY
```

## 31. Logging

Không log:

- password.
- password hash.
- access token.
- refresh token.
- secret key.
- sensitive authentication data.

Không log toàn bộ Entity graph.

## 32. API serialization

Không nên serialize JPA Entity trực tiếp ra API response.

DTO layer nên tách khỏi Entity khi API layer được triển khai.

## 33. Migration

Schema change phải đi qua migration strategy của project.

Không dùng destructive schema auto-update ở production.

Mọi thay đổi PK/FK/column type/unique constraint phải đánh giá migration impact.

## 34. Testing bắt buộc

Các domain quan trọng phải có test cho:

- tenant isolation.
- cross-tenant rejection.
- unique constraint.
- negative inventory prevention.
- concurrent stock update.
- partial receiving.
- partial fulfillment.
- reservation.
- transfer in-transit.
- stock count variance.
- enum persistence.
- BigDecimal precision.
- historical data protection.

## 35. Không được tự ý thêm

Không tự tạo domain/entity chưa có requirement:

- WarehouseLocation.
- Zone.
- Rack.
- Shelf.
- Bin.
- Customer.
- CRM.
- SalesOrder.
- PurchaseOrder.
- Invoice.
- Payment.
- AccountingLedger.
- Carrier.
- Pricing Engine.
- ProductStock.
- ProductWarehouse.
- ProductQuantity.
- AvailableInventory aggregate table.

## 36. Review rule cho AI / Coding Agent

Mọi AI coding agent khi sửa WMS phải:

1. Đọc source code trước.
2. Đọc build configuration.
3. Xác định convention hiện tại.
4. Không rewrite project.
5. Không invent feature.
6. Không đổi technology stack.
7. Không tạo PostgreSQL syntax.
8. Không tự thay UUID strategy.
9. Không tự thay time strategy.
10. Không tạo EAGER relationship tùy tiện.
11. Không dùng `CascadeType.ALL` mù quáng.
12. Không dùng Lombok `@Data` mù quáng.
13. Không thêm quantity vào Product.
14. Không bỏ Tenant scope.
15. Không sửa historical ledger.
16. Compile sau khi sửa.
17. Chạy relevant tests.
18. Không tuyên bố thành công nếu compile/test fail.

## 37. Open technology decisions

Chưa khóa hoàn toàn:

- Java exact version nếu build hiện tại chưa thống nhất.
- Spring Boot/Hibernate major upgrade.
- UUID storage `CHAR(36)` hay `BINARY(16)`.
- `Instant` hay `LocalDateTime`.
- Optimistic vs pessimistic locking.
- Soft delete architecture tổng thể.
- Flyway vs Liquibase nếu project chưa chọn.
- Redis/cache.
- Message broker.
- Search engine.
- Object storage.
- Deployment/cloud provider.
- Observability stack.

Các thay đổi trên cần ADR hoặc quyết định kiến trúc riêng.

## 38. Non-negotiable decisions

Bắt buộc:

- MySQL.
- Java + Spring Boot.
- Spring Data JPA + Hibernate.
- SaaS multi-tenant.
- Tenant isolation.
- UUID identifiers.
- BigDecimal cho money/quantity.
- Enum STRING.
- MySQL JSON cho flexible attributes.
- Không MongoDB cho core Product/Inventory.
- Product != Inventory.
- ProductVariant/SKU là inventory transaction target.
- Không Warehouse Location ở baseline.
- Không negative inventory.
- InventoryMovement là ledger.
- Lot/Batch/Serial được hỗ trợ.
- Partial Inbound.
- Partial Outbound.
- Inventory Reservation.
- Transfer có IN_TRANSIT.
- Partial Transfer Receiving.
- Stock Count variance.
- AuditLog append-only.
- Không cascade-delete historical transaction.
- ProductUom dùng UUID PK.
- Không dùng PostgreSQL-only SQL.

## 39. Definition of Done

Một thay đổi kỹ thuật chỉ hoàn tất khi:

- Phù hợp các constraint trong file này.
- Không phá tenant isolation.
- Không phá inventory consistency.
- Không phá historical data.
- Compile thành công.
- Relevant tests pass.
- Migration impact đã được xử lý hoặc ghi rõ.
- Không có dependency/framework mới không cần thiết.
- Không thêm feature ngoài scope.
