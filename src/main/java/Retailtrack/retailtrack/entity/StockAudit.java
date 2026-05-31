package Retailtrack.retailtrack.entity;

import Retailtrack.retailtrack.entity.enums.StockEventType;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "stock_audit")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StockAudit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @com.fasterxml.jackson.annotation.JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    /**
     * What caused this stock change:
     * SALE       → stock decreased due to a customer order.
     * RESTOCK    → stock increased due to a supplier delivery.
     * ADJUSTMENT → manual correction by an admin.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 15)
    private StockEventType eventType;

    /**
     * How much stock changed. Negative for SALE, positive for RESTOCK/ADJUSTMENT.
     */
    @Column(name = "quantity_change", nullable = false)
    private Integer quantityChange;

    /** Stock level immediately before this event. */
    @Column(name = "stock_before", nullable = false)
    private Integer stockBefore;

    /** Stock level immediately after this event. */
    @Column(name = "stock_after", nullable = false)
    private Integer stockAfter;

    @Column(length = 255)
    private String remarks;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // ── Lifecycle ─────────────────────────────────────────────────────────────
    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
