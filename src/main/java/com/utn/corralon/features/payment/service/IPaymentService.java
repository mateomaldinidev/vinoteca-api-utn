package com.utn.corralon.features.payment.service;


import com.utn.corralon.features.payment.dto.PaymentRequestDTO;
import com.utn.corralon.features.payment.dto.PaymentResponseDTO;

import java.util.UUID;

public interface IPaymentService {

    PaymentResponseDTO pay(
            PaymentRequestDTO request,
            String username,
            boolean isAdmin
    );

    PaymentResponseDTO getByOrderId(
            UUID orderId,
            String username,
            boolean isAdmin
    );
}