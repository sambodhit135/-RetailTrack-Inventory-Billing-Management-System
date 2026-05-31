package Retailtrack.retailtrack.dto.request;

import jakarta.validation.constraints.*;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderItemRequestDTO {

    @NotNull(message = "Product ID is required for each order item")
    private Integer productId;

    @NotNull(message = "Quantity is required for each order item")
    @Min(value = 1, message = "Quantity must be at least 1")
    private Integer quantity;
}
