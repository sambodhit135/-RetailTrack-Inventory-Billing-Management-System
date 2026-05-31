package Retailtrack.retailtrack.service;

import Retailtrack.retailtrack.dto.ReorderItemPriorityDTO;
import java.util.List;

/**
 * Service contract for the Smart Reorder Engine operations.
 */
public interface ReorderEngineService {

    /**
     * Retrieves the top N low stock products prioritized by their urgency scores.
     * Uses a min-heap structure to rank the elements.
     *
     * @param n the number of critical products to retrieve
     * @return the list of prioritized reorder item details
     */
    List<ReorderItemPriorityDTO> getTopLowStockProducts(int n);

    /**
     * Scans products below reorder thresholds and automatically drafts PENDING reorder requests.
     */
    void triggerAutomatedReorderRequests();
}
