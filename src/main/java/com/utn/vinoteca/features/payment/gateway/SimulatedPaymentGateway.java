package com.utn.vinoteca.features.payment.gateway;

import com.utn.vinoteca.features.payment.dto.PaymentRequestDTO;
import com.utn.vinoteca.features.payment.entity.PaymentMethod;
import com.utn.vinoteca.features.payment.entity.PaymentStatus;
import com.utn.vinoteca.features.payment.service.CardPaymentSimulator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SimulatedPaymentGateway implements PaymentGateway {

    private final CardPaymentSimulator cardPaymentSimulator;

    @Override
    public boolean supports(PaymentMethod method) {
        return true;
    }

    @Override
    public PaymentStatus charge(PaymentRequestDTO request) {
        return cardPaymentSimulator.simulate(request);
    }
}
