package Retailtrack.retailtrack.dto.request;

import Retailtrack.retailtrack.entity.enums.DiscountType;
import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Request payload for creating or updating a coupon.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CouponRequestDTO {

    @NotBlank(message = "Coupon code is required")
    @Size(max = 50, message = "Coupon code must not exceed 50 characters")
    private String code;

    @NotNull(message = "Discount type is required")
    private DiscountType discountType;

    @NotNull(message = "Discount value is required")
    @DecimalMin(value = "0.0", inclusive = false, message = "Discount value must be greater than 0")
    private BigDecimal discountValue;

    @NotNull(message = "Minimum cart value is required")
    @DecimalMin(value = "0.0", message = "Minimum cart value cannot be negative")
    @Builder.Default
    private BigDecimal minCartValue = BigDecimal.ZERO;

    @NotNull(message = "Max uses is required")
    @Min(value = 1, message = "Max uses must be at least 1")
    @Builder.Default
    private Integer maxUses = 1;

    private LocalDate expiryDate;

    @Builder.Default
    private Boolean isActive = true;
}
