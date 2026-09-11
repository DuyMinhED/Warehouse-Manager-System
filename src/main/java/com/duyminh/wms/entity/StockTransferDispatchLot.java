package com.duyminh.wms.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "stock_transfer_dispatch_lots",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_stock_transfer_dispatch_lot_dispatch_lot",
                columnNames = {"dispatch_id", "inventory_lot_id"}
        ),
        indexes = @Index(name = "idx_stock_transfer_dispatch_lots_inventory_lot", columnList = "inventory_lot_id")
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(onlyExplicitlyIncluded = true)
public class StockTransferDispatchLot {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @EqualsAndHashCode.Include
    @ToString.Include
    private UUID id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    // TODO: enforce dispatch.tenant == tenant in the Transfer service/domain layer.
    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dispatch_id", nullable = false)
    private StockTransferDispatch dispatch;

    // TODO: enforce inventoryLot tenant, source warehouse, and variant consistency in the Transfer service/domain layer.
    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "inventory_lot_id", nullable = false)
    private InventoryLot inventoryLot;

    @NotNull
    @Positive
    @Column(nullable = false, precision = 19, scale = 6)
    private BigDecimal quantity;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}
