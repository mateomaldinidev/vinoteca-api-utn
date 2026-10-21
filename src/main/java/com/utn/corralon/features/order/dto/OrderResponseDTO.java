package com.utn.corralon.features.order.dto;

import com.utn.corralon.features.order.enums.DeliveryType;
import com.utn.corralon.features.order.enums.OrderStatus;
import com.utn.corralon.features.orderItem.dto.OrderItemResponseDTO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder

public class OrderResponseDTO {
    @Schema(description = "Unique external identifier of the order", example = "550e8400-e29b-41d4-a716-446655440000")
    private UUID externalId;

    @Schema(description = "External identifier of the user who placed the order", example = "550e8400-e29b-41d4-a716-446655440001")
    private UUID userExternalId;

    @Schema(description = "External identifier of the delivery address", example = "550e8400-e29b-41d4-a716-446655440002")
    private UUID addressExternalId;

    @Schema(description = "Total amount of the order", example = "2500.50")
    private BigDecimal total;

    @Schema(description = "Order creation date and time", example = "2025-01-15T10:30:00")
    private LocalDateTime createdAt;

    @Schema(description = "Current order status", example = "PENDING")
    private OrderStatus status;

    @Schema(description = "Order delivery type", example = "DELIVERY")
    private DeliveryType deliveryType;

    @Schema(description = "List of order items")
    private List<OrderItemResponseDTO> items;
}
