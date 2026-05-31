package Retailtrack.retailtrack.dto.response;

import lombok.*;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SupplierResponseDTO {

    private Integer id;

    private String name;

    private String email;

    private String phone;

    private String address;

    /**
     * Number of days this supplier takes to deliver after an order is placed.
     * Used by the Reorder Engine to calculate when to trigger a reorder alert.
     */
    private Integer leadTimeDays;

    private LocalDateTime createdAt;
}
