package Retailtrack.retailtrack.controller;

import Retailtrack.retailtrack.dto.request.OrderRequestDTO;
import Retailtrack.retailtrack.dto.response.OrderResponseDTO;
import Retailtrack.retailtrack.service.OrderService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST Controller for Order and Billing management.
 * Exposes endpoints under {@code /api/orders}.
 */
@Slf4j
@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    // ── POST /api/orders ──────────────────────────────────────────────────────

    /**
     * Places a new checkout order.
     *
     * @param request validated order details payload
     * @return {@code 201 Created} with the placed order details DTO
     */
    @PostMapping
    public ResponseEntity<OrderResponseDTO> createOrder(
            @Valid @RequestBody OrderRequestDTO request) {
        log.info("REST: Received request to checkout order for customer: '{}'", request.getCustomerName());
        OrderResponseDTO response = orderService.createOrder(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // ── GET /api/orders/{id} ──────────────────────────────────────────────────

    /**
     * Retrieves details of a specific order by ID.
     *
     * @param id the order ID
     * @return {@code 200 OK} with the order details DTO, or {@code 404} if not found
     */
    @GetMapping("/{id}")
    public ResponseEntity<OrderResponseDTO> getOrderById(@PathVariable Integer id) {
        log.info("REST: Received request to retrieve order with ID: {}", id);
        OrderResponseDTO response = orderService.getOrderById(id);
        return ResponseEntity.ok(response);
    }

    // ── GET /api/orders ───────────────────────────────────────────────────────

    /**
     * Retrieves list of all placed orders.
     *
     * @return {@code 200 OK} with the list of all orders
     */
    @GetMapping
    public ResponseEntity<List<OrderResponseDTO>> getAllOrders() {
        log.info("REST: Received request to retrieve all orders");
        List<OrderResponseDTO> response = orderService.getAllOrders();
        return ResponseEntity.ok(response);
    }
}
