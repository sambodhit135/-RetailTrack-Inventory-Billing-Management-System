package Retailtrack.retailtrack.service;

import Retailtrack.retailtrack.dto.request.OrderRequestDTO;
import Retailtrack.retailtrack.dto.response.OrderResponseDTO;

import java.util.List;

/**
 * Service contract for Order and Billing operations.
 */
public interface OrderService {

    /**
     * Creates/Places a new order.
     * Transactional: validates items, checks and deducts stock, computes taxes and persists the order.
     *
     * @param dto validated order request details DTO
     * @return the created order response DTO
     */
    OrderResponseDTO createOrder(OrderRequestDTO dto);

    /**
     * Retrieves an order by its ID.
     *
     * @param id the order ID
     * @return the matching order details as a response DTO
     * @throws Retailtrack.retailtrack.exception.ResourceNotFoundException if order does not exist
     */
    OrderResponseDTO getOrderById(Integer id);

    /**
     * Retrieves all orders in the system.
     *
     * @return list of all order response DTOs
     */
    List<OrderResponseDTO> getAllOrders();
}
