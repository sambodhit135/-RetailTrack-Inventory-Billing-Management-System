package Retailtrack.retailtrack.service.impl;

import Retailtrack.retailtrack.entity.StockAudit;
import Retailtrack.retailtrack.repository.StockAuditRepository;
import Retailtrack.retailtrack.service.StockAuditService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Service implementation for Stock Audit management.
 */
@Slf4j
@Service
@Transactional(readOnly = true)
public class StockAuditServiceImpl implements StockAuditService {

    private final StockAuditRepository stockAuditRepository;

    public StockAuditServiceImpl(StockAuditRepository stockAuditRepository) {
        this.stockAuditRepository = stockAuditRepository;
    }

    @Override
    public List<StockAudit> getAuditsByProductId(Long productId) {
        log.info("Fetching stock audits for product ID: {}", productId);
        if (productId == null) {
            return List.of();
        }
        return stockAuditRepository.findByProductIdOrderByCreatedAtDesc(productId.intValue());
    }
}
