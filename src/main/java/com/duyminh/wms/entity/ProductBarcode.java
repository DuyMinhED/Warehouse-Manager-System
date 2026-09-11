package com.duyminh.wms.entity;

import com.duyminh.wms.entity.enums.BarcodeType;
import com.duyminh.wms.entity.enums.ProductBarcodeStatus;
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
        name = "product_barcodes",
        uniqueConstraints = @UniqueConstraint(name = "uq_product_barcode_tenant_barcode", columnNames = {"tenant_id", "barcode"}),
        indexes = {
                @Index(name = "idx_product_barcodes_variant", columnList = "variant_id"),
                @Index(name = "idx_product_barcodes_tenant_status", columnList = "tenant_id, status")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(onlyExplicitlyIncluded = true)
public class ProductBarcode {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @EqualsAndHashCode.Include
    @ToString.Include
    private UUID id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    // TODO: enforce variant.tenant == tenant in the ProductBarcode service/domain layer.
    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "variant_id", nullable = false)
    private ProductVariant variant;

    @NotBlank
    @Size(max = 100)
    @Column(nullable = false, length = 100)
    @ToString.Include
    private String barcode;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "barcode_type", nullable = false, length = 30)
    @ToString.Include
    private BarcodeType barcodeType;

    @Column(name = "is_primary", nullable = false)
    private boolean primary;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @ToString.Include
    private ProductBarcodeStatus status = ProductBarcodeStatus.ACTIVE;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
