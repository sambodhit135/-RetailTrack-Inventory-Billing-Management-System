package Retailtrack.retailtrack.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * Many-to-many join table between {@link Product} and {@link Supplier}.
 * Uses an explicit entity instead of @ManyToMany so we can store the
 * extra {@code is_primary} column directly on the relationship.
 */
@Entity
@Table(name = "product_suppliers")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductSupplier {

    @EmbeddedId
    private ProductSupplierId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("productId")
    @JoinColumn(name = "product_id")
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("supplierId")
    @JoinColumn(name = "supplier_id")
    private Supplier supplier;

    /**
     * True if this is the default/preferred supplier for this product.
     * Only one supplier per product should have this flag set to true.
     */
    @Column(name = "is_primary", nullable = false)
    @Builder.Default
    private Boolean isPrimary = false;
}
