package Retailtrack.retailtrack.controller;

import Retailtrack.retailtrack.dto.request.ProductRequestDTO;
import Retailtrack.retailtrack.dto.response.ProductResponseDTO;
import Retailtrack.retailtrack.dto.response.StockMovementResponseDTO;
import Retailtrack.retailtrack.service.ProductService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST Controller for Product management.
 * Exposes CRUD endpoints under {@code /api/products} as well as bulk stock updates
 * and native JDBC audit history access.
 */
@Slf4j
@RestController
@RequestMapping("/api/products")
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    // ── POST /api/products ────────────────────────────────────────────────────

    /**
     * Creates a new product.
     *
     * @param request validated product request details DTO
     * @return {@code 201 Created} with the created product response DTO
     */
    @PostMapping
    public ResponseEntity<ProductResponseDTO> createProduct(
            @Valid @RequestBody ProductRequestDTO request) {
        log.info("REST: Received request to create new product: '{}'", request.getName());
        ProductResponseDTO created = productService.createProduct(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    // ── GET /api/products ─────────────────────────────────────────────────────

    /**
     * Retrieves all products with computed stock statuses.
     *
     * @return {@code 200 OK} with the list of all products
     */
    @GetMapping
    public ResponseEntity<List<ProductResponseDTO>> getAllProducts() {
        log.info("REST: Received request to retrieve all products");
        List<ProductResponseDTO> products = productService.getAllProducts();
        return ResponseEntity.ok(products);
    }

    // ── GET /api/products/{id} ────────────────────────────────────────────────

    /**
     * Retrieves a single product by its database identifier.
     *
     * @param id the product ID
     * @return {@code 200 OK} with the product response details, or {@code 404} if not found
     */
    @GetMapping("/{id}")
    public ResponseEntity<ProductResponseDTO> getProductById(@PathVariable Integer id) {
        log.info("REST: Received request to retrieve product with ID: {}", id);
        ProductResponseDTO product = productService.getProductById(id);
        return ResponseEntity.ok(product);
    }

    // ── PUT /api/products/{id} ────────────────────────────────────────────────

    /**
     * Updates an existing product's details.
     *
     * @param id      the product ID
     * @param request validated product request details DTO
     * @return {@code 200 OK} with the updated product response DTO
     */
    @PutMapping("/{id}")
    public ResponseEntity<ProductResponseDTO> updateProduct(
            @PathVariable Integer id,
            @Valid @RequestBody ProductRequestDTO request) {
        log.info("REST: Received request to update product with ID: {}", id);
        ProductResponseDTO updated = productService.updateProduct(id, request);
        return ResponseEntity.ok(updated);
    }

    // ── POST /api/products/bulk-stock-update ──────────────────────────────────

    /**
     * Updates stock in batch using high-performance raw SQL operations.
     * Rollback is guaranteed across the entire batch if any operation fails.
     *
     * @param stockAdjustments list of stock changes (containing productId, quantityChange, remarks)
     * @return {@code 200 OK} on successful batch execution
     */
    @PostMapping("/bulk-stock-update")
    public ResponseEntity<Void> bulkUpdateStock(
            @RequestBody List<Map<String, Object>> stockAdjustments) {
        log.info("REST: Received request for bulk stock update of {} items", 
                stockAdjustments != null ? stockAdjustments.size() : 0);
        productService.bulkUpdateStock(stockAdjustments);
        return ResponseEntity.ok().build();
    }

    // ── GET /api/products/{id}/audit-log ──────────────────────────────────────

    /**
     * Retrieves the custom native query join audit logs for stock movements.
     *
     * @param id the product ID
     * @return {@code 200 OK} with the list of stock audit logs
     */
    @GetMapping("/{id}/audit-log")
    public ResponseEntity<List<StockMovementResponseDTO>> getStockMovementAuditLog(
            @PathVariable Integer id) {
        log.info("REST: Received request for stock movement audit log of product ID: {}", id);
        List<StockMovementResponseDTO> auditLog = productService.getStockMovementAuditLog(id);
        return ResponseEntity.ok(auditLog);
    }
}
