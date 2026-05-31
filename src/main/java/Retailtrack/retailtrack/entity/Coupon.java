package Retailtrack.retailtrack.entity;

import Retailtrack.retailtrack.entity.enums.DiscountType;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "coupons")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Coupon {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, unique = true, length = 50)
    private String code;

    /**
     * PERCENTAGE → discount is a percentage of cart total.
     * FLAT       → discount is a fixed rupee amount.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "discount_type", nullable = false, length = 15)
    private DiscountType discountType;

    @Column(name = "discount_value", nullable = false, precision = 10, scale = 2)
    private BigDecimal discountValue;

    /**
     * Minimum cart value required for this coupon to be applicable.
     */
    @Column(name = "min_cart_value", nullable = false, precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal minCartValue = BigDecimal.ZERO;

    /** Maximum number of times this coupon can be used across all customers. */
    @Column(name = "max_uses", nullable = false)
    @Builder.Default
    private Integer maxUses = 1;

    /** Running count of how many times this coupon has been redeemed. */
    @Column(name = "used_count", nullable = false)
    @Builder.Default
    private Integer usedCount = 0;

    @Column(name = "expiry_date")
    private LocalDate expiryDate;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // ── Lifecycle ─────────────────────────────────────────────────────────────
    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
