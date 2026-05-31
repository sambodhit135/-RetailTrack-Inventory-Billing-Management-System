package Retailtrack.retailtrack.dto.response;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CategoryResponseDTO {

    private Integer id;

    private String name;

    /**
     * GST percentage applicable to all products in this category.
     * e.g. 5.00, 12.00, 18.00
     */
    private BigDecimal gstSlab;

    private String description;

    private LocalDateTime createdAt;
}
