package com.duyminh.wms.entity;

import com.duyminh.wms.entity.enums.StockTransferStatus;
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
        name = "stock_transfers",
        uniqueConstraints = @UniqueConstraint(name = "uq_stock_transfer_tenant_number", columnNames = {"tenant_id", "transfer_number"}),
        indexes = {
                @Index(name = "idx_stock_transfers_tenant_source", columnList = "tenant_id, source_warehouse_id"),
                @Index(name = "idx_stock_transfers_tenant_destination", columnList = "tenant_id, destination_warehouse_id"),
                @Index(name = "idx_stock_transfers_tenant_status", columnList = "tenant_id, status")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(onlyExplicitlyIncluded = true)
public class StockTransfer {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @EqualsAndHashCode.Include
    @ToString.Include
    private UUID id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    // TODO: enforce sourceWarehouse.tenant == tenant and source != destination in the Transfer service/domain layer.
    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_warehouse_id", nullable = false)
    private Warehouse sourceWarehouse;

    // TODO: enforce destinationWarehouse.tenant == tenant in the Transfer service/domain layer.
    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "destination_warehouse_id", nullable = false)
    private Warehouse destinationWarehouse;

    @NotBlank
    @Size(max = 50)
    @Column(name = "transfer_number", nullable = false, length = 50)
    @ToString.Include
    private String transferNumber;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @ToString.Include
    private StockTransferStatus status = StockTransferStatus.DRAFT;

    @Column(columnDefinition = "TEXT")
    private String note;

    // TODO: enforce createdBy.tenant == tenant in the Transfer service/domain layer.
    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", nullable = false)
    private User createdBy;

    @Column(name = "completed_at")
    private Instant completedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
