package com.utn.vinoteca.features.cart.dto;

import com.utn.vinoteca.features.cartItem.dto.CartItemRequestDTO;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.List;
import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class CartRequestDTO {
    @NotNull(message = "User ID cannot be null")
    @Schema(description = "External identifier of the user owning the cart", example = "550e8400-e29b-41d4-a716-446655440000")
    private UUID userId;


    @Valid // Asegura que cada CartItemRequestDTO en la lista también sea validado
    @NotEmpty(message = "Cart items list cannot be empty.") // tambien que no sea null
    @Schema(description = "List of cart items")
    private List<CartItemRequestDTO> items;
}
