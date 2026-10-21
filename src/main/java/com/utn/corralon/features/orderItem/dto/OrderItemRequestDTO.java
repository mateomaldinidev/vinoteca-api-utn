package com.utn.corralon.features.orderItem.dto;

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

    @NotNull(message = "Product variant external ID is required")
    @Schema(description = "External identifier of the product variant", example = "550e8400-e29b-41d4-a716-446655440000")
    private UUID productVariantExternalId;

    @NotNull(message = "Quantity is required")
    @Min(value = 1, message = "Quantity must be at least 1")
    @Schema(description = "Quantity of the product variant ordered", example = "5")
    private Integer quantity;
}
