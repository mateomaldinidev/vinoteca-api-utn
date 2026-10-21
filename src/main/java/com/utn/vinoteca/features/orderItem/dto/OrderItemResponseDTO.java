package com.utn.vinoteca.features.orderItem.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Setter
@Getter
@AllArgsConstructor
@RequiredArgsConstructor
@Builder
public class OrderItemResponseDTO {
    @Schema(description = "Unique external identifier of the order item", example = "550e8400-e29b-41d4-a716-446655440000")
    private UUID externalId;

    @Schema(description = "External identifier of the order", example = "550e8400-e29b-41d4-a716-446655440001")
    private UUID orderExternalId;

    @Schema(description = "External identifier of the wine variant", example = "550e8400-e29b-41d4-a716-446655440002")
    private UUID wineVariantExternalId;

    @Schema(description = "Quantity of the wine variant ordered", example = "5")
    private Integer quantity;

    @Schema(description = "Unit price of the wine variant at the time of the order", example = "1500.00")
    private BigDecimal unitPrice;

    @Schema(description = "Subtotal for this order item", example = "7500.00")
    private BigDecimal subtotal;

}
