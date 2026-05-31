package Retailtrack.retailtrack.dto;

import lombok.*;

/**
 * Priority DTO wrapper representing a product's reorder priority score.
 * Implements Comparable to sort ascendingly by priorityScore (min-heap logic).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReorderItemPriorityDTO implements Comparable<ReorderItemPriorityDTO> {

    private Integer productId;
    private String productName;
    private Integer currentStock;
    private Integer reorderThreshold;
    private Double priorityScore;

    @Override
    public int compareTo(ReorderItemPriorityDTO other) {
        if (this.priorityScore == null && other.priorityScore == null) {
            return 0;
        }
        if (this.priorityScore == null) {
            return 1; // Put null values at the end of the heap
        }
        if (other.priorityScore == null) {
            return -1;
        }
        return Double.compare(this.priorityScore, other.priorityScore);
    }
}
