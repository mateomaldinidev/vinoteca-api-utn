package com.utn.vinoteca.features.payment.service;


import com.utn.vinoteca.features.payment.dto.PaymentRequestDTO;
import com.utn.vinoteca.features.payment.dto.PaymentResponseDTO;

import java.util.UUID;

public interface IPaymentService {

    PaymentResponseDTO pay(PaymentRequestDTO request);

    PaymentResponseDTO getByOrderId(UUID orderId);
}