package com.utn.corralon.features.cart.dto;

import com.utn.corralon.features.cart_item.dto.CartItemRequestDTO;
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

    @Valid
    @NotEmpty(message = "Cart items list cannot be empty.")
    private List<CartItemRequestDTO> items;
}

