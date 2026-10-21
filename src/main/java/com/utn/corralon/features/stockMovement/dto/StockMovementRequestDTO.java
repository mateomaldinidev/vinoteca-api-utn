package com.utn.corralon.features.stockMovement.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class StockMovementRequestDTO {

    @NotNull(message = "The quantity cannot be null.")
    @Schema(description = "Quantity of stock moved", example = "50")
    private Integer quantity;

    @NotBlank
    @Size(min = 10, max = 500, message = "The reason lenth is min = 10, max = 500.")
    @Schema(description = "Reason for the stock movement", example = "Ajuste por inventario mensual")
    private String reason;

    @NotNull(message = "The variantId cannot be null.")
    @Schema(description = "External identifier of the product variant", example = "550e8400-e29b-41d4-a716-446655440000")
    private UUID variantId;
}
