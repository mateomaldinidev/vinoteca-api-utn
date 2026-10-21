package com.utn.vinoteca.features.orderItem.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.*;

import java.util.UUID;

@Setter
@Getter
@AllArgsConstructor
@RequiredArgsConstructor
@Builder
public class OrderItemRequestDTO {

    @NotNull(message = "Wine variant external ID is required")
    @Schema(description = "External identifier of the wine variant", example = "550e8400-e29b-41d4-a716-446655440000")
    private UUID wineVariantExternalId;

    @NotNull(message = "Quantity is required")
    @Min(value = 1, message = "Quantity must be at least 1")
    @Schema(description = "Quantity of the wine variant ordered", example = "5")
    private Integer quantity;
}
