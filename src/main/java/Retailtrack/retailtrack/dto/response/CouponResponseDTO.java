package Retailtrack.retailtrack.dto.response;

import Retailtrack.retailtrack.entity.enums.DiscountType;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * DTO representing coupon details returned to clients.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CouponResponseDTO {

    private Integer id;
    private String code;
    private DiscountType discountType;
    private BigDecimal discountValue;
    private BigDecimal minCartValue;
    private Integer maxUses;
    private Integer usedCount;
    private LocalDate expiryDate;
    private Boolean isActive;
    private LocalDateTime createdAt;
}
