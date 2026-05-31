package Retailtrack.retailtrack.service.impl;

import Retailtrack.retailtrack.dto.ReorderItemPriorityDTO;
import Retailtrack.retailtrack.entity.Product;
import Retailtrack.retailtrack.entity.ProductSupplier;
import Retailtrack.retailtrack.entity.ReorderRequest;
import Retailtrack.retailtrack.entity.Supplier;
import Retailtrack.retailtrack.entity.enums.ReorderStatus;
import Retailtrack.retailtrack.repository.ProductRepository;
import Retailtrack.retailtrack.repository.ProductSupplierRepository;
import Retailtrack.retailtrack.repository.ReorderRequestRepository;
import Retailtrack.retailtrack.service.ReorderEngineService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Implementation of {@link ReorderEngineService} providing stock priority ranking
 * and automated drafting of reorder requests.
 */
@Slf4j
@Service
@Transactional
public class ReorderEngineServiceImpl implements ReorderEngineService {

    private final ProductRepository productRepository;
    private final ProductSupplierRepository productSupplierRepository;
    private final ReorderRequestRepository reorderRequestRepository;

    public ReorderEngineServiceImpl(ProductRepository productRepository,
                                    ProductSupplierRepository productSupplierRepository,
                                    ReorderRequestRepository reorderRequestRepository) {
        this.productRepository = productRepository;
        this.productSupplierRepository = productSupplierRepository;
        this.reorderRequestRepository = reorderRequestRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReorderItemPriorityDTO> getTopLowStockProducts(int n) {
        log.info("ReorderEngine: Entering getTopLowStockProducts with limit n = {}", n);

        if (n <= 0) {
            log.warn("ReorderEngine: Limit n must be greater than zero. Received: {}. Returning empty list.", n);
            return Collections.emptyList();
        }

        // Fetch products below or at reorder threshold
        List<Product> lowStockProducts = productRepository.findProductsBelowReorderThreshold();
        log.info("ReorderEngine: Found {} products at or below reorder threshold", lowStockProducts.size());

        // Min-Heap PriorityQueue to sort products based on priority score (lowest score first)
        PriorityQueue<ReorderItemPriorityDTO> priorityQueue = new PriorityQueue<>();

        for (Product product : lowStockProducts) {
            int leadTime = 5; // Default lead time if no supplier exists
            Optional<ProductSupplier> primarySupplierOpt = productSupplierRepository
                    .findByProductIdAndIsPrimaryTrue(product.getId());

            if (primarySupplierOpt.isPresent() && primarySupplierOpt.get().getSupplier() != null) {
                Integer days = primarySupplierOpt.get().getSupplier().getLeadTimeDays();
                if (days != null && days > 0) {
                    leadTime = days;
                }
            } else {
                // Fallback to first associated supplier if no primary supplier is set
                List<ProductSupplier> allSuppliers = productSupplierRepository.findByProductId(product.getId());
                if (!allSuppliers.isEmpty() && allSuppliers.get(0).getSupplier() != null) {
                    Integer days = allSuppliers.get(0).getSupplier().getLeadTimeDays();
                    if (days != null && days > 0) {
                        leadTime = days;
                    }
                }
            }

            int stock = product.getStockQuantity() != null ? product.getStockQuantity() : 0;
            int threshold = product.getReorderThreshold() != null ? product.getReorderThreshold() : 0;
            BigDecimal sales = product.getAvgDailySales();

            // Calculate Priority Score = (stockQuantity - reorderThreshold) / (avgDailySales * supplierLeadTime)
            double denominator = 0.0;
            if (sales != null) {
                denominator = sales.doubleValue() * leadTime;
            }

            double score;
            if (denominator <= 0.0) {
                // Denominator of 0 or less implies invalid sales rate or lead time
                // Assign a very large score to push it to the end of the priority list (not urgent)
                score = Double.MAX_VALUE;
            } else {
                score = (double) (stock - threshold) / denominator;
            }

            ReorderItemPriorityDTO dto = ReorderItemPriorityDTO.builder()
                    .productId(product.getId())
                    .productName(product.getName())
                    .currentStock(stock)
                    .reorderThreshold(threshold)
                    .priorityScore(score)
                    .build();

            priorityQueue.offer(dto);
        }

        // Collect top-N items
        List<ReorderItemPriorityDTO> result = new ArrayList<>();
        int count = 0;
        while (!priorityQueue.isEmpty() && count < n) {
            result.add(priorityQueue.poll());
            count++;
        }

        log.info("ReorderEngine: Exiting getTopLowStockProducts. Returning {} prioritized products.", result.size());
        return result;
    }

    @Override
    public void triggerAutomatedReorderRequests() {
        log.info("ReorderEngine: Entering triggerAutomatedReorderRequests");

        // Fetch products below or at reorder threshold
        List<Product> lowStockProducts = productRepository.findProductsBelowReorderThreshold();
        log.info("ReorderEngine: Found {} products at or below threshold.", lowStockProducts.size());

        int draftedCount = 0;

        for (Product product : lowStockProducts) {
            // Find supplier
            Supplier supplier = null;
            Optional<ProductSupplier> primarySupplierOpt = productSupplierRepository
                    .findByProductIdAndIsPrimaryTrue(product.getId());

            if (primarySupplierOpt.isPresent()) {
                supplier = primarySupplierOpt.get().getSupplier();
            } else {
                List<ProductSupplier> allSuppliers = productSupplierRepository.findByProductId(product.getId());
                if (!allSuppliers.isEmpty()) {
                    supplier = allSuppliers.get(0).getSupplier();
                }
            }

            if (supplier == null) {
                log.warn("ReorderEngine: Skipping product '{}' (ID: {}). No associated supplier found.",
                        product.getName(), product.getId());
                continue;
            }

            // Verify if there is already a PENDING reorder request for this product
            List<ReorderRequest> existing = reorderRequestRepository.findByProductId(product.getId());
            boolean hasPending = existing.stream().anyMatch(r -> r.getStatus() == ReorderStatus.PENDING);

            if (hasPending) {
                log.info("ReorderEngine: PENDING reorder request already exists for product '{}' (ID: {}). Skipping.",
                        product.getName(), product.getId());
                continue;
            }

            // Calculate quantity = (reorderThreshold * 2) - stockQuantity
            int stock = product.getStockQuantity() != null ? product.getStockQuantity() : 0;
            int threshold = product.getReorderThreshold() != null ? product.getReorderThreshold() : 0;
            int requestedQuantity = (threshold * 2) - stock;

            if (requestedQuantity <= 0) {
                log.warn("ReorderEngine: Calculated request quantity for '{}' (ID: {}) is <= 0 ({}). Skipping.",
                        product.getName(), product.getId(), requestedQuantity);
                continue;
            }

            ReorderRequest request = ReorderRequest.builder()
                    .product(product)
                    .supplier(supplier)
                    .requestedQuantity(requestedQuantity)
                    .status(ReorderStatus.PENDING)
                    .build();

            reorderRequestRepository.save(request);
            draftedCount++;

            log.info("ReorderEngine: Drafted PENDING reorder request for product '{}' (ID: {}) with supplier '{}' for {} units.",
                    product.getName(), product.getId(), supplier.getName(), requestedQuantity);
        }

        log.info("ReorderEngine: Exiting triggerAutomatedReorderRequests. Total reorder requests drafted: {}", draftedCount);
    }
}
