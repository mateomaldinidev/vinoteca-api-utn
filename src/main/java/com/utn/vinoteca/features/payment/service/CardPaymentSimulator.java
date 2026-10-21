package com.utn.vinoteca.features.payment.service;

import com.utn.vinoteca.features.payment.dto.PaymentRequestDTO;
import com.utn.vinoteca.features.payment.entity.PaymentStatus;
import org.springframework.stereotype.Component;

@Component
public class CardPaymentSimulator {

    public PaymentStatus simulate(PaymentRequestDTO request) {
        return Math.random() > 0.3
                ? PaymentStatus.APPROVED
                : PaymentStatus.REJECTED;
    }
}
