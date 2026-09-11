package com.duyminh.wms.entity;

import com.duyminh.wms.entity.enums.OutboundOrderStatus;
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
        name = "outbound_orders",
        uniqueConstraints = @UniqueConstraint(name = "uq_outbound_order_tenant_number", columnNames = {"tenant_id", "order_number"}),
        indexes = {
                @Index(name = "idx_outbound_orders_tenant_warehouse", columnList = "tenant_id, warehouse_id"),
                @Index(name = "idx_outbound_orders_tenant_status", columnList = "tenant_id, status")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(onlyExplicitlyIncluded = true)
public class OutboundOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @EqualsAndHashCode.Include
    @ToString.Include
    private UUID id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    // TODO: enforce warehouse.tenant == tenant in the Outbound service/domain layer.
    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "warehouse_id", nullable = false)
    private Warehouse warehouse;

    @NotBlank
    @Size(max = 50)
    @Column(name = "order_number", nullable = false, length = 50)
    @ToString.Include
    private String orderNumber;

    @Size(max = 100)
    @Column(name = "external_reference", length = 100)
    private String externalReference;

    @Size(max = 200)
    @Column(name = "recipient_name", length = 200)
    private String recipientName;

    @Size(max = 30)
    @Column(name = "recipient_phone", length = 30)
    private String recipientPhone;

    @Size(max = 255)
    @Column(name = "recipient_address", length = 255)
    private String recipientAddress;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @ToString.Include
    private OutboundOrderStatus status = OutboundOrderStatus.DRAFT;

    @Column(name = "expected_ship_at")
    private Instant expectedShipAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(columnDefinition = "TEXT")
    private String note;

    // TODO: enforce createdBy.tenant == tenant in the Outbound service/domain layer.
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
