package Retailtrack.retailtrack.service;

import Retailtrack.retailtrack.dto.request.CategoryRequestDTO;
import Retailtrack.retailtrack.dto.response.CategoryResponseDTO;

import java.util.List;

/**
 * Service contract for Category management operations.
 * All methods operate on DTOs; entity mapping is handled in the implementation layer.
 */
public interface CategoryService {

    /**
     * Creates a new category from the given request payload.
     *
     * @param request DTO containing category details
     * @return the persisted category as a response DTO
     */
    CategoryResponseDTO createCategory(CategoryRequestDTO request);

    /**
     * Retrieves all categories in the system.
     *
     * @return list of all category response DTOs
     */
    List<CategoryResponseDTO> getAllCategories();

    /**
     * Retrieves a single category by its primary key.
     *
     * @param id the category ID
     * @return the matching category as a response DTO
     * @throws Retailtrack.retailtrack.exception.ResourceNotFoundException if not found
     */
    CategoryResponseDTO getCategoryById(Integer id);

    /**
     * Updates an existing category with the given payload.
     *
     * @param id      the ID of the category to update
     * @param request DTO containing updated values
     * @return the updated category as a response DTO
     * @throws Retailtrack.retailtrack.exception.ResourceNotFoundException if not found
     */
    CategoryResponseDTO updateCategory(Integer id, CategoryRequestDTO request);

    /**
     * Deletes a category by its primary key.
     *
     * @param id the ID of the category to delete
     * @throws Retailtrack.retailtrack.exception.ResourceNotFoundException if not found
     */
    void deleteCategory(Integer id);
}
