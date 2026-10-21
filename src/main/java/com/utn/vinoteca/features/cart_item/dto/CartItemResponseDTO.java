package com.utn.vinoteca.features.cart_item.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CartItemResponseDTO {
    @Schema(description = "Unique external identifier of the cart item", example = "550e8400-e29b-41d4-a716-446655440000")
    private UUID externalId;

    @Schema(description = "External identifier of the cart", example = "550e8400-e29b-41d4-a716-446655440001")
    private UUID cartExternalId;

    @Schema(description = "External identifier of the product variant", example = "550e8400-e29b-41d4-a716-446655440002")
    private UUID productVariantExternalId;

    @Schema(description = "Name of the product", example = "Cemento Portland 50kg") // <-- Nuevo
    private String productName; // <-- Nuevo

    @Schema(description = "Attribute of the product variant", example = "Bolsa 50kg") // <-- Nuevo
    private String variantAttribute; // <-- Nuevo

    @Schema(description = "Unit price of the product variant", example = "1500.00") // <-- Nuevo
    private BigDecimal unitPrice; // <-- Nuevo

    @Schema(description = "Quantity of the product variant in the cart", example = "3")
    private Integer quantity;

    @Schema(description = "Creation date and time of the cart item", example = "2025-01-15T10:30:00")
    private LocalDateTime createdAt;
}
