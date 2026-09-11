package com.duyminh.wms.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "stock_count_items",
        indexes = {
                @Index(name = "idx_stock_count_items_tenant_count", columnList = "tenant_id, stock_count_id"),
                @Index(name = "idx_stock_count_items_tenant_variant", columnList = "tenant_id, variant_id"),
                @Index(name = "idx_stock_count_items_inventory_lot", columnList = "inventory_lot_id"),
                @Index(name = "idx_stock_count_items_inventory_serial", columnList = "inventory_serial_id")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(onlyExplicitlyIncluded = true)
public class StockCountItem {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @EqualsAndHashCode.Include
    @ToString.Include
    private UUID id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    // TODO: enforce stockCount.tenant == tenant in the Stock Count service/domain layer.
    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "stock_count_id", nullable = false)
    private StockCount stockCount;

    // TODO: enforce variant.tenant == tenant in the Stock Count service/domain layer.
    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "variant_id", nullable = false)
    private ProductVariant variant;

    // TODO: enforce inventoryLot tenant, warehouse, and variant consistency in the Stock Count service/domain layer.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "inventory_lot_id")
    private InventoryLot inventoryLot;

    // TODO: enforce inventorySerial tenant, warehouse, variant, and 0/1 quantity rules in the Stock Count service/domain layer.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "inventory_serial_id")
    private InventorySerial inventorySerial;

    @NotNull
    @PositiveOrZero
    @Column(name = "system_quantity", nullable = false, precision = 19, scale = 6)
    private BigDecimal systemQuantity;

    @PositiveOrZero
    @Column(name = "counted_quantity", precision = 19, scale = 6)
    private BigDecimal countedQuantity;

    @Column(name = "variance_quantity", precision = 19, scale = 6)
    private BigDecimal varianceQuantity;

    // TODO: enforce countedBy.tenant == tenant in the Stock Count service/domain layer when present.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "counted_by")
    private User countedBy;

    @Column(name = "counted_at")
    private Instant countedAt;

    @Column(columnDefinition = "TEXT")
    private String note;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
