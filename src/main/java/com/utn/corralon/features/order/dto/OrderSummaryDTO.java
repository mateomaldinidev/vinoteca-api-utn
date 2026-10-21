package com.utn.corralon.features.order.dto;

import com.utn.corralon.features.order.enums.OrderStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
public class OrderSummaryDTO {

    @Schema(description = "Unique external identifier of the order", example = "550e8400-e29b-41d4-a716-446655440000")
    private UUID externalId;

    @Schema(description = "Total amount of the order", example = "2500.50")
    private BigDecimal total;

    @Schema(description = "Current order status", example = "PENDING")
    private OrderStatus status;

    @Schema(description = "Order creation date and time", example = "2025-01-15T10:30:00")
    private LocalDateTime createdAt;
}
