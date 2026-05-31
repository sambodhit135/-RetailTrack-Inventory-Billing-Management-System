package Retailtrack.retailtrack.dto.response;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO carrying details of a customer invoice.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InvoiceResponseDTO {

    private Integer invoiceId;
    private String invoiceNumber;
    private Integer orderId;
    private LocalDateTime generatedAt;
    private String customerName;
    private BigDecimal totalAmount;
    private BigDecimal discountAmount;
    private BigDecimal gstAmount;
    private BigDecimal grandTotal;
    private List<InvoiceItemDTO> items;

    /**
     * DTO representing an itemized row inside the invoice.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class InvoiceItemDTO {
        private Integer productId;
        private String productName;
        private Integer quantity;
        private BigDecimal unitPrice;
        private BigDecimal discountAmount;
        private BigDecimal gstRate;
        private BigDecimal gstAmount;
        private BigDecimal subtotal;
    }
}
