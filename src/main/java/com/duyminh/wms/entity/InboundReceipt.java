package com.duyminh.wms.entity;

import com.duyminh.wms.entity.enums.InboundReceiptStatus;
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
        name = "inbound_receipts",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_inbound_receipt_tenant_number",
                columnNames = {"tenant_id", "receipt_number"}
        ),
        indexes = {
                @Index(name = "idx_inbound_receipts_tenant_warehouse", columnList = "tenant_id, warehouse_id"),
                @Index(name = "idx_inbound_receipts_tenant_supplier", columnList = "tenant_id, supplier_id"),
                @Index(name = "idx_inbound_receipts_tenant_status", columnList = "tenant_id, status")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(onlyExplicitlyIncluded = true)
public class InboundReceipt {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @EqualsAndHashCode.Include
    @ToString.Include
    private UUID id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    // TODO: enforce warehouse.tenant == tenant in the Inbound service/domain layer.
    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "warehouse_id", nullable = false)
    private Warehouse warehouse;

    // TODO: enforce supplier.tenant == tenant in the Inbound service/domain layer when supplier is present.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "supplier_id")
    private Supplier supplier;

    @NotBlank
    @Size(max = 50)
    @Column(name = "receipt_number", nullable = false, length = 50)
    @ToString.Include
    private String receiptNumber;

    @Size(max = 100)
    @Column(name = "external_reference", length = 100)
    private String externalReference;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @ToString.Include
    private InboundReceiptStatus status = InboundReceiptStatus.DRAFT;

    @Column(name = "expected_at")
    private Instant expectedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(columnDefinition = "TEXT")
    private String note;

    // TODO: enforce createdBy.tenant == tenant in the Inbound service/domain layer.
    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", nullable = false)
    private User createdBy;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
