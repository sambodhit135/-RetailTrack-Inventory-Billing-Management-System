package Retailtrack.retailtrack.dto.request;

import jakarta.validation.constraints.*;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SupplierRequestDTO {

    @NotBlank(message = "Supplier name must not be blank")
    @Size(max = 150, message = "Supplier name must not exceed 150 characters")
    private String name;

    @NotBlank(message = "Supplier email must not be blank")
    @Email(message = "Supplier email must be a valid email address")
    @Size(max = 150, message = "Supplier email must not exceed 150 characters")
    private String email;

    @Size(max = 20, message = "Phone number must not exceed 20 characters")
    private String phone;

    private String address;

    /**
     * Number of days this supplier takes to deliver after an order is placed.
     * Used by the Reorder Engine to calculate when to trigger a reorder alert.
     */
    @NotNull(message = "Lead time days is required")
    @Min(value = 1, message = "Lead time days must be at least 1")
    private Integer leadTimeDays;
}
