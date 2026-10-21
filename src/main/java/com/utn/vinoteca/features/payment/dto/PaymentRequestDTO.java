package com.utn.vinoteca.features.payment.dto;

import com.utn.vinoteca.features.payment.enums.PaymentMethod;
import com.utn.vinoteca.features.payment.enums.PaymentStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class PaymentRequestDTO {
    @NotNull(message = "Order ID is required")
    @Schema(description = "External identifier of the order being paid", example = "550e8400-e29b-41d4-a716-446655440000")

    private UUID orderId;
    @NotNull(message = "Payment method is required")
    @Schema(description = "Payment method used", example = "CREDIT_CARD")
    private PaymentMethod paymentMethod;

    @NotNull(message = "Amount is required")
    @Positive(message = "Amount must be greater than 0")
    @Schema(description = "Payment amount", example = "2500.50")
    private BigDecimal amount;
}
