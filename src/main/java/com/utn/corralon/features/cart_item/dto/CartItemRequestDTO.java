package com.utn.corralon.features.cart_item.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.*;

import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class CartItemRequestDTO {
    @NotNull(message = "Product variant ID cannot be null")
    @Schema(description = "External identifier of the product variant", example = "550e8400-e29b-41d4-a716-446655440000")
    private UUID productVariantId;

    @NotNull(message = "Quantity cannot be null")
    @Min(value = 1, message = "Quantity must be at least 1")
    @Positive(message = "Quantity must be positive.")
    @Schema(description = "Quantity of the product variant in the cart", example = "3")
    private Integer quantity;


}
