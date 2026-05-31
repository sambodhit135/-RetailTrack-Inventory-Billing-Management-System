package Retailtrack.retailtrack.service.impl;

import Retailtrack.retailtrack.dto.request.CategoryRequestDTO;
import Retailtrack.retailtrack.dto.response.CategoryResponseDTO;
import Retailtrack.retailtrack.entity.Category;
import Retailtrack.retailtrack.exception.ResourceNotFoundException;
import Retailtrack.retailtrack.repository.CategoryRepository;
import Retailtrack.retailtrack.service.CategoryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Implementation of {@link CategoryService} providing full CRUD operations
 * for the Category entity. Uses constructor-based dependency injection and
 * maps between DTOs and entities internally.
 */
@Slf4j
@Service
@Transactional
public class CategoryServiceImpl implements CategoryService {

    private final CategoryRepository categoryRepository;

    public CategoryServiceImpl(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    // ── Create ────────────────────────────────────────────────────────────────

    @Override
    public CategoryResponseDTO createCategory(CategoryRequestDTO request) {
        log.info("Creating new category with name: '{}'", request.getName());

        Category category = Category.builder()
                .name(request.getName())
                .gstSlab(request.getGstSlab())
                .description(request.getDescription())
                .build();

        Category saved = categoryRepository.save(category);
        log.info("Category created successfully with id: {}", saved.getId());

        return toResponseDTO(saved);
    }

    // ── Read ──────────────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public List<CategoryResponseDTO> getAllCategories() {
        log.info("Fetching all categories");

        List<CategoryResponseDTO> categories = categoryRepository.findAll()
                .stream()
                .map(this::toResponseDTO)
                .collect(Collectors.toList());

        log.info("Fetched {} categories successfully", categories.size());
        return categories;
    }

    @Override
    @Transactional(readOnly = true)
    public CategoryResponseDTO getCategoryById(Integer id) {
        log.info("Fetching category with id: {}", id);

        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category", "id", id));

        log.info("Category with id: {} fetched successfully", id);
        return toResponseDTO(category);
    }

    // ── Update ────────────────────────────────────────────────────────────────

    @Override
    public CategoryResponseDTO updateCategory(Integer id, CategoryRequestDTO request) {
        log.info("Updating category with id: {}", id);

        Category existing = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category", "id", id));

        existing.setName(request.getName());
        existing.setGstSlab(request.getGstSlab());
        existing.setDescription(request.getDescription());

        Category updated = categoryRepository.save(existing);
        log.info("Category with id: {} updated successfully", id);

        return toResponseDTO(updated);
    }

    // ── Delete ────────────────────────────────────────────────────────────────

    @Override
    public void deleteCategory(Integer id) {
        log.info("Deleting category with id: {}", id);

        Category existing = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category", "id", id));

        categoryRepository.delete(existing);
        log.info("Category with id: {} deleted successfully", id);
    }

    // ── Mapper ────────────────────────────────────────────────────────────────

    /**
     * Maps a {@link Category} entity to a {@link CategoryResponseDTO}.
     *
     * @param category the entity to map
     * @return the populated response DTO
     */
    private CategoryResponseDTO toResponseDTO(Category category) {
        return CategoryResponseDTO.builder()
                .id(category.getId())
                .name(category.getName())
                .gstSlab(category.getGstSlab())
                .description(category.getDescription())
                .createdAt(category.getCreatedAt())
                .build();
    }
}
