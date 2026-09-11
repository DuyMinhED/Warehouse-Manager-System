package com.duyminh.wms.entity;

import com.duyminh.wms.entity.enums.SupplierStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
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
        name = "suppliers",
        uniqueConstraints = @UniqueConstraint(name = "uq_supplier_tenant_code", columnNames = {"tenant_id", "code"}),
        indexes = {
                @Index(name = "idx_suppliers_tenant_name", columnList = "tenant_id, name"),
                @Index(name = "idx_suppliers_tenant_status", columnList = "tenant_id, status")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(onlyExplicitlyIncluded = true)
public class Supplier {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @EqualsAndHashCode.Include
    @ToString.Include
    private UUID id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @NotBlank
    @Size(max = 50)
    @Column(nullable = false, length = 50)
    @ToString.Include
    private String code;

    @NotBlank
    @Size(max = 200)
    @Column(nullable = false, length = 200)
    @ToString.Include
    private String name;

    @Size(max = 150)
    @Column(name = "contact_name", length = 150)
    private String contactName;

    @Email(message = "The input data must be in email format")
    @Size(max = 255)
    @Column(length = 255)
    private String email;

    @Size(max = 30)
    @Column(length = 30)
    private String phone;

    @Size(max = 255)
    @Column(length = 255)
    private String address;

    @Size(max = 50)
    @Column(name = "tax_code", length = 50)
    private String taxCode;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @ToString.Include
    private SupplierStatus status = SupplierStatus.ACTIVE;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
