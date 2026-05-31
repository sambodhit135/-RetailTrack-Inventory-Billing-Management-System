package Retailtrack.retailtrack.repository;

import Retailtrack.retailtrack.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductRepository extends JpaRepository<Product, Integer> {

    List<Product> findByCategoryId(Integer categoryId);

    @Override
    @Query("SELECT p FROM Product p JOIN FETCH p.category")
    List<Product> findAll();

    /**
     * Finds all products whose current stock is at or below their reorder threshold.
     * Used by the Reorder Engine to generate reorder alerts.
     */
    @Query("SELECT p FROM Product p WHERE p.stockQuantity <= p.reorderThreshold")
    List<Product> findProductsBelowReorderThreshold();

    List<Product> findByNameContainingIgnoreCase(String name);
}
