package Retailtrack.retailtrack.controller;

import Retailtrack.retailtrack.dto.response.InvoiceResponseDTO;
import Retailtrack.retailtrack.service.InvoiceService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST Controller for Invoice management.
 * Exposes endpoints under {@code /api/invoices} for JSON metadata and HTML printable rendering.
 */
@Slf4j
@RestController
@RequestMapping("/api/invoices")
public class InvoiceController {

    private final InvoiceService invoiceService;

    public InvoiceController(InvoiceService invoiceService) {
        this.invoiceService = invoiceService;
    }

    // ── GET /api/invoices/order/{orderId} ─────────────────────────────────────

    /**
     * Generates or retrieves the invoice details in JSON format.
     *
     * @param orderId the order ID
     * @return {@code 200 OK} with the invoice response DTO
     */
    @GetMapping("/order/{orderId}")
    public ResponseEntity<InvoiceResponseDTO> getInvoiceForOrder(@PathVariable Integer orderId) {
        log.info("REST: Request received to generate/fetch JSON invoice for order ID: {}", orderId);
        InvoiceResponseDTO response = invoiceService.generateInvoiceForOrder(orderId);
        return ResponseEntity.ok(response);
    }

    // ── GET /api/invoices/render/{orderId} ────────────────────────────────────

    /**
     * Renders and returns a browser-printable, clean HTML view of the invoice.
     * Sets the Content-Type header explicitly to text/html.
     *
     * @param orderId the order ID
     * @return {@code 200 OK} containing the HTML invoice page
     */
    @GetMapping("/render/{orderId}")
    public ResponseEntity<String> renderInvoiceHtml(@PathVariable Integer orderId) {
        log.info("REST: Request received to render HTML invoice for order ID: {}", orderId);
        String html = invoiceService.renderInvoiceHtml(orderId);
        return ResponseEntity.ok()
                .header("Content-Type", "text/html; charset=UTF-8")
                .body(html);
    }
}
