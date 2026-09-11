package com.duyminh.wms.entity;

import com.duyminh.wms.entity.enums.InventoryTrackingType;
import com.duyminh.wms.entity.enums.ProductVariantStatus;
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
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(
        name = "product_variants",
        uniqueConstraints = @UniqueConstraint(name = "uq_product_variant_tenant_sku", columnNames = {"tenant_id", "sku"}),
        indexes = {
                @Index(name = "idx_product_variants_tenant", columnList = "tenant_id"),
                @Index(name = "idx_product_variants_product", columnList = "product_id"),
                @Index(name = "idx_product_variants_tenant_status", columnList = "tenant_id, status")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(onlyExplicitlyIncluded = true)
public class ProductVariant {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @EqualsAndHashCode.Include
    @ToString.Include
    private UUID id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    // TODO: enforce product.tenant == tenant in the ProductVariant service/domain layer.
    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @NotBlank
    @Size(max = 100)
    @Column(nullable = false, length = 100)
    @ToString.Include
    private String sku;

    @Size(max = 200)
    @Column(length = 200)
    private String name;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "json")
    private Map<String, Object> attributes;

    @PositiveOrZero
    @Column(name = "cost_price", precision = 19, scale = 4)
    private BigDecimal costPrice;

    @PositiveOrZero
    @Column(name = "selling_price", precision = 19, scale = 4)
    private BigDecimal sellingPrice;

    @PositiveOrZero
    @Column(precision = 19, scale = 6)
    private BigDecimal weight;

    @PositiveOrZero
    @Column(precision = 19, scale = 6)
    private BigDecimal length;

    @PositiveOrZero
    @Column(precision = 19, scale = 6)
    private BigDecimal width;

    @PositiveOrZero
    @Column(precision = 19, scale = 6)
    private BigDecimal height;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "tracking_type", nullable = false, length = 20)
    @ToString.Include
    private InventoryTrackingType trackingType = InventoryTrackingType.NONE;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @ToString.Include
    private ProductVariantStatus status = ProductVariantStatus.ACTIVE;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
