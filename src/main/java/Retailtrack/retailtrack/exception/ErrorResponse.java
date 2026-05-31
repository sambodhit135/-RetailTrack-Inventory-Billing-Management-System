package Retailtrack.retailtrack.exception;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;

/**
 * Uniform error envelope returned as JSON for every exception handled
 * by {@link GlobalExceptionHandler}.
 *
 * Example response body:
 * <pre>
 * {
 *   "timestamp": "2024-11-01T14:32:10",
 *   "status": 404,
 *   "error": "Product not found with id: '99'",
 *   "path": "/api/products/99"
 * }
 * </pre>
 */
public record ErrorResponse(

        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        LocalDateTime timestamp,

        int status,

        String error,

        String path
) {
    /** Convenience factory — sets timestamp to now automatically. */
    public static ErrorResponse of(int status, String error, String path) {
        return new ErrorResponse(LocalDateTime.now(), status, error, path);
    }
}
