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
        name = "outbound_fulfillments",
        indexes = {
                @Index(name = "idx_outbound_fulfillments_tenant_item", columnList = "tenant_id, order_item_id"),
                @Index(name = "idx_outbound_fulfillments_fulfilled_at", columnList = "fulfilled_at")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(onlyExplicitlyIncluded = true)
public class OutboundFulfillment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @EqualsAndHashCode.Include
    @ToString.Include
    private UUID id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    // TODO: enforce orderItem.tenant == tenant in the Outbound service/domain layer.
    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_item_id", nullable = false)
    private OutboundOrderItem orderItem;

    @NotNull
    @Positive
    @Column(nullable = false, precision = 19, scale = 6)
    private BigDecimal quantity;

    // TODO: enforce unit.tenant == tenant and validate it against ProductUom in the Outbound service/domain layer.
    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "unit_id", nullable = false)
    private Unit unit;

    @NotNull
    @Positive
    @Column(name = "conversion_factor", nullable = false, precision = 19, scale = 6)
    private BigDecimal conversionFactor;

    @NotNull
    @Positive
    @Column(name = "base_quantity", nullable = false, precision = 19, scale = 6)
    private BigDecimal baseQuantity;

    // TODO: enforce fulfilledBy.tenant == tenant in the Outbound service/domain layer.
    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fulfilled_by", nullable = false)
    private User fulfilledBy;

    @NotNull
    @Column(name = "fulfilled_at", nullable = false)
    private Instant fulfilledAt;

    @Column(columnDefinition = "TEXT")
    private String note;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}
