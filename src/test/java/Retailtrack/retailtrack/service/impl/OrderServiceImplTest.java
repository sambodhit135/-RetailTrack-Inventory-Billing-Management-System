package Retailtrack.retailtrack.service.impl;

import Retailtrack.retailtrack.dto.request.OrderRequestDTO;
import Retailtrack.retailtrack.dto.request.OrderItemRequestDTO;
import Retailtrack.retailtrack.dto.response.OrderResponseDTO;
import Retailtrack.retailtrack.entity.*;
import Retailtrack.retailtrack.entity.enums.DiscountType;
import Retailtrack.retailtrack.exception.InsufficientStockException;
import Retailtrack.retailtrack.repository.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Component test suite for the OrderServiceImpl checking order checkouts,
 * Greedy coupon selections, GST splits, and rollback safeguards.
 */
@ExtendWith(MockitoExtension.class)
public class OrderServiceImplTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OrderItemRepository orderItemRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private CouponRepository couponRepository;

    @Mock
    private StockAuditRepository stockAuditRepository;

    @InjectMocks
    private OrderServiceImpl orderService;

    @Test
    public void testCreateOrder_GreedyCouponSelection() {
        // Arrange
        Category categoryA = Category.builder().id(1).name("Cat A").gstSlab(BigDecimal.valueOf(10.00)).build();
        Category categoryB = Category.builder().id(2).name("Cat B").gstSlab(BigDecimal.valueOf(5.00)).build();

        Product productA = Product.builder()
                .id(1)
                .name("Product A")
                .price(BigDecimal.valueOf(100.00))
                .stockQuantity(10)
                .category(categoryA)
                .build();

        Product productB = Product.builder()
                .id(2)
                .name("Product B")
                .price(BigDecimal.valueOf(300.00))
                .stockQuantity(5)
                .category(categoryB)
                .build();

        // Target cart total: 2 * 100 + 1 * 300 = 500
        OrderRequestDTO dto = OrderRequestDTO.builder()
                .customerName("Alice")
                .couponCode(null)
                .items(Arrays.asList(
                        OrderItemRequestDTO.builder().productId(1).quantity(2).build(),
                        OrderItemRequestDTO.builder().productId(2).quantity(1).build()
                ))
                .build();

        // Setup active coupons:
        // Coupon 1: FLAT 50, min = 100
        Coupon coupon1 = Coupon.builder().id(101).code("FLAT50").discountType(DiscountType.FLAT).discountValue(BigDecimal.valueOf(50.00)).minCartValue(BigDecimal.valueOf(100.00)).maxUses(10).usedCount(1).isActive(true).build();
        // Coupon 2: PERCENTAGE 10%, min = 200
        Coupon coupon2 = Coupon.builder().id(102).code("TENPERCENT").discountType(DiscountType.PERCENTAGE).discountValue(BigDecimal.valueOf(10.00)).minCartValue(BigDecimal.valueOf(200.00)).maxUses(10).usedCount(0).isActive(true).build();
        // Coupon 3: FLAT 100, min = 600 (not applicable because 500 < 600)
        Coupon coupon3 = Coupon.builder().id(103).code("FLAT100").discountType(DiscountType.FLAT).discountValue(BigDecimal.valueOf(100.00)).minCartValue(BigDecimal.valueOf(600.00)).maxUses(5).usedCount(0).isActive(true).build();
        // Coupon 4: PERCENTAGE 20%, min = 300
        Coupon coupon4 = Coupon.builder().id(104).code("TWENTYPERCENT").discountType(DiscountType.PERCENTAGE).discountValue(BigDecimal.valueOf(20.00)).minCartValue(BigDecimal.valueOf(300.00)).maxUses(5).usedCount(0).isActive(true).build();

        when(productRepository.findById(1)).thenReturn(Optional.of(productA));
        when(productRepository.findById(2)).thenReturn(Optional.of(productB));
        when(couponRepository.findActiveCoupons(any(LocalDate.class))).thenReturn(Arrays.asList(coupon1, coupon2, coupon3, coupon4));

        // Mock Order saved state
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
            Order order = invocation.getArgument(0);
            if (order.getId() == null) {
                order.setId(99); // Generate ID on first insert
            }
            return order;
        });

        // Act
        OrderResponseDTO response = orderService.createOrder(dto);

        // Assert
        assertNotNull(response);
        assertEquals(99, response.getOrderId());
        assertEquals("Alice", response.getCustomerName());

        // Expected billing calculations:
        // Initial gross total: 500.00
        // Greedy Coupon Selection picks Coupon 4 first (20% of 500 = 100 savings). New subtotal = 400.
        // Picks Coupon 1 next (Flat 50 savings). New subtotal = 350.
        // Total discount: 150.00.
        assertEquals(0, response.getTotalAmount().compareTo(BigDecimal.valueOf(500.00)));
        assertEquals(0, response.getDiscountAmount().compareTo(BigDecimal.valueOf(150.00)));

        // GST calculations:
        // Item A subtotal = 200 - 150 * (200 / 500) = 140. GST = 140 * 0.10 = 14.00
        // Item B subtotal = 300 - 150 * (300 / 500) = 210. GST = 210 * 0.05 = 10.50
        // Total GST = 24.50. Grand Total = 350 + 24.50 = 374.50
        assertEquals(0, response.getGstAmount().compareTo(BigDecimal.valueOf(24.50)));
        assertEquals(0, response.getGrandTotal().compareTo(BigDecimal.valueOf(374.50)));

        // Verify stock levels deducted correctly (10-2=8; 5-1=4)
        assertEquals(8, productA.getStockQuantity());
        assertEquals(4, productB.getStockQuantity());

        // Verify mocks saves occurred
        verify(productRepository, times(1)).save(productA);
        verify(productRepository, times(1)).save(productB);
        verify(couponRepository, times(1)).save(coupon1);
        verify(couponRepository, times(1)).save(coupon4);
        verify(couponRepository, never()).save(coupon2);
        verify(couponRepository, never()).save(coupon3);
        verify(stockAuditRepository, times(2)).save(any(StockAudit.class));
    }

    @Test
    public void testCreateOrder_ThrowsInsufficientStockException() {
        // Arrange
        Product product = Product.builder()
                .id(1)
                .name("Product A")
                .price(BigDecimal.valueOf(100.00))
                .stockQuantity(1) // only 1 unit in stock
                .build();

        OrderRequestDTO dto = OrderRequestDTO.builder()
                .customerName("Bob")
                .items(Collections.singletonList(
                        OrderItemRequestDTO.builder().productId(1).quantity(5).build() // Requesting 5 units (exceeds stock)
                ))
                .build();

        // Mock first save which happens at the start of createOrder()
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(productRepository.findById(1)).thenReturn(Optional.of(product));

        // Act & Assert
        // Assert that calling createOrder triggers InsufficientStockException
        assertThrows(InsufficientStockException.class, () -> orderService.createOrder(dto));

        // Verify that only the initial save is done, and no audits were persisted
        verify(orderRepository, times(1)).save(any(Order.class));
        verify(stockAuditRepository, never()).save(any(StockAudit.class));
    }
}
