package Retailtrack.retailtrack.repository;

import Retailtrack.retailtrack.entity.ProductSupplier;
import Retailtrack.retailtrack.entity.ProductSupplierId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductSupplierRepository extends JpaRepository<ProductSupplier, ProductSupplierId> {

    List<ProductSupplier> findByProductId(Integer productId);

    List<ProductSupplier> findBySupplierId(Integer supplierId);

    /** Returns the primary supplier for a given product, if one is set. */
    Optional<ProductSupplier> findByProductIdAndIsPrimaryTrue(Integer productId);
}
