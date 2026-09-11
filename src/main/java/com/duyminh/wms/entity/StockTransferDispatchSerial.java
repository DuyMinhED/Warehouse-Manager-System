package com.duyminh.wms.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "stock_transfer_dispatch_serials",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_stock_transfer_dispatch_serial_dispatch_serial",
                columnNames = {"dispatch_id", "inventory_serial_id"}
        ),
        indexes = @Index(name = "idx_stock_transfer_dispatch_serials_inventory_serial", columnList = "inventory_serial_id")
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(onlyExplicitlyIncluded = true)
public class StockTransferDispatchSerial {

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

    // TODO: enforce inventorySerial tenant, source warehouse, and variant consistency in the Transfer service/domain layer.
    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "inventory_serial_id", nullable = false)
    private InventorySerial inventorySerial;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}
