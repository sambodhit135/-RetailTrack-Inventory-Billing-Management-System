package Retailtrack.retailtrack.repository;

import Retailtrack.retailtrack.entity.StockAudit;
import Retailtrack.retailtrack.entity.enums.StockEventType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StockAuditRepository extends JpaRepository<StockAudit, Integer> {

    /** Full audit trail for a single product, newest first. */
    List<StockAudit> findByProductIdOrderByCreatedAtDesc(Integer productId);

    /** All events of a specific type (e.g. all SALEs or all RESTOCKs). */
    List<StockAudit> findByEventType(StockEventType eventType);
}
