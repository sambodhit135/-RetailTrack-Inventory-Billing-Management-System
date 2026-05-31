package Retailtrack.retailtrack.service;

import Retailtrack.retailtrack.dto.response.InvoiceResponseDTO;

/**
 * Service contract for Invoice rendering and generation operations.
 */
public interface InvoiceService {

    /**
     * Generates or retrieves the invoice for a given order.
     * Completes PENDING orders to COMPLETED status automatically upon invoicing.
     *
     * @param orderId the ID of the checkout order
     * @return the invoice response details DTO
     * @throws Retailtrack.retailtrack.exception.ResourceNotFoundException if order is not found
     * @throws IllegalStateException if the order is cancelled
     */
    InvoiceResponseDTO generateInvoiceForOrder(Integer orderId);

    /**
     * Renders a browser-printable, clean HTML page string for the invoice of a given order.
     *
     * @param orderId the ID of the checkout order
     * @return dynamic HTML5 invoice template page string
     * @throws Retailtrack.retailtrack.exception.ResourceNotFoundException if order is not found
     */
    String renderInvoiceHtml(Integer orderId);
}
