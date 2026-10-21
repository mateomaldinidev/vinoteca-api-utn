package com.utn.corralon.features.cart.dto;

import com.utn.corralon.features.cart_item.dto.CartItemResponseDTO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CartResponseDTO {
    @Schema(description = "Unique external identifier of the cart", example = "550e8400-e29b-41d4-a716-446655440000")
    private UUID externalId;

    @Schema(description = "External identifier of the user owning the cart", example = "550e8400-e29b-41d4-a716-446655440001")
    private UUID userExternalId;

    @Schema(description = "Last update date and time of the cart", example = "2025-01-15T10:30:00")
    private LocalDateTime lastUpdated;

    @Schema(description = "Total amount of the cart", example = "2500.50")
    private BigDecimal totalAmount;

    @Schema(description = "List of cart items")
    private List<CartItemResponseDTO> cartItems;

}
