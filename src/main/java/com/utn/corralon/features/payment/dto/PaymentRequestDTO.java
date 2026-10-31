package com.utn.corralon.features.payment.dto;

import com.utn.corralon.features.payment.entity.PaymentMethod;
import com.utn.corralon.features.payment.entity.PaymentStatus;
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
    private UUID orderId;

    @NotNull(message = "Payment method is required")
    private PaymentMethod paymentMethod;
}
