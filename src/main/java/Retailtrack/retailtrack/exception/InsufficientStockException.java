package Retailtrack.retailtrack.exception;

/**
 * Thrown when a customer tries to order more units of a product
 * than are currently available in stock.
 *
 * Maps to HTTP 409 Conflict in {@link GlobalExceptionHandler}.
 */
public class InsufficientStockException extends RuntimeException {

    public InsufficientStockException(String message) {
        super(message);
    }

    public InsufficientStockException(String productName, int requested, int available) {
        super(String.format(
                "Insufficient stock for '%s': requested %d but only %d available.",
                productName, requested, available
        ));
    }
}
