package Retailtrack.retailtrack.service;

import Retailtrack.retailtrack.entity.StockAudit;
import java.util.List;

/**
 * Service contract for Stock Audit operations.
 */
public interface StockAuditService {

    /**
     * Retrieves all stock audit records for a given product ID, ordered by creation date descending.
     *
     * @param productId the ID of the product
     * @return list of matching stock audit records
     */
    List<StockAudit> getAuditsByProductId(Long productId);
}
