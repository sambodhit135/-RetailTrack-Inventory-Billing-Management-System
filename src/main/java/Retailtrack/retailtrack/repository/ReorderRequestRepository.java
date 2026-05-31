package Retailtrack.retailtrack.repository;

import Retailtrack.retailtrack.entity.ReorderRequest;
import Retailtrack.retailtrack.entity.enums.ReorderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReorderRequestRepository extends JpaRepository<ReorderRequest, Integer> {

    List<ReorderRequest> findByProductId(Integer productId);

    List<ReorderRequest> findByStatus(ReorderStatus status);

    List<ReorderRequest> findBySupplierId(Integer supplierId);
}
