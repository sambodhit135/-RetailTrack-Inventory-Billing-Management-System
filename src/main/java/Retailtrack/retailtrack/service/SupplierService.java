package Retailtrack.retailtrack.service;

import Retailtrack.retailtrack.dto.request.SupplierRequestDTO;
import Retailtrack.retailtrack.dto.response.SupplierResponseDTO;

import java.util.List;

/**
 * Service contract for Supplier management operations.
 * All methods operate on DTOs; entity mapping is handled in the implementation layer.
 */
public interface SupplierService {

    /**
     * Creates a new supplier from the given request payload.
     *
     * @param request DTO containing supplier details
     * @return the persisted supplier as a response DTO
     */
    SupplierResponseDTO createSupplier(SupplierRequestDTO request);

    /**
     * Retrieves all suppliers in the system.
     *
     * @return list of all supplier response DTOs
     */
    List<SupplierResponseDTO> getAllSuppliers();

    /**
     * Retrieves a single supplier by its primary key.
     *
     * @param id the supplier ID
     * @return the matching supplier as a response DTO
     * @throws Retailtrack.retailtrack.exception.ResourceNotFoundException if not found
     */
    SupplierResponseDTO getSupplierById(Integer id);

    /**
     * Updates an existing supplier with the given payload.
     *
     * @param id      the ID of the supplier to update
     * @param request DTO containing updated values
     * @return the updated supplier as a response DTO
     * @throws Retailtrack.retailtrack.exception.ResourceNotFoundException if not found
     */
    SupplierResponseDTO updateSupplier(Integer id, SupplierRequestDTO request);

    /**
     * Deletes a supplier by its primary key.
     *
     * @param id the ID of the supplier to delete
     * @throws Retailtrack.retailtrack.exception.ResourceNotFoundException if not found
     */
    void deleteSupplier(Integer id);
}
