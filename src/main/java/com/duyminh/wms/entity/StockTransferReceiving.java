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
        name = "stock_transfer_receivings",
        indexes = {
                @Index(name = "idx_stock_transfer_receivings_tenant_dispatch", columnList = "tenant_id, dispatch_id"),
                @Index(name = "idx_stock_transfer_receivings_received_at", columnList = "received_at")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(onlyExplicitlyIncluded = true)
public class StockTransferReceiving {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @EqualsAndHashCode.Include
    @ToString.Include
    private UUID id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    // TODO: enforce dispatch.tenant == tenant and cumulative received <= dispatch.baseQuantity in the Transfer service/domain layer.
    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dispatch_id", nullable = false)
    private StockTransferDispatch dispatch;

    @NotNull
    @Positive
    @Column(name = "received_quantity", nullable = false, precision = 19, scale = 6)
    private BigDecimal receivedQuantity;

    // TODO: enforce receivedBy.tenant == tenant in the Transfer service/domain layer.
    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "received_by", nullable = false)
    private User receivedBy;

    @NotNull
    @Column(name = "received_at", nullable = false)
    private Instant receivedAt;

    @Column(columnDefinition = "TEXT")
    private String note;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}
