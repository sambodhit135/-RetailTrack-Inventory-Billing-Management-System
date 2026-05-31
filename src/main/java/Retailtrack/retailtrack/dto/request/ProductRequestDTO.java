package Retailtrack.retailtrack.dto.request;

import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductRequestDTO {

    @NotBlank(message = "Product name must not be blank")
    @Size(max = 200, message = "Product name must not exceed 200 characters")
    private String name;

    private String description;

    @NotNull(message = "Price is required")
    @DecimalMin(value = "0.0", inclusive = false, message = "Price must be greater than 0")
    @Digits(integer = 8, fraction = 2, message = "Price must have at most 8 integer and 2 decimal digits")
    private BigDecimal price;

    @NotNull(message = "Stock quantity is required")
    @Min(value = 0, message = "Stock quantity cannot be negative")
    private Integer stockQuantity;

    @NotNull(message = "Reorder threshold is required")
    @Min(value = 0, message = "Reorder threshold cannot be negative")
    private Integer reorderThreshold;

    /**
     * Estimated units sold per day.
     * Used by the Reorder Engine to project how long stock will last.
     */
    @NotNull(message = "Average daily sales is required")
    @DecimalMin(value = "0.0", message = "Average daily sales cannot be negative")
    private BigDecimal avgDailySales;

    @NotNull(message = "Category ID is required")
    private Integer categoryId;
}
