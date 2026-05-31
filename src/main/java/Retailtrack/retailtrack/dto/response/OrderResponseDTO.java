package Retailtrack.retailtrack.dto.response;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderResponseDTO {

    private Integer orderId;

    private String customerName;

    /** Sum of (unit_price × quantity) for all line items, before discounts and GST. */
    private BigDecimal totalAmount;

    /** Total discount applied via coupon or manual override. */
    private BigDecimal discountAmount;

    /** Total GST charged across all line items. */
    private BigDecimal gstAmount;

    /** Final payable amount: totalAmount − discountAmount + gstAmount. */
    private BigDecimal grandTotal;

    /** Order lifecycle status: PENDING, COMPLETED, or CANCELLED. */
    private String status;

    /** Invoice number if the order has been completed and an invoice generated. */
    private String invoiceNumber;

    /** Timestamp when the order was placed. */
    private LocalDateTime createdAt;

    /** Line items included in this order. */
    private List<OrderItemResponseDTO> items;

    // ── Nested line-item summary ──────────────────────────────────────────────
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OrderItemResponseDTO {

        private Integer productId;
        private String  productName;
        private Integer quantity;
        private BigDecimal unitPrice;
        private BigDecimal gstAmount;
        private BigDecimal subtotal;
    }
}
