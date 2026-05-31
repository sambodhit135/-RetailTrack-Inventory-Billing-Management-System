package Retailtrack.retailtrack.dto.response;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductResponseDTO {

    private Integer id;

    private String name;

    private String description;

    private BigDecimal price;

    private Integer stockQuantity;

    private Integer reorderThreshold;

    private BigDecimal avgDailySales;

    private Integer categoryId;

    private String categoryName;

    /**
     * Computed stock status:
     * "OUT_OF_STOCK" → stockQuantity == 0
     * "LOW_STOCK"    → 0 < stockQuantity <= reorderThreshold
     * "IN_STOCK"     → stockQuantity > reorderThreshold
     */
    private String status;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
