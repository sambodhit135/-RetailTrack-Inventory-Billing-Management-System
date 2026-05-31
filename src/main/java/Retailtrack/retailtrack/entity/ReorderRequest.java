package Retailtrack.retailtrack.entity;

import Retailtrack.retailtrack.entity.enums.ReorderStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "reorder_requests")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReorderRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "supplier_id", nullable = false)
    private Supplier supplier;

    /** How many units were requested from the supplier. */
    @Column(name = "requested_quantity", nullable = false)
    private Integer requestedQuantity;

    /**
     * Lifecycle of the reorder:
     * PENDING   → created by the Reorder Engine, not yet communicated to supplier.
     * SENT      → supplier has been notified.
     * FULFILLED → stock has been received and updated.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    @Builder.Default
    private ReorderStatus status = ReorderStatus.PENDING;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // ── Lifecycle ─────────────────────────────────────────────────────────────
    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
