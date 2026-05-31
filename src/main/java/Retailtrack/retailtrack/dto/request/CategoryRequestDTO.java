package Retailtrack.retailtrack.dto.request;

import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CategoryRequestDTO {

    @NotBlank(message = "Category name must not be blank")
    @Size(max = 100, message = "Category name must not exceed 100 characters")
    private String name;

    /**
     * GST percentage applicable to all products in this category.
     * e.g. 5.00, 12.00, 18.00
     */
    @NotNull(message = "GST slab is required")
    @DecimalMin(value = "0.0", inclusive = true, message = "GST slab cannot be negative")
    @DecimalMax(value = "100.00", message = "GST slab cannot exceed 100%")
    @Digits(integer = 3, fraction = 2, message = "GST slab must have at most 3 integer and 2 decimal digits")
    private BigDecimal gstSlab;

    private String description;
}
