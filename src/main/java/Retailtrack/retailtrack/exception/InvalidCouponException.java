package Retailtrack.retailtrack.exception;

/**
 * Thrown when a coupon code fails validation — it may be:
 * - not found / incorrect code
 * - expired (past expiry_date)
 * - fully used (used_count >= max_uses)
 * - inactive (is_active = false)
 * - not applicable (cart total below min_cart_value)
 *
 * Maps to HTTP 400 Bad Request in {@link GlobalExceptionHandler}.
 */
public class InvalidCouponException extends RuntimeException {

    public InvalidCouponException(String message) {
        super(message);
    }
}
