package Retailtrack.retailtrack.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.*;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderRequestDTO {

    /** Customer name or identifier shown on the invoice. */
    @NotBlank(message = "Customer name is required")
    @Size(max = 150, message = "Customer name must not exceed 150 characters")
    private String customerName;

    /**
     * Optional coupon code to apply a discount to this order.
     * Leave null or blank if no coupon is being used.
     */
    private String couponCode;

    @NotEmpty(message = "Order must contain at least one item")
    @Valid
    private List<OrderItemRequestDTO> items;
}
