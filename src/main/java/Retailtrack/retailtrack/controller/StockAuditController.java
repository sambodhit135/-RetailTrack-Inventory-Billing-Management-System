package Retailtrack.retailtrack.controller;

import Retailtrack.retailtrack.entity.StockAudit;
import Retailtrack.retailtrack.service.StockAuditService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for retrieving stock audit trail data.
 */
@Slf4j
@RestController
@RequestMapping("/api/stock-audit")
public class StockAuditController {

    private final StockAuditService stockAuditService;

    public StockAuditController(StockAuditService stockAuditService) {
        this.stockAuditService = stockAuditService;
    }

    /**
     * Retrieves all stock audit events for a specific product.
     *
     * @param productId the ID of the product
     * @return a list of StockAudit records, newest first
     */
    @GetMapping("/product/{productId}")
    public ResponseEntity<List<StockAudit>> getProductStockAudits(@PathVariable Long productId) {
        log.info("REST: Received request to fetch stock audit trail for product ID: {}", productId);
        List<StockAudit> audits = stockAuditService.getAuditsByProductId(productId);
        return ResponseEntity.ok(audits);
    }
}
