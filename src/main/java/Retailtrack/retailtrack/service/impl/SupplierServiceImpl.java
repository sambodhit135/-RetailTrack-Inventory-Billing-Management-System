package Retailtrack.retailtrack.service.impl;

import Retailtrack.retailtrack.dto.request.SupplierRequestDTO;
import Retailtrack.retailtrack.dto.response.SupplierResponseDTO;
import Retailtrack.retailtrack.entity.Supplier;
import Retailtrack.retailtrack.exception.ResourceNotFoundException;
import Retailtrack.retailtrack.repository.SupplierRepository;
import Retailtrack.retailtrack.service.SupplierService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Implementation of {@link SupplierService} providing full CRUD operations
 * for the Supplier entity. Uses constructor-based dependency injection and
 * maps between DTOs and entities internally.
 */
@Slf4j
@Service
@Transactional
public class SupplierServiceImpl implements SupplierService {

    private final SupplierRepository supplierRepository;

    public SupplierServiceImpl(SupplierRepository supplierRepository) {
        this.supplierRepository = supplierRepository;
    }

    // ── Create ────────────────────────────────────────────────────────────────

    @Override
    public SupplierResponseDTO createSupplier(SupplierRequestDTO request) {
        log.info("Creating new supplier with name: '{}' and email: '{}'",
                request.getName(), request.getEmail());

        Supplier supplier = Supplier.builder()
                .name(request.getName())
                .email(request.getEmail())
                .phone(request.getPhone())
                .address(request.getAddress())
                .leadTimeDays(request.getLeadTimeDays())
                .build();

        Supplier saved = supplierRepository.save(supplier);
        log.info("Supplier created successfully with id: {}", saved.getId());

        return toResponseDTO(saved);
    }

    // ── Read ──────────────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public List<SupplierResponseDTO> getAllSuppliers() {
        log.info("Fetching all suppliers");

        List<SupplierResponseDTO> suppliers = supplierRepository.findAll()
                .stream()
                .map(this::toResponseDTO)
                .collect(Collectors.toList());

        log.info("Fetched {} suppliers successfully", suppliers.size());
        return suppliers;
    }

    @Override
    @Transactional(readOnly = true)
    public SupplierResponseDTO getSupplierById(Integer id) {
        log.info("Fetching supplier with id: {}", id);

        Supplier supplier = supplierRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Supplier", "id", id));

        log.info("Supplier with id: {} fetched successfully", id);
        return toResponseDTO(supplier);
    }

    // ── Update ────────────────────────────────────────────────────────────────

    @Override
    public SupplierResponseDTO updateSupplier(Integer id, SupplierRequestDTO request) {
        log.info("Updating supplier with id: {}", id);

        Supplier existing = supplierRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Supplier", "id", id));

        existing.setName(request.getName());
        existing.setEmail(request.getEmail());
        existing.setPhone(request.getPhone());
        existing.setAddress(request.getAddress());
        existing.setLeadTimeDays(request.getLeadTimeDays());

        Supplier updated = supplierRepository.save(existing);
        log.info("Supplier with id: {} updated successfully", id);

        return toResponseDTO(updated);
    }

    // ── Delete ────────────────────────────────────────────────────────────────

    @Override
    public void deleteSupplier(Integer id) {
        log.info("Deleting supplier with id: {}", id);

        Supplier existing = supplierRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Supplier", "id", id));

        supplierRepository.delete(existing);
        log.info("Supplier with id: {} deleted successfully", id);
    }

    // ── Mapper ────────────────────────────────────────────────────────────────

    /**
     * Maps a {@link Supplier} entity to a {@link SupplierResponseDTO}.
     *
     * @param supplier the entity to map
     * @return the populated response DTO
     */
    private SupplierResponseDTO toResponseDTO(Supplier supplier) {
        return SupplierResponseDTO.builder()
                .id(supplier.getId())
                .name(supplier.getName())
                .email(supplier.getEmail())
                .phone(supplier.getPhone())
                .address(supplier.getAddress())
                .leadTimeDays(supplier.getLeadTimeDays())
                .createdAt(supplier.getCreatedAt())
                .build();
    }
}
