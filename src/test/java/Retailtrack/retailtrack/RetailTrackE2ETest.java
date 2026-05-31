package Retailtrack.retailtrack;

import Retailtrack.retailtrack.dto.request.CategoryRequestDTO;
import Retailtrack.retailtrack.dto.request.CouponRequestDTO;
import Retailtrack.retailtrack.dto.request.OrderItemRequestDTO;
import Retailtrack.retailtrack.dto.request.OrderRequestDTO;
import Retailtrack.retailtrack.dto.request.ProductRequestDTO;
import Retailtrack.retailtrack.dto.response.CategoryResponseDTO;
import Retailtrack.retailtrack.dto.response.CouponResponseDTO;
import Retailtrack.retailtrack.dto.response.InvoiceResponseDTO;
import Retailtrack.retailtrack.dto.response.OrderResponseDTO;
import Retailtrack.retailtrack.dto.response.ProductResponseDTO;
import Retailtrack.retailtrack.entity.enums.DiscountType;
import Retailtrack.retailtrack.repository.CouponRepository;
import Retailtrack.retailtrack.service.CategoryService;
import Retailtrack.retailtrack.service.CouponService;
import Retailtrack.retailtrack.service.InvoiceService;
import Retailtrack.retailtrack.service.OrderService;
import Retailtrack.retailtrack.service.ProductService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
public class RetailTrackE2ETest {

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private ProductService productService;

    @Autowired
    private CouponService couponService;

    @Autowired
    private OrderService orderService;

    @Autowired
    private InvoiceService invoiceService;

    @Autowired
    private CouponRepository couponRepository;

    @Test
    public void testCompleteCheckoutAndInvoicingFlow() {
        // Clear all existing coupons to prevent Greedy Billing Engine from selecting additional active coupons.
        couponRepository.deleteAll();

        // Step 1: Create and save a new Category and a new Product with 50 units of stock.
        CategoryRequestDTO categoryRequest = CategoryRequestDTO.builder()
                .name("E2E Category")
                .gstSlab(BigDecimal.valueOf(18.00))
                .description("Category for E2E integration test")
                .build();
        CategoryResponseDTO categoryResponse = categoryService.createCategory(categoryRequest);
        assertNotNull(categoryResponse);
        assertNotNull(categoryResponse.getId());

        ProductRequestDTO productRequest = ProductRequestDTO.builder()
                .name("E2E Test Product")
                .description("Product for E2E integration test")
                .price(BigDecimal.valueOf(200.00))
                .stockQuantity(50)
                .reorderThreshold(10)
                .avgDailySales(BigDecimal.valueOf(2.5))
                .categoryId(categoryResponse.getId())
                .build();
        ProductResponseDTO productResponse = productService.createProduct(productRequest);
        assertNotNull(productResponse);
        assertNotNull(productResponse.getId());

        // Step 2: Create and save a new active Coupon (e.g., 10% off).
        CouponRequestDTO couponRequest = CouponRequestDTO.builder()
                .code("E2ETENPERCENT")
                .discountType(DiscountType.PERCENTAGE)
                .discountValue(BigDecimal.valueOf(10.00))
                .minCartValue(BigDecimal.valueOf(100.00))
                .maxUses(5)
                .expiryDate(LocalDate.now().plusDays(10))
                .isActive(true)
                .build();
        CouponResponseDTO couponResponse = couponService.createCoupon(couponRequest);
        assertNotNull(couponResponse);
        assertNotNull(couponResponse.getId());

        // Step 3: Create an OrderRequestDTO purchasing 5 units of the product and pass the coupon code.
        OrderItemRequestDTO itemRequest = OrderItemRequestDTO.builder()
                .productId(productResponse.getId())
                .quantity(5)
                .build();

        OrderRequestDTO orderRequest = OrderRequestDTO.builder()
                .customerName("Jane Doe")
                .couponCode("E2ETENPERCENT")
                .items(Collections.singletonList(itemRequest))
                .build();

        // Step 4: Call orderService.createOrder() and assert that the returned OrderResponseDTO has
        // the correctly calculated discount applied and a status of PENDING (initial checkout state).
        OrderResponseDTO orderResponse = orderService.createOrder(orderRequest);
        assertNotNull(orderResponse);
        assertNotNull(orderResponse.getOrderId());
        assertEquals("PENDING", orderResponse.getStatus());
        assertEquals(0, orderResponse.getTotalAmount().compareTo(BigDecimal.valueOf(1000.00)));
        assertEquals(0, orderResponse.getDiscountAmount().compareTo(BigDecimal.valueOf(100.00)));
        assertEquals(0, orderResponse.getGstAmount().compareTo(BigDecimal.valueOf(162.00)));
        assertEquals(0, orderResponse.getGrandTotal().compareTo(BigDecimal.valueOf(1062.00)));

        // Step 5: Call invoiceService.generateInvoiceForOrder() using the new Order ID.
        InvoiceResponseDTO invoiceResponse = invoiceService.generateInvoiceForOrder(orderResponse.getOrderId());

        // Step 6: Assert that the InvoiceResponseDTO is not null, contains a generated invoice number,
        // and perfectly matches the order's grand total. Also verify the order's status transitions to COMPLETED.
        assertNotNull(invoiceResponse);
        assertNotNull(invoiceResponse.getInvoiceId());
        assertNotNull(invoiceResponse.getInvoiceNumber());
        assertTrue(invoiceResponse.getInvoiceNumber().startsWith("INV-"));
        assertEquals(orderResponse.getOrderId(), invoiceResponse.getOrderId());
        assertEquals(0, invoiceResponse.getGrandTotal().compareTo(orderResponse.getGrandTotal()));

        // Verify the order's status has transitioned to COMPLETED
        OrderResponseDTO updatedOrder = orderService.getOrderById(orderResponse.getOrderId());
        assertEquals("COMPLETED", updatedOrder.getStatus());
    }
}
