package Retailtrack.retailtrack.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.*;

import java.io.Serializable;
import java.util.Objects;

/**
 * Composite primary key for the {@link ProductSupplier} join table.
 * JPA requires the composite key class to implement Serializable
 * and properly override equals() and hashCode().
 */
@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ProductSupplierId implements Serializable {

    @Column(name = "product_id")
    private Integer productId;

    @Column(name = "supplier_id")
    private Integer supplierId;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ProductSupplierId that)) return false;
        return Objects.equals(productId, that.productId)
                && Objects.equals(supplierId, that.supplierId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(productId, supplierId);
    }
}
