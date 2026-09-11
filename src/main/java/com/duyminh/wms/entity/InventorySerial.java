package com.duyminh.wms.entity;

import com.duyminh.wms.entity.enums.InventorySerialStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "inventory_serials",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_inventory_serial_tenant_serial_number",
                columnNames = {"tenant_id", "serial_number"}
        ),
        indexes = {
                @Index(name = "idx_inventory_serials_tenant_variant", columnList = "tenant_id, variant_id"),
                @Index(name = "idx_inventory_serials_tenant_warehouse", columnList = "tenant_id, warehouse_id"),
                @Index(name = "idx_inventory_serials_lot", columnList = "lot_id"),
                @Index(name = "idx_inventory_serials_status", columnList = "status")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(onlyExplicitlyIncluded = true)
public class InventorySerial {

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

    // TODO: enforce lot tenant, warehouse, and variant consistency in the Inventory service/domain layer.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lot_id")
    private InventoryLot lot;

    @NotBlank
    @Size(max = 150)
    @Column(name = "serial_number", nullable = false, length = 150)
    @ToString.Include
    private String serialNumber;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @ToString.Include
    private InventorySerialStatus status = InventorySerialStatus.AVAILABLE;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
