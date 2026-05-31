package Retailtrack.retailtrack.service.impl;

import Retailtrack.retailtrack.dto.response.InvoiceResponseDTO;
import Retailtrack.retailtrack.entity.Invoice;
import Retailtrack.retailtrack.entity.Order;
import Retailtrack.retailtrack.entity.OrderItem;
import Retailtrack.retailtrack.entity.enums.OrderStatus;
import Retailtrack.retailtrack.exception.ResourceNotFoundException;
import Retailtrack.retailtrack.repository.InvoiceRepository;
import Retailtrack.retailtrack.repository.OrderRepository;
import Retailtrack.retailtrack.service.InvoiceService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Implementation of {@link InvoiceService} managing invoice generation,
 * order completion transitions, and browser-printable HTML page rendering.
 */
@Slf4j
@Service
@Transactional
public class InvoiceServiceImpl implements InvoiceService {

    private final InvoiceRepository invoiceRepository;
    private final OrderRepository orderRepository;

    public InvoiceServiceImpl(InvoiceRepository invoiceRepository, OrderRepository orderRepository) {
        this.invoiceRepository = invoiceRepository;
        this.orderRepository = orderRepository;
    }

    @Override
    public InvoiceResponseDTO generateInvoiceForOrder(Integer orderId) {
        log.info("Invoice: Request received to generate invoice for order ID: {}", orderId);

        // 1. Check if invoice already exists
        Optional<Invoice> existingInvoice = invoiceRepository.findByOrderId(orderId);
        if (existingInvoice.isPresent()) {
            log.info("Invoice: Found existing invoice ID: {} for order ID: {}", existingInvoice.get().getId(), orderId);
            return toInvoiceResponseDTO(existingInvoice.get());
        }

        // 2. Fetch order if invoice does not exist
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> {
                    log.error("Invoice Error: Order ID {} not found", orderId);
                    return new ResourceNotFoundException("Order", "id", orderId);
                });

        // 3. Verify order is COMPLETED (transition PENDING to COMPLETED)
        if (order.getStatus() == OrderStatus.PENDING) {
            log.info("Invoice: Transitioning order ID {} status PENDING -> COMPLETED", orderId);
            order.setStatus(OrderStatus.COMPLETED);
            orderRepository.save(order);
        } else if (order.getStatus() == OrderStatus.CANCELLED) {
            log.error("Invoice Error: Order ID {} is CANCELLED. Cannot generate invoice.", orderId);
            throw new IllegalStateException("Cannot generate invoice for a CANCELLED order.");
        }

        // 4. Generate unique invoice number
        String invoiceNumber = "INV-" + System.currentTimeMillis();
        Invoice invoice = Invoice.builder()
                .order(order)
                .invoiceNumber(invoiceNumber)
                .build();

        Invoice saved = invoiceRepository.save(invoice);
        log.info("Invoice: Generated new invoice successfully. ID: {}, Invoice No: {}", saved.getId(), invoiceNumber);

        return toInvoiceResponseDTO(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public String renderInvoiceHtml(Integer orderId) {
        log.info("Invoice: Rendering HTML printable view for order ID: {}", orderId);
        InvoiceResponseDTO invoice = generateInvoiceForOrder(orderId);

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        String formattedDate = invoice.getGeneratedAt() != null ? invoice.getGeneratedAt().format(formatter) : "";

        // Build item rows dynamically
        StringBuilder itemRowsBuilder = new StringBuilder();
        for (InvoiceResponseDTO.InvoiceItemDTO item : invoice.getItems()) {
            itemRowsBuilder.append("<tr>")
                    .append("<td>").append(escapeHtml(item.getProductName())).append("</td>")
                    .append("<td style=\"text-align: right;\">₹").append(item.getUnitPrice().setScale(2, RoundingMode.HALF_UP)).append("</td>")
                    .append("<td style=\"text-align: right;\">").append(item.getQuantity()).append("</td>")
                    .append("<td style=\"text-align: right; color: #16a34a;\">-₹").append(item.getDiscountAmount().setScale(2, RoundingMode.HALF_UP)).append("</td>")
                    .append("<td style=\"text-align: right;\">").append(item.getGstRate().setScale(2, RoundingMode.HALF_UP)).append("%</td>")
                    .append("<td style=\"text-align: right;\">₹").append(item.getSubtotal().setScale(2, RoundingMode.HALF_UP)).append("</td>")
                    .append("</tr>\n");
        }

        // Load premium browser-printable HTML template
        return getHtmlTemplate()
                .replace("%INVOICE_NUMBER%", invoice.getInvoiceNumber())
                .replace("%DATE%", formattedDate)
                .replace("%CUSTOMER_NAME%", escapeHtml(invoice.getCustomerName()))
                .replace("%ORDER_ID%", String.valueOf(invoice.getOrderId()))
                .replace("%ITEM_ROWS%", itemRowsBuilder.toString())
                .replace("%GROSS_TOTAL%", invoice.getTotalAmount().setScale(2, RoundingMode.HALF_UP).toString())
                .replace("%TOTAL_SAVINGS%", invoice.getDiscountAmount().setScale(2, RoundingMode.HALF_UP).toString())
                .replace("%TOTAL_GST%", invoice.getGstAmount().setScale(2, RoundingMode.HALF_UP).toString())
                .replace("%GRAND_TOTAL%", invoice.getGrandTotal().setScale(2, RoundingMode.HALF_UP).toString());
    }

    // ── Mappers & Helpers ─────────────────────────────────────────────────────

    private InvoiceResponseDTO toInvoiceResponseDTO(Invoice invoice) {
        Order order = invoice.getOrder();
        List<InvoiceResponseDTO.InvoiceItemDTO> itemDTOs = new ArrayList<>();
        BigDecimal totalGross = order.getTotalAmount();
        BigDecimal totalDiscount = order.getDiscountAmount();
        BigDecimal discountAppliedSum = BigDecimal.ZERO;

        for (int i = 0; i < order.getOrderItems().size(); i++) {
            OrderItem item = order.getOrderItems().get(i);
            BigDecimal itemBaseAmount = item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity()));

            BigDecimal proportionateDiscount;
            if (i == order.getOrderItems().size() - 1) {
                proportionateDiscount = totalDiscount.subtract(discountAppliedSum);
            } else {
                if (totalGross.compareTo(BigDecimal.ZERO) > 0) {
                    proportionateDiscount = totalDiscount.multiply(itemBaseAmount)
                            .divide(totalGross, 2, RoundingMode.HALF_UP);
                } else {
                    proportionateDiscount = BigDecimal.ZERO;
                }
                discountAppliedSum = discountAppliedSum.add(proportionateDiscount);
            }

            itemDTOs.add(InvoiceResponseDTO.InvoiceItemDTO.builder()
                    .productId(item.getProduct().getId())
                    .productName(item.getProduct().getName())
                    .quantity(item.getQuantity())
                    .unitPrice(item.getUnitPrice())
                    .discountAmount(proportionateDiscount)
                    .gstRate(item.getProduct().getCategory().getGstSlab())
                    .gstAmount(item.getGstAmount())
                    .subtotal(item.getSubtotal())
                    .build());
        }

        return InvoiceResponseDTO.builder()
                .invoiceId(invoice.getId())
                .invoiceNumber(invoice.getInvoiceNumber())
                .orderId(order.getId())
                .generatedAt(invoice.getGeneratedAt())
                .customerName("Walk-in Customer")
                .totalAmount(order.getTotalAmount())
                .discountAmount(order.getDiscountAmount())
                .gstAmount(order.getGstAmount())
                .grandTotal(order.getGrandTotal())
                .items(itemDTOs)
                .build();
    }

    private String escapeHtml(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }

    private String getHtmlTemplate() {
        return "<!DOCTYPE html>\n" +
                "<html lang=\"en\">\n" +
                "<head>\n" +
                "    <meta charset=\"UTF-8\">\n" +
                "    <title>Invoice - %INVOICE_NUMBER%</title>\n" +
                "    <style>\n" +
                "        body {\n" +
                "            font-family: 'Segoe UI', system-ui, -apple-system, sans-serif;\n" +
                "            background-color: #f8fafc;\n" +
                "            color: #1e293b;\n" +
                "            margin: 0;\n" +
                "            padding: 40px;\n" +
                "        }\n" +
                "        .invoice-container {\n" +
                "            max-width: 800px;\n" +
                "            margin: 0 auto;\n" +
                "            background: #ffffff;\n" +
                "            padding: 40px;\n" +
                "            border-radius: 12px;\n" +
                "            box-shadow: 0 4px 6px -1px rgb(0 0 0 / 0.1), 0 2px 4px -2px rgb(0 0 0 / 0.1);\n" +
                "            border: 1px solid #e2e8f0;\n" +
                "        }\n" +
                "        .header {\n" +
                "            display: flex;\n" +
                "            justify-content: space-between;\n" +
                "            align-items: center;\n" +
                "            border-bottom: 2px solid #f1f5f9;\n" +
                "            padding-bottom: 24px;\n" +
                "            margin-bottom: 30px;\n" +
                "        }\n" +
                "        .logo {\n" +
                "            font-size: 24px;\n" +
                "            font-weight: 700;\n" +
                "            color: #2563eb;\n" +
                "            letter-spacing: -0.5px;\n" +
                "        }\n" +
                "        .invoice-info {\n" +
                "            text-align: right;\n" +
                "        }\n" +
                "        .invoice-info h1 {\n" +
                "            margin: 0 0 8px 0;\n" +
                "            font-size: 28px;\n" +
                "            color: #0f172a;\n" +
                "        }\n" +
                "        .invoice-info p {\n" +
                "            margin: 4px 0;\n" +
                "            color: #64748b;\n" +
                "        }\n" +
                "        .details-section {\n" +
                "            margin-bottom: 30px;\n" +
                "        }\n" +
                "        .details-section h3 {\n" +
                "            margin-top: 0;\n" +
                "            color: #334155;\n" +
                "            border-bottom: 1px solid #e2e8f0;\n" +
                "            padding-bottom: 8px;\n" +
                "        }\n" +
                "        .details-grid {\n" +
                "            display: grid;\n" +
                "            grid-template-columns: 1fr 1fr;\n" +
                "            gap: 20px;\n" +
                "        }\n" +
                "        .details-grid p {\n" +
                "            margin: 4px 0;\n" +
                "            color: #475569;\n" +
                "        }\n" +
                "        .invoice-table {\n" +
                "            width: 100%;\n" +
                "            border-collapse: collapse;\n" +
                "            margin-bottom: 30px;\n" +
                "        }\n" +
                "        .invoice-table th {\n" +
                "            background-color: #f8fafc;\n" +
                "            color: #475569;\n" +
                "            font-weight: 600;\n" +
                "            text-align: left;\n" +
                "            padding: 12px 16px;\n" +
                "            border-bottom: 2px solid #e2e8f0;\n" +
                "        }\n" +
                "        .invoice-table td {\n" +
                "            padding: 14px 16px;\n" +
                "            border-bottom: 1px solid #f1f5f9;\n" +
                "            color: #334155;\n" +
                "        }\n" +
                "        .totals-section {\n" +
                "            display: flex;\n" +
                "            justify-content: flex-end;\n" +
                "        }\n" +
                "        .totals-table {\n" +
                "            width: 300px;\n" +
                "            border-collapse: collapse;\n" +
                "        }\n" +
                "        .totals-table td {\n" +
                "            padding: 8px 0;\n" +
                "            color: #475569;\n" +
                "        }\n" +
                "        .totals-table tr.grand-total td {\n" +
                "            font-size: 20px;\n" +
                "            font-weight: 700;\n" +
                "            color: #0f172a;\n" +
                "            border-top: 2px solid #e2e8f0;\n" +
                "            padding-top: 12px;\n" +
                "        }\n" +
                "        .btn-container {\n" +
                "            margin-top: 40px;\n" +
                "            text-align: center;\n" +
                "        }\n" +
                "        .btn-print {\n" +
                "            background-color: #2563eb;\n" +
                "            color: white;\n" +
                "            padding: 10px 24px;\n" +
                "            border: none;\n" +
                "            border-radius: 6px;\n" +
                "            font-weight: 600;\n" +
                "            cursor: pointer;\n" +
                "            box-shadow: 0 4px 6px -1px rgba(37, 99, 235, 0.2);\n" +
                "            transition: all 0.2s ease;\n" +
                "        }\n" +
                "        .btn-print:hover {\n" +
                "            background-color: #1d4ed8;\n" +
                "        }\n" +
                "        @media print {\n" +
                "            body {\n" +
                "                background-color: #fff;\n" +
                "                padding: 0;\n" +
                "            }\n" +
                "            .invoice-container {\n" +
                "                box-shadow: none;\n" +
                "                border: none;\n" +
                "                padding: 0;\n" +
                "            }\n" +
                "            .no-print {\n" +
                "                display: none !important;\n" +
                "            }\n" +
                "        }\n" +
                "    </style>\n" +
                "</head>\n" +
                "<body>\n" +
                "    <div class=\"invoice-container\">\n" +
                "        <div class=\"header\">\n" +
                "            <div class=\"logo\">\n" +
                "                RetailTrack System\n" +
                "            </div>\n" +
                "            <div class=\"invoice-info\">\n" +
                "                <h1>INVOICE</h1>\n" +
                "                <p><strong>Invoice No:</strong> %INVOICE_NUMBER%</p>\n" +
                "                <p><strong>Date:</strong> %DATE%</p>\n" +
                "            </div>\n" +
                "        </div>\n" +
                "\n" +
                "        <div class=\"details-section\">\n" +
                "            <div class=\"details-grid\">\n" +
                "                <div>\n" +
                "                    <h3>Invoice To</h3>\n" +
                "                    <p><strong>Customer Name:</strong> %CUSTOMER_NAME%</p>\n" +
                "                    <p><strong>Order ID:</strong> %ORDER_ID%</p>\n" +
                "                </div>\n" +
                "                <div>\n" +
                "                    <h3>Payment Details</h3>\n" +
                "                    <p><strong>Status:</strong> COMPLETED</p>\n" +
                "                    <p><strong>Method:</strong> Cash/POS</p>\n" +
                "                </div>\n" +
                "            </div>\n" +
                "        </div>\n" +
                "\n" +
                "        <table class=\"invoice-table\">\n" +
                "            <thead>\n" +
                "                <tr>\n" +
                "                    <th>Product Name</th>\n" +
                "                    <th style=\"text-align: right;\">Unit Price</th>\n" +
                "                    <th style=\"text-align: right;\">Qty</th>\n" +
                "                    <th style=\"text-align: right;\">Discount</th>\n" +
                "                    <th style=\"text-align: right;\">GST %</th>\n" +
                "                    <th style=\"text-align: right;\">Subtotal</th>\n" +
                "                </tr>\n" +
                "            </thead>\n" +
                "            <tbody>\n" +
                "                %ITEM_ROWS%\n" +
                "            </tbody>\n" +
                "        </table>\n" +
                "\n" +
                "        <div class=\"totals-section\">\n" +
                "            <table class=\"totals-table\">\n" +
                "                <tr>\n" +
                "                    <td>Gross Total:</td>\n" +
                "                    <td style=\"text-align: right;\">₹%GROSS_TOTAL%</td>\n" +
                "                </tr>\n" +
                "                <tr>\n" +
                "                    <td>Total Savings:</td>\n" +
                "                    <td style=\"text-align: right; color: #16a34a;\">-₹%TOTAL_SAVINGS%</td>\n" +
                "                </tr>\n" +
                "                <tr>\n" +
                "                    <td>Total GST:</td>\n" +
                "                    <td style=\"text-align: right;\">+₹%TOTAL_GST%</td>\n" +
                "                </tr>\n" +
                "                <tr class=\"grand-total\">\n" +
                "                    <td>Grand Total:</td>\n" +
                "                    <td style=\"text-align: right; color: #2563eb;\">₹%GRAND_TOTAL%</td>\n" +
                "                </tr>\n" +
                "            </table>\n" +
                "        </div>\n" +
                "\n" +
                "        <div class=\"btn-container no-print\">\n" +
                "            <button class=\"btn-print\" onclick=\"window.print()\">Print Invoice</button>\n" +
                "        </div>\n" +
                "    </div>\n" +
                "\n" +
                "    <script>\n" +
                "        window.onload = function() {\n" +
                "            window.print();\n" +
                "        };\n" +
                "    </script>\n" +
                "</body>\n" +
                "</html>";
    }
}
