package com.utn.vinoteca.features.payment.gateway;

import com.utn.vinoteca.exception.BusinessRuleException;
import com.utn.vinoteca.features.payment.enums.PaymentMethod;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class PaymentGatewayRegistry {

    private final List<PaymentGateway> gateways;

    public PaymentGateway forMethod(PaymentMethod method) {
        return gateways.stream()
                .filter(gateway -> gateway.supports(method))
                .findFirst()
                .orElseThrow(() -> new BusinessRuleException(
                        "Unsupported payment method: " + method));
    }
}
