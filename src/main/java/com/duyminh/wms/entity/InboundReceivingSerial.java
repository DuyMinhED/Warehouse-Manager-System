package com.duyminh.wms.entity;

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

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "inbound_receiving_serials",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_inbound_receiving_serial_tenant_number",
                columnNames = {"tenant_id", "serial_number"}
        ),
        indexes = {
                @Index(name = "idx_inbound_receiving_serials_receiving", columnList = "receiving_id"),
                @Index(name = "idx_inbound_receiving_serials_lot", columnList = "receiving_lot_id")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(onlyExplicitlyIncluded = true)
public class InboundReceivingSerial {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @EqualsAndHashCode.Include
    @ToString.Include
    private UUID id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    // TODO: enforce receiving.tenant == tenant in the Inbound service/domain layer.
    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "receiving_id", nullable = false)
    private InboundReceiving receiving;

    // TODO: enforce receivingLot.receiving == receiving and receivingLot.tenant == tenant in the Inbound service/domain layer.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "receiving_lot_id")
    private InboundReceivingLot receivingLot;

    @NotBlank
    @Size(max = 150)
    @Column(name = "serial_number", nullable = false, length = 150)
    @ToString.Include
    private String serialNumber;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}
