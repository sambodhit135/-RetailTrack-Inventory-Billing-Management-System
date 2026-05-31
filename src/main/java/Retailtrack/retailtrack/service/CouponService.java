package Retailtrack.retailtrack.service;

import Retailtrack.retailtrack.dto.request.CouponRequestDTO;
import Retailtrack.retailtrack.dto.response.CouponResponseDTO;
import Retailtrack.retailtrack.entity.Coupon;

import java.math.BigDecimal;
import java.util.List;

/**
 * Service contract for Coupon management operations.
 */
public interface CouponService {

    /**
     * Creates a new coupon.
     *
     * @param dto validated coupon request details DTO
     * @return the created coupon response DTO
     */
    CouponResponseDTO createCoupon(CouponRequestDTO dto);

    /**
     * Retrieves all active coupons (isActive = true and not expired).
     *
     * @return list of active coupon response DTOs
     */
    List<CouponResponseDTO> getActiveCoupons();

    /**
     * Validates if a coupon is eligible for a given cart value and returns the entity.
     * Throws InvalidCouponException if not found, inactive, expired, limit reached, or cart value below threshold.
     *
     * @param code      the coupon code (case-insensitive)
     * @param cartValue the current cart amount
     * @return the matching Coupon entity
     * @throws Retailtrack.retailtrack.exception.InvalidCouponException if coupon is invalid
     */
    Coupon validateAndFetchCoupon(String code, BigDecimal cartValue);
}
