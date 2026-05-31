package Retailtrack.retailtrack.service.impl;

import Retailtrack.retailtrack.dto.ReorderItemPriorityDTO;
import Retailtrack.retailtrack.entity.Product;
import Retailtrack.retailtrack.entity.ProductSupplier;
import Retailtrack.retailtrack.entity.Supplier;
import Retailtrack.retailtrack.repository.ProductRepository;
import Retailtrack.retailtrack.repository.ProductSupplierRepository;
import Retailtrack.retailtrack.repository.ReorderRequestRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit test suite for the ReorderEngineServiceImpl checking Priority scores calculations,
 * Min-Heap heap ordering, and exception/division-by-zero safety.
 */
@ExtendWith(MockitoExtension.class)
public class ReorderEngineServiceImplTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private ProductSupplierRepository productSupplierRepository;

    @Mock
    private ReorderRequestRepository reorderRequestRepository;

    @InjectMocks
    private ReorderEngineServiceImpl reorderEngineService;

    @Test
    public void testGetTopLowStockProducts_MinHeapOrdering() {
        // Arrange
        // Product A: stock = 2, threshold = 10, sales = 2.0, leadTime = 2 days
        // Denominator = 2.0 * 2 = 4.0; Score = (2 - 10) / 4.0 = -2.0
        Product productA = Product.builder()
                .id(1)
                .name("Product A")
                .stockQuantity(2)
                .reorderThreshold(10)
                .avgDailySales(BigDecimal.valueOf(2.0))
                .build();
        Supplier supplierA = Supplier.builder().id(10).leadTimeDays(2).build();
        ProductSupplier psA = ProductSupplier.builder().product(productA).supplier(supplierA).isPrimary(true).build();

        // Product B: stock = 5, threshold = 10, sales = 1.0, leadTime = 5 days
        // Denominator = 1.0 * 5 = 5.0; Score = (5 - 10) / 5.0 = -1.0
        Product productB = Product.builder()
                .id(2)
                .name("Product B")
                .stockQuantity(5)
                .reorderThreshold(10)
                .avgDailySales(BigDecimal.valueOf(1.0))
                .build();
        Supplier supplierB = Supplier.builder().id(11).leadTimeDays(5).build();
        ProductSupplier psB = ProductSupplier.builder().product(productB).supplier(supplierB).isPrimary(true).build();

        // Product C: stock = 0, threshold = 15, sales = 5.0, leadTime = 1 day
        // Denominator = 5.0 * 1 = 5.0; Score = (0 - 15) / 5.0 = -3.0
        Product productC = Product.builder()
                .id(3)
                .name("Product C")
                .stockQuantity(0)
                .reorderThreshold(15)
                .avgDailySales(BigDecimal.valueOf(5.0))
                .build();
        Supplier supplierC = Supplier.builder().id(12).leadTimeDays(1).build();
        ProductSupplier psC = ProductSupplier.builder().product(productC).supplier(supplierC).isPrimary(true).build();

        when(productRepository.findProductsBelowReorderThreshold()).thenReturn(Arrays.asList(productA, productB, productC));
        when(productSupplierRepository.findByProductIdAndIsPrimaryTrue(1)).thenReturn(Optional.of(psA));
        when(productSupplierRepository.findByProductIdAndIsPrimaryTrue(2)).thenReturn(Optional.of(psB));
        when(productSupplierRepository.findByProductIdAndIsPrimaryTrue(3)).thenReturn(Optional.of(psC));

        // Act
        // Request top 3 prioritized elements
        List<ReorderItemPriorityDTO> result = reorderEngineService.getTopLowStockProducts(3);

        // Assert
        assertNotNull(result);
        assertEquals(3, result.size());

        // Expect priority ordering: C (-3.0) -> A (-2.0) -> B (-1.0)
        assertEquals(3, result.get(0).getProductId()); // Product C
        assertEquals("Product C", result.get(0).getProductName());
        assertEquals(-3.0, result.get(0).getPriorityScore());

        assertEquals(1, result.get(1).getProductId()); // Product A
        assertEquals("Product A", result.get(1).getProductName());
        assertEquals(-2.0, result.get(1).getPriorityScore());

        assertEquals(2, result.get(2).getProductId()); // Product B
        assertEquals("Product B", result.get(2).getProductName());
        assertEquals(-1.0, result.get(2).getPriorityScore());

        verify(productRepository, times(1)).findProductsBelowReorderThreshold();
        verify(productSupplierRepository, times(3)).findByProductIdAndIsPrimaryTrue(anyInt());
    }

    @Test
    public void testGetTopLowStockProducts_DivisionByZeroSafety() {
        // Arrange
        // Product D: stock = 1, threshold = 10, sales = 0.0, leadTime = 3 days
        // Denominator = 0.0 * 3 = 0.0 -> Score should default to Double.MAX_VALUE (division-by-zero protection)
        Product productD = Product.builder()
                .id(4)
                .name("Product D")
                .stockQuantity(1)
                .reorderThreshold(10)
                .avgDailySales(BigDecimal.ZERO)
                .build();
        Supplier supplierD = Supplier.builder().id(13).leadTimeDays(3).build();
        ProductSupplier psD = ProductSupplier.builder().product(productD).supplier(supplierD).isPrimary(true).build();

        when(productRepository.findProductsBelowReorderThreshold()).thenReturn(Collections.singletonList(productD));
        when(productSupplierRepository.findByProductIdAndIsPrimaryTrue(4)).thenReturn(Optional.of(psD));

        // Act
        List<ReorderItemPriorityDTO> result = reorderEngineService.getTopLowStockProducts(1);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(4, result.get(0).getProductId());
        assertEquals(Double.MAX_VALUE, result.get(0).getPriorityScore());

        verify(productRepository, times(1)).findProductsBelowReorderThreshold();
        verify(productSupplierRepository, times(1)).findByProductIdAndIsPrimaryTrue(4);
    }
}
