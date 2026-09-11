package com.duyminh.wms.entity;

import com.duyminh.wms.entity.enums.InventoryLotStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
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
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(
        name = "inventory_lots",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_inventory_lot_tenant_warehouse_variant_number",
                columnNames = {"tenant_id", "warehouse_id", "variant_id", "lot_number"}
        ),
        indexes = {
                @Index(name = "idx_inventory_lots_tenant_variant", columnList = "tenant_id, variant_id"),
                @Index(name = "idx_inventory_lots_tenant_warehouse", columnList = "tenant_id, warehouse_id"),
                @Index(name = "idx_inventory_lots_expiry_date", columnList = "expiry_date")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(onlyExplicitlyIncluded = true)
public class InventoryLot {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @EqualsAndHashCode.Include
    @ToString.Include
    private UUID id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    // TODO: enforce warehouse.tenant == tenant in the Inventory service/domain layer.
    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "warehouse_id", nullable = false)
    private Warehouse warehouse;

    // TODO: enforce variant.tenant == tenant in the Inventory service/domain layer.
    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "variant_id", nullable = false)
    private ProductVariant variant;

    @NotBlank
    @Size(max = 100)
    @Column(name = "lot_number", nullable = false, length = 100)
    @ToString.Include
    private String lotNumber;

    @Column(name = "manufacture_date")
    private LocalDate manufactureDate;

    @Column(name = "expiry_date")
    private LocalDate expiryDate;

    @NotNull
    @PositiveOrZero
    @Column(name = "on_hand_quantity", nullable = false, precision = 19, scale = 6)
    private BigDecimal onHandQuantity = BigDecimal.ZERO;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @ToString.Include
    private InventoryLotStatus status = InventoryLotStatus.ACTIVE;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
