package com.duyminh.wms.entity;

import com.duyminh.wms.entity.enums.InventoryReservationStatus;
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
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "inventory_reservations",
        indexes = {
                @Index(name = "idx_inventory_reservations_tenant_warehouse_variant", columnList = "tenant_id, warehouse_id, variant_id"),
                @Index(name = "idx_inventory_reservations_outbound_item", columnList = "outbound_item_id"),
                @Index(name = "idx_inventory_reservations_lot", columnList = "lot_id"),
                @Index(name = "idx_inventory_reservations_serial", columnList = "serial_id"),
                @Index(name = "idx_inventory_reservations_tenant_status", columnList = "tenant_id, status")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(onlyExplicitlyIncluded = true)
public class InventoryReservation {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @EqualsAndHashCode.Include
    @ToString.Include
    private UUID id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    // TODO: enforce warehouse.tenant == tenant in the Reservation service/domain layer.
    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "warehouse_id", nullable = false)
    private Warehouse warehouse;

    // TODO: enforce variant.tenant == tenant and tracking rules in the Reservation service/domain layer.
    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "variant_id", nullable = false)
    private ProductVariant variant;

    // TODO: enforce outboundItem.tenant == tenant and outboundItem.variant == variant in the Reservation service/domain layer.
    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "outbound_item_id", nullable = false)
    private OutboundOrderItem outboundItem;

    // TODO: enforce lot tenant, warehouse, and variant consistency in the Reservation service/domain layer.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lot_id")
    private InventoryLot lot;

    // TODO: enforce one ACTIVE reservation per serial in the Reservation service/domain layer or MySQL generated-column strategy.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "serial_id")
    private InventorySerial serial;

    @NotNull
    @Positive
    @Column(nullable = false, precision = 19, scale = 6)
    private BigDecimal quantity;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @ToString.Include
    private InventoryReservationStatus status = InventoryReservationStatus.ACTIVE;

    // TODO: enforce reservedBy.tenant == tenant in the Reservation service/domain layer.
    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reserved_by", nullable = false)
    private User reservedBy;

    @NotNull
    @Column(name = "reserved_at", nullable = false)
    private Instant reservedAt;

    @Column(name = "released_at")
    private Instant releasedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
