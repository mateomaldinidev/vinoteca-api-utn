package com.utn.vinoteca.features.order.dto;


import com.utn.vinoteca.features.order.enums.DeliveryType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.UUID;

@Setter
@Getter
@RequiredArgsConstructor
@AllArgsConstructor
@Builder
public class OrderRequestDTO {

    @NotNull(message = "User external ID is required")
    @Schema(description = "External identifier of the user placing the order", example = "550e8400-e29b-41d4-a716-446655440000")
    private UUID userExternalId;

    @NotNull(message = "Delivery type is required")
    @Schema(description = "Order delivery type", example = "DELIVERY")
    private DeliveryType deliveryType;

    // opcional según deliveryType
    @Schema(description = "External identifier of the delivery address", example = "550e8400-e29b-41d4-a716-446655440001")
    private UUID addressExternalId;
}
