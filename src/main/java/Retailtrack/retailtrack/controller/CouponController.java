package Retailtrack.retailtrack.controller;

import Retailtrack.retailtrack.dto.request.CouponRequestDTO;
import Retailtrack.retailtrack.dto.response.CouponResponseDTO;
import Retailtrack.retailtrack.service.CouponService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST Controller for Coupon management.
 * Exposes endpoints under {@code /api/coupons}.
 */
@Slf4j
@RestController
@RequestMapping("/api/coupons")
public class CouponController {

    private final CouponService couponService;

    public CouponController(CouponService couponService) {
        this.couponService = couponService;
    }

    // ── POST /api/coupons ─────────────────────────────────────────────────────

    /**
     * Creates a new coupon.
     *
     * @param request validated coupon request DTO payload
     * @return {@code 201 Created} with the persisted coupon response DTO
     */
    @PostMapping
    public ResponseEntity<CouponResponseDTO> createCoupon(
            @Valid @RequestBody CouponRequestDTO request) {
        log.info("REST: Received request to create new coupon code: '{}'", request.getCode());
        CouponResponseDTO created = couponService.createCoupon(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    // ── GET /api/coupons ──────────────────────────────────────────────────────

    /**
     * Retrieves all active coupons (isActive = true and not expired).
     *
     * @return {@code 200 OK} with the list of active coupons
     */
    @GetMapping
    public ResponseEntity<List<CouponResponseDTO>> getActiveCoupons() {
        log.info("REST: Received request to fetch all active coupons");
        List<CouponResponseDTO> activeCoupons = couponService.getActiveCoupons();
        return ResponseEntity.ok(activeCoupons);
    }
}
