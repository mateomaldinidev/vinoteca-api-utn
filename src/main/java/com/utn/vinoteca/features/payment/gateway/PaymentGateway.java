package com.utn.vinoteca.features.payment.gateway;

import com.utn.vinoteca.features.payment.dto.PaymentRequestDTO;
import com.utn.vinoteca.features.payment.entity.PaymentMethod;
import com.utn.vinoteca.features.payment.entity.PaymentStatus;

public interface PaymentGateway {

    boolean supports(PaymentMethod method);

    PaymentStatus charge(PaymentRequestDTO request);
}
