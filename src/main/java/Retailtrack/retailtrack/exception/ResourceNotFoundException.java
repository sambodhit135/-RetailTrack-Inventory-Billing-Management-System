package Retailtrack.retailtrack.exception;

/**
 * Thrown when a requested resource (Product, Order, Category, etc.)
 * does not exist in the database.
 *
 * Maps to HTTP 404 Not Found in {@link GlobalExceptionHandler}.
 */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }

    public ResourceNotFoundException(String resourceName, String fieldName, Object fieldValue) {
        super(String.format("%s not found with %s: '%s'", resourceName, fieldName, fieldValue));
    }
}
