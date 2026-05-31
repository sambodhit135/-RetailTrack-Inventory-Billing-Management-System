package Retailtrack.retailtrack.service.impl;

import Retailtrack.retailtrack.dto.request.OrderRequestDTO;
import Retailtrack.retailtrack.dto.request.OrderItemRequestDTO;
import Retailtrack.retailtrack.dto.response.OrderResponseDTO;
import Retailtrack.retailtrack.entity.Coupon;
import Retailtrack.retailtrack.entity.Order;
import Retailtrack.retailtrack.entity.OrderItem;
import Retailtrack.retailtrack.entity.Product;
import Retailtrack.retailtrack.entity.StockAudit;
import Retailtrack.retailtrack.entity.enums.StockEventType;
import Retailtrack.retailtrack.entity.enums.OrderStatus;
import Retailtrack.retailtrack.exception.InsufficientStockException;
import Retailtrack.retailtrack.exception.InvalidCouponException;
import Retailtrack.retailtrack.exception.ResourceNotFoundException;
import Retailtrack.retailtrack.repository.CouponRepository;
import Retailtrack.retailtrack.repository.OrderItemRepository;
import Retailtrack.retailtrack.repository.OrderRepository;
import Retailtrack.retailtrack.repository.ProductRepository;
import Retailtrack.retailtrack.repository.StockAuditRepository;
import Retailtrack.retailtrack.service.OrderService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Implementation of {@link OrderService} managing order placement transactions,
 * stock validations, billing calculations, and persistence.
 */
@Slf4j
@Service
@Transactional
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final ProductRepository productRepository;
    private final CouponRepository couponRepository;
    private final StockAuditRepository stockAuditRepository;

    public OrderServiceImpl(OrderRepository orderRepository,
                            OrderItemRepository orderItemRepository,
                            ProductRepository productRepository,
                            CouponRepository couponRepository,
                            StockAuditRepository stockAuditRepository) {
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.productRepository = productRepository;
        this.couponRepository = couponRepository;
        this.stockAuditRepository = stockAuditRepository;
    }

    @Override
    public OrderResponseDTO createOrder(OrderRequestDTO dto) {
        log.info("Checkout: Creating new order for customer: '{}'", dto.getCustomerName());

        Order order = new Order();
        order.setStatus(OrderStatus.PENDING);
        order.setDiscountAmount(BigDecimal.ZERO);
        order.setTotalAmount(BigDecimal.ZERO);
        order.setGstAmount(BigDecimal.ZERO);
        order.setGrandTotal(BigDecimal.ZERO);
        
        // Save first to generate the Order ID for use in StockAudit remarks
        order = orderRepository.save(order);

        BigDecimal totalGrossAmount = BigDecimal.ZERO;
        List<OrderItem> orderItems = new ArrayList<>();

        // Loop 1: Verify product, check stock, deduct inventory, write audit logs, calculate initial gross amount
        for (OrderItemRequestDTO itemReq : dto.getItems()) {
            Product product = productRepository.findById(itemReq.getProductId())
                    .orElseThrow(() -> {
                        log.error("Checkout Error: Product ID {} not found.", itemReq.getProductId());
                        return new ResourceNotFoundException("Product", "id", itemReq.getProductId());
                    });

            if (itemReq.getQuantity() > product.getStockQuantity()) {
                log.error("Checkout Error: Insufficient stock for product '{}' (ID: {}). Requested: {}, Available: {}",
                        product.getName(), product.getId(), itemReq.getQuantity(), product.getStockQuantity());
                throw new InsufficientStockException(product.getName(), itemReq.getQuantity(), product.getStockQuantity());
            }

            // Deduct stock
            int stockBefore = product.getStockQuantity();
            int stockAfter = stockBefore - itemReq.getQuantity();
            product.setStockQuantity(stockAfter);
            productRepository.save(product);
            log.info("Checkout: Deducted stock for product ID {}: {} -> {}", product.getId(), stockBefore, product.getStockQuantity());

            // Create and persist StockAudit record right after stock deduction
            StockAudit audit = StockAudit.builder()
                    .product(product)
                    .eventType(StockEventType.SALE)
                    .quantityChange(-itemReq.getQuantity())
                    .stockBefore(stockBefore)
                    .stockAfter(stockAfter)
                    .remarks("Automated deduction from checkout order ID: " + order.getId())
                    .build();
            stockAuditRepository.save(audit);
            log.info("Checkout: Written StockAudit record for product ID {}", product.getId());

            BigDecimal unitPrice = product.getPrice();
            BigDecimal quantity = BigDecimal.valueOf(itemReq.getQuantity());
            BigDecimal itemBaseAmount = unitPrice.multiply(quantity);

            totalGrossAmount = totalGrossAmount.add(itemBaseAmount);

            // Construct OrderItem with placeholder subtotal/gst (calculated in pass 2)
            OrderItem orderItem = OrderItem.builder()
                    .order(order)
                    .product(product)
                    .quantity(itemReq.getQuantity())
                    .unitPrice(unitPrice)
                    .gstAmount(BigDecimal.ZERO)
                    .subtotal(BigDecimal.ZERO)
                    .build();

            orderItems.add(orderItem);
        }

        // Greedy Discount Optimization Algorithm (up to 2 coupons)
        List<Coupon> activeCoupons = couponRepository.findActiveCoupons(LocalDate.now());
        List<Coupon> selectedCoupons = new ArrayList<>();
        BigDecimal runningSubtotal = totalGrossAmount;

        log.info("Greedy: Optimizing discounts. Initial gross amount: {}", runningSubtotal);

        // Pre-select user requested coupon code if provided and valid
        if (dto.getCouponCode() != null && !dto.getCouponCode().trim().isEmpty()) {
            String reqCode = dto.getCouponCode().trim();
            Optional<Coupon> reqCouponOpt = couponRepository.findByCodeIgnoreCase(reqCode);
            if (reqCouponOpt.isPresent()) {
                Coupon reqCoupon = reqCouponOpt.get();
                // Validate manually
                if (Boolean.FALSE.equals(reqCoupon.getIsActive())) {
                    throw new InvalidCouponException("Requested coupon is inactive");
                }
                if (reqCoupon.getExpiryDate() != null && reqCoupon.getExpiryDate().isBefore(LocalDate.now())) {
                    throw new InvalidCouponException("Requested coupon has expired");
                }
                if (reqCoupon.getUsedCount() >= reqCoupon.getMaxUses()) {
                    throw new InvalidCouponException("Requested coupon usage limit reached");
                }
                if (runningSubtotal.compareTo(reqCoupon.getMinCartValue()) < 0) {
                    throw new InvalidCouponException("Cart subtotal " + runningSubtotal + " is below minimum cart value of " + reqCoupon.getMinCartValue());
                }

                selectedCoupons.add(reqCoupon);
                reqCoupon.setUsedCount(reqCoupon.getUsedCount() + 1);
                couponRepository.save(reqCoupon);

                BigDecimal savings = BigDecimal.ZERO;
                if (reqCoupon.getDiscountType() == Retailtrack.retailtrack.entity.enums.DiscountType.PERCENTAGE) {
                    savings = runningSubtotal.multiply(reqCoupon.getDiscountValue()).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
                } else {
                    savings = reqCoupon.getDiscountValue();
                }
                if (savings.compareTo(runningSubtotal) > 0) {
                    savings = runningSubtotal;
                }
                runningSubtotal = runningSubtotal.subtract(savings);
                log.info("Greedy: Pre-selected requested coupon '{}' providing savings of {}. New subtotal: {}",
                        reqCode, savings, runningSubtotal);
            } else {
                throw new InvalidCouponException("Requested coupon not found: " + reqCode);
            }
        }

        // Greedy search for remaining slots
        while (selectedCoupons.size() < 2) {
            Coupon bestCoupon = null;
            BigDecimal bestSavings = BigDecimal.ZERO;

            for (Coupon coupon : activeCoupons) {
                if (selectedCoupons.contains(coupon)) {
                    continue;
                }
                if (coupon.getUsedCount() >= coupon.getMaxUses()) {
                    continue;
                }
                if (runningSubtotal.compareTo(coupon.getMinCartValue()) < 0) {
                    continue;
                }

                BigDecimal savings = BigDecimal.ZERO;
                if (coupon.getDiscountType() == Retailtrack.retailtrack.entity.enums.DiscountType.PERCENTAGE) {
                    savings = runningSubtotal.multiply(coupon.getDiscountValue()).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
                } else if (coupon.getDiscountType() == Retailtrack.retailtrack.entity.enums.DiscountType.FLAT) {
                    savings = coupon.getDiscountValue();
                }

                if (savings.compareTo(runningSubtotal) > 0) {
                    savings = runningSubtotal;
                }

                if (savings.compareTo(bestSavings) > 0) {
                    bestSavings = savings;
                    bestCoupon = coupon;
                }
            }

            if (bestCoupon != null && bestSavings.compareTo(BigDecimal.ZERO) > 0) {
                selectedCoupons.add(bestCoupon);
                bestCoupon.setUsedCount(bestCoupon.getUsedCount() + 1);
                couponRepository.save(bestCoupon);

                runningSubtotal = runningSubtotal.subtract(bestSavings);
                log.info("Greedy: Selected coupon '{}' providing savings of {}. New subtotal: {}",
                        bestCoupon.getCode(), bestSavings, runningSubtotal);
            } else {
                break; // No coupon provides savings > 0
            }
        }

        BigDecimal totalDiscount = totalGrossAmount.subtract(runningSubtotal);
        log.info("Greedy: Selection complete. Selected coupons: {}. Total discount: {}",
                selectedCoupons.stream().map(Coupon::getCode).collect(Collectors.toList()), totalDiscount);

        // Loop 2: Distribute discount proportionally, calculate GST and subtotal per item
        BigDecimal totalDiscountApplied = BigDecimal.ZERO;
        BigDecimal totalGstAmount = BigDecimal.ZERO;

        for (int i = 0; i < orderItems.size(); i++) {
            OrderItem item = orderItems.get(i);
            BigDecimal itemBaseAmount = item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity()));
            
            BigDecimal proportionateDiscount;
            if (i == orderItems.size() - 1) {
                // Last item gets the remainder of the discount to avoid rounding discrepancies
                proportionateDiscount = totalDiscount.subtract(totalDiscountApplied);
            } else {
                if (totalGrossAmount.compareTo(BigDecimal.ZERO) > 0) {
                    proportionateDiscount = totalDiscount.multiply(itemBaseAmount)
                            .divide(totalGrossAmount, 2, RoundingMode.HALF_UP);
                } else {
                    proportionateDiscount = BigDecimal.ZERO;
                }
                totalDiscountApplied = totalDiscountApplied.add(proportionateDiscount);
            }

            // Item Subtotal = (Quantity * Price) - Proportionate Discount
            BigDecimal itemSubtotal = itemBaseAmount.subtract(proportionateDiscount);
            if (itemSubtotal.compareTo(BigDecimal.ZERO) < 0) {
                itemSubtotal = BigDecimal.ZERO;
            }

            // GST Amount = Item Subtotal * (GstSlab / 100)
            BigDecimal gstPercent = item.getProduct().getCategory().getGstSlab();
            BigDecimal itemGstAmount = itemSubtotal.multiply(gstPercent)
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);

            // Total Line Item Amount = Item Subtotal + GST Amount
            BigDecimal lineSubtotal = itemSubtotal.add(itemGstAmount);

            item.setGstAmount(itemGstAmount);
            item.setSubtotal(lineSubtotal);

            totalGstAmount = totalGstAmount.add(itemGstAmount);
        }

        order.setTotalAmount(totalGrossAmount);
        order.setDiscountAmount(totalDiscount);
        order.setGstAmount(totalGstAmount);
        order.setGrandTotal(totalGrossAmount.subtract(totalDiscount).add(totalGstAmount));
        order.setOrderItems(orderItems);

        Order saved = orderRepository.save(order);
        log.info("Checkout: Order ID {} saved successfully. Gross: {}, Discount: {}, GST: {}, Grand Total: {}",
                saved.getId(), saved.getTotalAmount(), saved.getDiscountAmount(), saved.getGstAmount(), saved.getGrandTotal());

        OrderResponseDTO response = toResponseDTO(saved);
        // Explicitly map customerName since it's not persisted
        response.setCustomerName(dto.getCustomerName());
        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public OrderResponseDTO getOrderById(Integer id) {
        log.info("Fetching order with ID: {}", id);

        Order order = orderRepository.findById(id)
                .orElseThrow(() -> {
                    log.error("Fetch Error: Order with ID {} not found.", id);
                    return new ResourceNotFoundException("Order", "id", id);
                });

        log.info("Successfully fetched order with ID: {}", id);
        return toResponseDTO(order);
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderResponseDTO> getAllOrders() {
        log.info("Fetching all orders in the system.");

        List<OrderResponseDTO> orders = orderRepository.findAllByOrderByCreatedAtDesc()
                .stream()
                .map(this::toResponseDTO)
                .collect(Collectors.toList());

        log.info("Successfully retrieved {} orders.", orders.size());
        return orders;
    }

    // ── Mapper ────────────────────────────────────────────────────────────────

    private OrderResponseDTO toResponseDTO(Order order) {
        List<OrderResponseDTO.OrderItemResponseDTO> itemDTOs = order.getOrderItems().stream()
                .map(item -> OrderResponseDTO.OrderItemResponseDTO.builder()
                        .productId(item.getProduct().getId())
                        .productName(item.getProduct().getName())
                        .quantity(item.getQuantity())
                        .unitPrice(item.getUnitPrice())
                        .gstAmount(item.getGstAmount())
                        .subtotal(item.getSubtotal())
                        .build())
                .collect(Collectors.toList());

        String invoiceNo = order.getInvoice() != null ? order.getInvoice().getInvoiceNumber() : null;

        return OrderResponseDTO.builder()
                .orderId(order.getId())
                .customerName("Walk-in Customer") // Fallback placeholder since name is not in db schema
                .totalAmount(order.getTotalAmount())
                .discountAmount(order.getDiscountAmount())
                .gstAmount(order.getGstAmount())
                .grandTotal(order.getGrandTotal())
                .status(order.getStatus().name())
                .invoiceNumber(invoiceNo)
                .createdAt(order.getCreatedAt())
                .items(itemDTOs)
                .build();
    }
}
