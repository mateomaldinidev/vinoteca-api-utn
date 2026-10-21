package com.utn.vinoteca.features.stockMovement.dto;

import com.utn.vinoteca.features.stockMovement.enums.StockMovementType;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.UUID;

public record StockMovementResponseDTO(
    @Schema(description = "Unique external identifier of the stock movement", example = "550e8400-e29b-41d4-a716-446655440000")
    UUID externalId,
    @Schema(description = "Type of stock movement", example = "ENTRY")
    StockMovementType type,
    @Schema(description = "Quantity of stock moved", example = "50")
    Integer quantity,
    @Schema(description = "Date and time of the stock movement", example = "2025-01-15T10:30:00")
    LocalDateTime movementDate,
    @Schema(description = "Reason for the stock movement", example = "Ajuste por inventario mensual")
    String reason,
    @Schema(description = "External identifier of the product variant", example = "550e8400-e29b-41d4-a716-446655440001")
    UUID variantExternalId
) {}
