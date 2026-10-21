package com.utn.vinoteca.features.payment.mapper;

import com.utn.vinoteca.features.payment.dto.PaymentResponseDTO;
import com.utn.vinoteca.features.payment.entity.PaymentEntity;
import jakarta.validation.constraints.NotNull;
import org.springframework.stereotype.Component;

@Component
public class PaymentMapper {
    public PaymentResponseDTO toDTO(@NotNull PaymentEntity payment){
        return new PaymentResponseDTO(
                payment.getExternalId(),
                payment.getOrder().getExternalId(),
                payment.getPaymentMethod(),
                payment.getAmount(),
                payment.getPaymentStatus(),
                payment.getCreatedAt()

        );
    }
}
