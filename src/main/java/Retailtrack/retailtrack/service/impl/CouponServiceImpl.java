package Retailtrack.retailtrack.service.impl;

import Retailtrack.retailtrack.dto.request.CouponRequestDTO;
import Retailtrack.retailtrack.dto.response.CouponResponseDTO;
import Retailtrack.retailtrack.entity.Coupon;
import Retailtrack.retailtrack.exception.InvalidCouponException;
import Retailtrack.retailtrack.repository.CouponRepository;
import Retailtrack.retailtrack.service.CouponService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Implementation of {@link CouponService} providing coupon operations and validation.
 */
@Slf4j
@Service
@Transactional
public class CouponServiceImpl implements CouponService {

    private final CouponRepository couponRepository;

    public CouponServiceImpl(CouponRepository couponRepository) {
        this.couponRepository = couponRepository;
    }

    @Override
    public CouponResponseDTO createCoupon(CouponRequestDTO dto) {
        log.info("Creating new coupon with code: '{}'", dto.getCode());

        if (couponRepository.existsByCodeIgnoreCase(dto.getCode())) {
            log.error("Coupon with code '{}' already exists.", dto.getCode());
            throw new IllegalArgumentException("Coupon code already exists: " + dto.getCode());
        }

        Coupon coupon = Coupon.builder()
                .code(dto.getCode().trim().toUpperCase())
                .discountType(dto.getDiscountType())
                .discountValue(dto.getDiscountValue())
                .minCartValue(dto.getMinCartValue() != null ? dto.getMinCartValue() : BigDecimal.ZERO)
                .maxUses(dto.getMaxUses() != null ? dto.getMaxUses() : 1)
                .expiryDate(dto.getExpiryDate())
                .isActive(dto.getIsActive() != null ? dto.getIsActive() : true)
                .build();

        Coupon saved = couponRepository.save(coupon);
        log.info("Coupon created successfully with ID: {}", saved.getId());

        return toResponseDTO(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CouponResponseDTO> getActiveCoupons() {
        log.info("Fetching all active coupons");
        List<Coupon> activeCoupons = couponRepository.findActiveCoupons(LocalDate.now());
        log.info("Fetched {} active coupons successfully", activeCoupons.size());
        return activeCoupons.stream()
                .map(this::toResponseDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public Coupon validateAndFetchCoupon(String code, BigDecimal cartValue) {
        log.info("Validating coupon code: '{}' for cart value: {}", code, cartValue);

        if (code == null || code.trim().isEmpty()) {
            throw new InvalidCouponException("Coupon code cannot be empty");
        }

        Coupon coupon = couponRepository.findByCodeIgnoreCase(code.trim())
                .orElseThrow(() -> {
                    log.error("Coupon code '{}' not found", code);
                    return new InvalidCouponException("Coupon code not found: " + code);
                });

        if (Boolean.FALSE.equals(coupon.getIsActive())) {
            log.error("Coupon '{}' is inactive", code);
            throw new InvalidCouponException("Coupon is inactive");
        }

        if (coupon.getExpiryDate() != null && coupon.getExpiryDate().isBefore(LocalDate.now())) {
            log.error("Coupon '{}' is expired. Expiry date: {}", code, coupon.getExpiryDate());
            throw new InvalidCouponException("Coupon has expired");
        }

        if (coupon.getUsedCount() >= coupon.getMaxUses()) {
            log.error("Coupon '{}' has reached max uses. Max: {}, Used: {}", code, coupon.getMaxUses(), coupon.getUsedCount());
            throw new InvalidCouponException("Coupon usage limit exceeded");
        }

        if (cartValue.compareTo(coupon.getMinCartValue()) < 0) {
            log.error("Cart value {} is below min cart value {} required for coupon '{}'", cartValue, coupon.getMinCartValue(), code);
            throw new InvalidCouponException("Minimum cart value of " + coupon.getMinCartValue() + " is required");
        }

        log.info("Coupon '{}' validated successfully", code);
        return coupon;
    }

    // ── Mapper ────────────────────────────────────────────────────────────────

    private CouponResponseDTO toResponseDTO(Coupon coupon) {
        return CouponResponseDTO.builder()
                .id(coupon.getId())
                .code(coupon.getCode())
                .discountType(coupon.getDiscountType())
                .discountValue(coupon.getDiscountValue())
                .minCartValue(coupon.getMinCartValue())
                .maxUses(coupon.getMaxUses())
                .usedCount(coupon.getUsedCount())
                .expiryDate(coupon.getExpiryDate())
                .isActive(coupon.getIsActive())
                .createdAt(coupon.getCreatedAt())
                .build();
    }
}
