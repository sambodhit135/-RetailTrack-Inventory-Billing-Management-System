package Retailtrack.retailtrack.service;

import Retailtrack.retailtrack.dto.request.ProductRequestDTO;
import Retailtrack.retailtrack.dto.response.ProductResponseDTO;
import Retailtrack.retailtrack.dto.response.StockMovementResponseDTO;

import java.util.List;
import java.util.Map;

/**
 * Service contract for Product management operations.
 * Handles JPA standard operations and raw JDBC bulk updates / audit logging.
 */
public interface ProductService {

    /**
     * Creates a new product. Checks if the category exists.
     *
     * @param dto the product request details
     * @return the created product DTO
     */
    ProductResponseDTO createProduct(ProductRequestDTO dto);

    /**
     * Retrieves a product by its ID, computing its stock status.
     *
     * @param id the product ID
     * @return the product response DTO
     */
    ProductResponseDTO getProductById(Integer id);

    /**
     * Retrieves all products, computing stock status for each.
     *
     * @return list of all product response DTOs
     */
    List<ProductResponseDTO> getAllProducts();

    /**
     * Updates an existing product's core details.
     *
     * @param id  the product ID to update
     * @param dto the updated product details
     * @return the updated product response DTO
     */
    ProductResponseDTO updateProduct(Integer id, ProductRequestDTO dto);

    /**
     * Performs a high-performance bulk update on product stock and records audit entries.
     * Transactional: if any single update fails, all changes roll back.
     *
     * @param stockAdjustments list of adjustments containing keys: productId, quantityChange, remarks
     */
    void bulkUpdateStock(List<Map<String, Object>> stockAdjustments);

    /**
     * Retrieves the native JDBC audit history for a product.
     *
     * @param productId the product ID to retrieve audit log for
     * @return list of stock movement logs
     */
    List<StockMovementResponseDTO> getStockMovementAuditLog(Integer productId);
}
