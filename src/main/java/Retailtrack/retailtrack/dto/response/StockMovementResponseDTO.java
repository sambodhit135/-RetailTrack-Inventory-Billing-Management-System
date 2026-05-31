package Retailtrack.retailtrack.dto.response;

import lombok.*;
import java.time.LocalDateTime;

/**
 * DTO representing a stock movement audit log entry.
 * Populated using raw JDBC template query with custom native join.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockMovementResponseDTO {

    private Integer id;
    private Integer productId;
    private String productName;
    private String eventType;
    private Integer quantityChange;
    private Integer stockBefore;
    private Integer stockAfter;
    private String remarks;
    private LocalDateTime createdAt;
}
