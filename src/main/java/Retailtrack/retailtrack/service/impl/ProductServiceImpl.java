package Retailtrack.retailtrack.service.impl;

import Retailtrack.retailtrack.dto.request.ProductRequestDTO;
import Retailtrack.retailtrack.dto.response.ProductResponseDTO;
import Retailtrack.retailtrack.dto.response.StockMovementResponseDTO;
import Retailtrack.retailtrack.entity.Category;
import Retailtrack.retailtrack.entity.Product;
import Retailtrack.retailtrack.exception.InsufficientStockException;
import Retailtrack.retailtrack.exception.ResourceNotFoundException;
import Retailtrack.retailtrack.repository.CategoryRepository;
import Retailtrack.retailtrack.repository.ProductRepository;
import Retailtrack.retailtrack.service.ProductService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Implementation of {@link ProductService} blending Spring Data JPA and raw Spring {@link JdbcTemplate}
 * to balance standard CRUD operations with high-performance inventory updates and native queries.
 */
@Slf4j
@Service
@Transactional
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final JdbcTemplate jdbcTemplate;

    public ProductServiceImpl(ProductRepository productRepository,
                              CategoryRepository categoryRepository,
                              JdbcTemplate jdbcTemplate) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
        this.jdbcTemplate = jdbcTemplate;
    }

    // ── Create ────────────────────────────────────────────────────────────────

    @Override
    public ProductResponseDTO createProduct(ProductRequestDTO dto) {
        log.info("JPA: Creating new product with name: '{}', category ID: {}", dto.getName(), dto.getCategoryId());

        Category category = categoryRepository.findById(dto.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category", "id", dto.getCategoryId()));

        Product product = Product.builder()
                .name(dto.getName())
                .description(dto.getDescription())
                .price(dto.getPrice())
                .stockQuantity(dto.getStockQuantity())
                .reorderThreshold(dto.getReorderThreshold())
                .avgDailySales(dto.getAvgDailySales())
                .category(category)
                .build();

        Product saved = productRepository.save(product);
        log.info("JPA: Product created successfully with id: {}", saved.getId());

        return toResponseDTO(saved);
    }

    // ── Read ──────────────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public ProductResponseDTO getProductById(Integer id) {
        log.info("JPA: Fetching product with id: {}", id);

        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "id", id));

        log.info("JPA: Product with id: {} fetched successfully", id);
        return toResponseDTO(product);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductResponseDTO> getAllProducts() {
        log.info("JPA: Fetching all products");

        List<ProductResponseDTO> products = productRepository.findAll()
                .stream()
                .map(this::toResponseDTO)
                .collect(Collectors.toList());

        log.info("JPA: Fetches completed. Total products retrieved: {}", products.size());
        return products;
    }

    // ── Update ────────────────────────────────────────────────────────────────

    @Override
    public ProductResponseDTO updateProduct(Integer id, ProductRequestDTO dto) {
        log.info("JPA: Updating product with id: {}", id);

        Product existing = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "id", id));

        Category category = categoryRepository.findById(dto.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category", "id", dto.getCategoryId()));

        existing.setName(dto.getName());
        existing.setDescription(dto.getDescription());
        existing.setPrice(dto.getPrice());
        existing.setStockQuantity(dto.getStockQuantity());
        existing.setReorderThreshold(dto.getReorderThreshold());
        existing.setAvgDailySales(dto.getAvgDailySales());
        existing.setCategory(category);

        Product updated = productRepository.save(existing);
        log.info("JPA: Product with id: {} updated successfully", id);

        return toResponseDTO(updated);
    }

    // ── Bulk & Native Operations (JDBC Template) ──────────────────────────────

    @Override
    public void bulkUpdateStock(List<Map<String, Object>> stockAdjustments) {
        if (stockAdjustments == null || stockAdjustments.isEmpty()) {
            log.info("JDBC: No stock adjustments provided for bulk update. Skipping.");
            return;
        }

        log.info("JDBC: Starting bulk stock update for {} records.", stockAdjustments.size());

        // Extract all product IDs to load their current stock
        List<Integer> productIds = stockAdjustments.stream()
                .map(adj -> {
                    Object pIdVal = adj.get("productId");
                    if (pIdVal == null) {
                        throw new IllegalArgumentException("Field 'productId' is required for stock adjustments");
                    }
                    return ((Number) pIdVal).intValue();
                })
                .distinct()
                .collect(Collectors.toList());

        // Query current stocks and names using native query
        String inSql = String.join(",", Collections.nCopies(productIds.size(), "?"));
        String selectSql = "SELECT id, name, stock_quantity FROM products WHERE id IN (" + inSql + ")";
        
        Map<Integer, ProductStockInfo> productMap = new HashMap<>();
        
        log.info("JDBC: Querying current stock levels using native query: {}", selectSql);
        jdbcTemplate.query(selectSql, 
            ps -> {
                for (int i = 0; i < productIds.size(); i++) {
                    ps.setInt(i + 1, productIds.get(i));
                }
            }, 
            rs -> {
                while (rs.next()) {
                    int id = rs.getInt("id");
                    String name = rs.getString("name");
                    int stock = rs.getInt("stock_quantity");
                    productMap.put(id, new ProductStockInfo(id, name, stock));
                }
                return null;
            }
        );

        List<Object[]> productUpdateArgs = new ArrayList<>();
        List<Object[]> auditInsertArgs = new ArrayList<>();

        for (Map<String, Object> adj : stockAdjustments) {
            Object rawProductId = adj.get("productId");
            Object rawQtyChange = adj.get("quantityChange");
            String remarks = (String) adj.get("remarks");

            if (rawProductId == null || rawQtyChange == null) {
                throw new IllegalArgumentException("Fields 'productId' and 'quantityChange' are mandatory for each stock adjustment");
            }

            Integer productId = ((Number) rawProductId).intValue();
            Integer quantityChange = ((Number) rawQtyChange).intValue();

            ProductStockInfo info = productMap.get(productId);
            if (info == null) {
                log.error("JDBC: Product ID {} not found in database.", productId);
                throw new ResourceNotFoundException("Product", "id", productId);
            }

            int stockBefore = info.currentStock;
            int stockAfter = stockBefore + quantityChange;

            if (stockAfter < 0) {
                log.error("JDBC: Cannot adjust stock for '{}' (ID: {}). Current: {}, Adjustment: {}. Resulting stock would be negative.",
                        info.name, productId, stockBefore, quantityChange);
                throw new InsufficientStockException(info.name, Math.abs(quantityChange), stockBefore);
            }

            // Update in-memory state to handle duplicate product IDs correctly
            info.currentStock = stockAfter;

            // Prepare products update args
            productUpdateArgs.add(new Object[]{quantityChange, productId});

            // Prepare stock audit insert args
            String eventType = (quantityChange >= 0) ? "RESTOCK" : "ADJUSTMENT";
            auditInsertArgs.add(new Object[]{productId, eventType, quantityChange, stockBefore, stockAfter, remarks});
        }

        // Execute batch updates on products table
        String updateProductSql = "UPDATE products SET stock_quantity = stock_quantity + ?, updated_at = NOW() WHERE id = ?";
        log.info("JDBC: Executing batch update on products: {}", updateProductSql);
        jdbcTemplate.batchUpdate(updateProductSql, productUpdateArgs);

        // Execute batch inserts on stock_audit table
        String insertAuditSql = "INSERT INTO stock_audit (product_id, event_type, quantity_change, stock_before, stock_after, remarks, created_at) " +
                "VALUES (?, ?, ?, ?, ?, ?, NOW())";
        log.info("JDBC: Executing batch insert into stock_audit: {}", insertAuditSql);
        jdbcTemplate.batchUpdate(insertAuditSql, auditInsertArgs);

        log.info("JDBC: Bulk stock update completed successfully.");
    }

    @Override
    @Transactional(readOnly = true)
    public List<StockMovementResponseDTO> getStockMovementAuditLog(Integer productId) {
        log.info("JDBC: Fetching stock movement audit log for product ID: {}", productId);

        // Ensure the product exists
        if (!productRepository.existsById(productId)) {
            log.error("JDBC: Product ID {} not found. Cannot retrieve audit log.", productId);
            throw new ResourceNotFoundException("Product", "id", productId);
        }

        String sql = "SELECT sa.id, sa.product_id, p.name AS product_name, sa.event_type, sa.quantity_change, sa.stock_before, sa.stock_after, sa.remarks, sa.created_at " +
                "FROM stock_audit sa " +
                "JOIN products p ON sa.product_id = p.id " +
                "WHERE sa.product_id = ? " +
                "ORDER BY sa.created_at DESC";

        log.info("JDBC: Running custom native join query: {}", sql);
        List<StockMovementResponseDTO> auditLogs = jdbcTemplate.query(sql, (rs, rowNum) -> {
            return StockMovementResponseDTO.builder()
                    .id(rs.getInt("id"))
                    .productId(rs.getInt("product_id"))
                    .productName(rs.getString("product_name"))
                    .eventType(rs.getString("event_type"))
                    .quantityChange(rs.getInt("quantity_change"))
                    .stockBefore(rs.getInt("stock_before"))
                    .stockAfter(rs.getInt("stock_after"))
                    .remarks(rs.getString("remarks"))
                    .createdAt(rs.getTimestamp("created_at").toLocalDateTime())
                    .build();
        }, productId);

        log.info("JDBC: Successfully fetched {} audit log entries for product ID: {}", auditLogs.size(), productId);
        return auditLogs;
    }

    // ── Mappers & Helpers ─────────────────────────────────────────────────────

    private ProductResponseDTO toResponseDTO(Product product) {
        String status = computeStatus(product.getStockQuantity(), product.getReorderThreshold());
        return ProductResponseDTO.builder()
                .id(product.getId())
                .name(product.getName())
                .description(product.getDescription())
                .price(product.getPrice())
                .stockQuantity(product.getStockQuantity())
                .reorderThreshold(product.getReorderThreshold())
                .avgDailySales(product.getAvgDailySales())
                .categoryId(product.getCategory() != null ? product.getCategory().getId() : null)
                .categoryName(product.getCategory() != null ? product.getCategory().getName() : null)
                .status(status)
                .createdAt(product.getCreatedAt())
                .updatedAt(product.getUpdatedAt())
                .build();
    }

    private String computeStatus(Integer stock, Integer threshold) {
        if (stock == null || stock == 0) {
            return "OUT_OF_STOCK";
        } else if (stock <= threshold) {
            return "LOW_STOCK";
        } else {
            return "IN_STOCK";
        }
    }

    /**
     * In-memory cache helper to hold stock quantity details during bulk operations.
     */
    private static class ProductStockInfo {
        final int id;
        final String name;
        int currentStock;

        ProductStockInfo(int id, String name, int currentStock) {
            this.id = id;
            this.name = name;
            this.currentStock = currentStock;
        }
    }
}
