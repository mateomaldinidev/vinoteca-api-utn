package com.utn.vinoteca.integration;

import com.utn.vinoteca.features.order.dto.OrderResponseDTO;
import com.utn.vinoteca.features.order.entity.OrderEntity;
import com.utn.vinoteca.features.order.enums.OrderStatus;
import com.utn.vinoteca.features.payment.dto.PaymentRequestDTO;
import com.utn.vinoteca.features.payment.dto.PaymentResponseDTO;
import com.utn.vinoteca.features.payment.entity.PaymentEntity;
import com.utn.vinoteca.features.payment.enums.PaymentMethod;
import com.utn.vinoteca.features.payment.enums.PaymentStatus;
import com.utn.vinoteca.features.payment.service.CardPaymentSimulator;
import com.utn.vinoteca.features.user.entity.UserEntity;
import com.utn.vinoteca.features.wineVariant.entity.WineVariantEntity;
import com.utn.vinoteca.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

class PaymentApprovedTest extends AbstractIntegrationTest {

    @MockitoBean
    private CardPaymentSimulator cardPaymentSimulator;

    @Test
    void approvedPaymentMarksOrderAsPaidAndKeepsStockDiscounted() {
        when(cardPaymentSimulator.simulate(any())).thenReturn(PaymentStatus.APPROVED);

        UserEntity user = createUser();
        WineVariantEntity variant = createVariant(money("100.00"), 10, null, null);

        addToCart(user, variant, 2);
        OrderResponseDTO order = checkout(user);

        PaymentResponseDTO payment = paymentService.pay(PaymentRequestDTO.builder()
                .orderId(order.getExternalId())
                .paymentMethod(PaymentMethod.CREDIT_CARD)
                .amount(order.getTotal())
                .build());

        assertThat(payment.status()).isEqualTo(PaymentStatus.APPROVED);

        OrderEntity storedOrder = orderRepository.findByExternalId(order.getExternalId()).orElseThrow();
        assertThat(storedOrder.getStatus()).isEqualTo(OrderStatus.PAID);

        PaymentEntity storedPayment = paymentRepository.findByOrder_ExternalId(order.getExternalId()).orElseThrow();
        assertThat(storedPayment.getPaymentStatus()).isEqualTo(PaymentStatus.APPROVED);

        assertThat(variant.getStock()).isEqualTo(8);
    }
}
