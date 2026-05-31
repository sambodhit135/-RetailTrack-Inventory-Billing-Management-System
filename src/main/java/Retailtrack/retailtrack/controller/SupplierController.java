package Retailtrack.retailtrack.controller;

import Retailtrack.retailtrack.dto.request.SupplierRequestDTO;
import Retailtrack.retailtrack.dto.response.SupplierResponseDTO;
import Retailtrack.retailtrack.service.SupplierService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST Controller for Supplier management.
 * Exposes CRUD endpoints under {@code /api/suppliers}.
 */
@RestController
@RequestMapping("/api/suppliers")
public class SupplierController {

    private final SupplierService supplierService;

    public SupplierController(SupplierService supplierService) {
        this.supplierService = supplierService;
    }

    // ── POST /api/suppliers ───────────────────────────────────────────────────

    /**
     * Creates a new supplier.
     *
     * @param request validated request payload
     * @return {@code 201 Created} with the persisted supplier DTO
     */
    @PostMapping
    public ResponseEntity<SupplierResponseDTO> createSupplier(
            @Valid @RequestBody SupplierRequestDTO request) {
        SupplierResponseDTO created = supplierService.createSupplier(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    // ── GET /api/suppliers ────────────────────────────────────────────────────

    /**
     * Retrieves all suppliers.
     *
     * @return {@code 200 OK} with the list of all suppliers
     */
    @GetMapping
    public ResponseEntity<List<SupplierResponseDTO>> getAllSuppliers() {
        return ResponseEntity.ok(supplierService.getAllSuppliers());
    }

    // ── GET /api/suppliers/{id} ───────────────────────────────────────────────

    /**
     * Retrieves a single supplier by its ID.
     *
     * @param id the supplier identifier
     * @return {@code 200 OK} with the matching supplier, or {@code 404} if not found
     */
    @GetMapping("/{id}")
    public ResponseEntity<SupplierResponseDTO> getSupplierById(@PathVariable Integer id) {
        return ResponseEntity.ok(supplierService.getSupplierById(id));
    }

    // ── PUT /api/suppliers/{id} ───────────────────────────────────────────────

    /**
     * Updates an existing supplier.
     *
     * @param id      the supplier identifier
     * @param request validated update payload
     * @return {@code 200 OK} with the updated supplier, or {@code 404} if not found
     */
    @PutMapping("/{id}")
    public ResponseEntity<SupplierResponseDTO> updateSupplier(
            @PathVariable Integer id,
            @Valid @RequestBody SupplierRequestDTO request) {
        return ResponseEntity.ok(supplierService.updateSupplier(id, request));
    }

    // ── DELETE /api/suppliers/{id} ────────────────────────────────────────────

    /**
     * Deletes a supplier by its ID.
     *
     * @param id the supplier identifier
     * @return {@code 204 No Content} on success, or {@code 404} if not found
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteSupplier(@PathVariable Integer id) {
        supplierService.deleteSupplier(id);
        return ResponseEntity.noContent().build();
    }
}
