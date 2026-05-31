package Retailtrack.retailtrack.controller;

import Retailtrack.retailtrack.dto.request.CategoryRequestDTO;
import Retailtrack.retailtrack.dto.response.CategoryResponseDTO;
import Retailtrack.retailtrack.service.CategoryService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST Controller for Category management.
 * Exposes CRUD endpoints under {@code /api/categories}.
 */
@RestController
@RequestMapping("/api/categories")
public class CategoryController {

    private final CategoryService categoryService;

    public CategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    // ── POST /api/categories ──────────────────────────────────────────────────

    /**
     * Creates a new category.
     *
     * @param request validated request payload
     * @return {@code 201 Created} with the persisted category DTO
     */
    @PostMapping
    public ResponseEntity<CategoryResponseDTO> createCategory(
            @Valid @RequestBody CategoryRequestDTO request) {
        CategoryResponseDTO created = categoryService.createCategory(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    // ── GET /api/categories ───────────────────────────────────────────────────

    /**
     * Retrieves all categories.
     *
     * @return {@code 200 OK} with the list of all categories
     */
    @GetMapping
    public ResponseEntity<List<CategoryResponseDTO>> getAllCategories() {
        return ResponseEntity.ok(categoryService.getAllCategories());
    }

    // ── GET /api/categories/{id} ──────────────────────────────────────────────

    /**
     * Retrieves a single category by its ID.
     *
     * @param id the category identifier
     * @return {@code 200 OK} with the matching category, or {@code 404} if not found
     */
    @GetMapping("/{id}")
    public ResponseEntity<CategoryResponseDTO> getCategoryById(@PathVariable Integer id) {
        return ResponseEntity.ok(categoryService.getCategoryById(id));
    }

    // ── PUT /api/categories/{id} ──────────────────────────────────────────────

    /**
     * Updates an existing category.
     *
     * @param id      the category identifier
     * @param request validated update payload
     * @return {@code 200 OK} with the updated category, or {@code 404} if not found
     */
    @PutMapping("/{id}")
    public ResponseEntity<CategoryResponseDTO> updateCategory(
            @PathVariable Integer id,
            @Valid @RequestBody CategoryRequestDTO request) {
        return ResponseEntity.ok(categoryService.updateCategory(id, request));
    }

    // ── DELETE /api/categories/{id} ───────────────────────────────────────────

    /**
     * Deletes a category by its ID.
     *
     * @param id the category identifier
     * @return {@code 204 No Content} on success, or {@code 404} if not found
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCategory(@PathVariable Integer id) {
        categoryService.deleteCategory(id);
        return ResponseEntity.noContent().build();
    }
}
