package Retailtrack.retailtrack.controller;

import Retailtrack.retailtrack.dto.ReorderItemPriorityDTO;
import Retailtrack.retailtrack.service.ReorderEngineService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST Controller for the Smart Reorder Engine operations.
 * Exposes endpoints under {@code /api/reorder} for low stock queries and alerts.
 */
@Slf4j
@RestController
@RequestMapping("/api/reorder")
public class ReorderEngineController {

    private final ReorderEngineService reorderEngineService;

    public ReorderEngineController(ReorderEngineService reorderEngineService) {
        this.reorderEngineService = reorderEngineService;
    }

    // ── GET /api/reorder/critical ─────────────────────────────────────────────

    /**
     * Retrieves the top N critical low-stock products prioritized by their urgency.
     *
     * @param limit maximum number of items to return (defaults to 5)
     * @return {@code 200 OK} with the prioritized list of products
     */
    @GetMapping("/critical")
    public ResponseEntity<List<ReorderItemPriorityDTO>> getCriticalProducts(
            @RequestParam(name = "limit", defaultValue = "5") int limit) {
        log.info("REST: Received request for top {} critical low stock products", limit);
        List<ReorderItemPriorityDTO> critical = reorderEngineService.getTopLowStockProducts(limit);
        return ResponseEntity.ok(critical);
    }

    // ── POST /api/reorder/trigger ─────────────────────────────────────────────

    /**
     * Manually triggers the automated reorder request drafting system.
     *
     * @return {@code 200 OK} indicating successful completion
     */
    @PostMapping("/trigger")
    public ResponseEntity<Void> triggerAutomatedReorder() {
        log.info("REST: Received request to manually trigger automated reorder requests");
        reorderEngineService.triggerAutomatedReorderRequests();
        return ResponseEntity.ok().build();
    }
}
