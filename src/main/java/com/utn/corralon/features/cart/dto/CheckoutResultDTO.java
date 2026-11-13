package com.utn.corralon.features.cart.dto;

import com.utn.corralon.features.order.dto.OrderResponseDTO;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class CheckoutResultDTO {
    private final OrderResponseDTO order;
    private final boolean replay;
}
