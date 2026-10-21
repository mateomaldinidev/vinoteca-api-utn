package com.utn.vinoteca.features.payment.dto;

import com.utn.vinoteca.features.payment.enums.PaymentMethod;
import com.utn.vinoteca.features.payment.enums.PaymentStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record PaymentResponseDTO(
        @Schema(description = "Unique external identifier of the payment", example = "550e8400-e29b-41d4-a716-446655440000")
        UUID externalId,
        @Schema(description = "External identifier of the order being paid", example = "550e8400-e29b-41d4-a716-446655440001")
        UUID orderExternalId,
        @Schema(description = "Payment method used", example = "CREDIT_CARD")
        PaymentMethod paymentMethod,
        @Schema(description = "Payment amount", example = "2500.50")
        BigDecimal amount,
        @Schema(description = "Current payment status", example = "APPROVED")
        PaymentStatus status,
        @Schema(description = "Payment creation date and time", example = "2025-01-15T10:30:00")
        LocalDateTime createdAt
) {
}
