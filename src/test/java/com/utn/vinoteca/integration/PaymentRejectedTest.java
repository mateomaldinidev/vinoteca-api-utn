package com.utn.vinoteca.integration;

import com.utn.vinoteca.features.order.dto.OrderResponseDTO;
import com.utn.vinoteca.features.order.entity.OrderEntity;
import com.utn.vinoteca.features.order.enums.OrderStatus;
import com.utn.vinoteca.features.payment.dto.PaymentRequestDTO;
import com.utn.vinoteca.features.payment.dto.PaymentResponseDTO;
import com.utn.vinoteca.features.payment.entity.PaymentEntity;
import com.utn.vinoteca.features.payment.entity.PaymentMethod;
import com.utn.vinoteca.features.payment.entity.PaymentStatus;
import com.utn.vinoteca.features.payment.service.CardPaymentSimulator;
import com.utn.vinoteca.features.stockMovement.enums.StockMovementType;
import com.utn.vinoteca.features.user.entity.UserEntity;
import com.utn.vinoteca.features.wineVariant.entity.WineVariantEntity;
import com.utn.vinoteca.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

class PaymentRejectedTest extends AbstractIntegrationTest {

    @MockitoBean
    private CardPaymentSimulator cardPaymentSimulator;

    @Test
    void rejectedPaymentCancelsOrderAndRestoresStock() {
        when(cardPaymentSimulator.simulate(any())).thenReturn(PaymentStatus.REJECTED);

        UserEntity user = createUser();
        WineVariantEntity variant = createVariant(money("100.00"), 10, null, null);

        addToCart(user, variant, 2);
        OrderResponseDTO order = checkout(user);

        assertThat(variant.getStock()).isEqualTo(8);

        PaymentResponseDTO payment = paymentService.pay(PaymentRequestDTO.builder()
                .orderId(order.getExternalId())
                .paymentMethod(PaymentMethod.CREDIT_CARD)
                .amount(order.getTotal())
                .build());

        assertThat(payment.status()).isEqualTo(PaymentStatus.REJECTED);

        OrderEntity storedOrder = orderRepository.findByExternalId(order.getExternalId()).orElseThrow();
        assertThat(storedOrder.getStatus()).isEqualTo(OrderStatus.CANCELLED);

        PaymentEntity storedPayment = paymentRepository.findByOrder_ExternalId(order.getExternalId()).orElseThrow();
        assertThat(storedPayment.getPaymentStatus()).isEqualTo(PaymentStatus.REJECTED);

        assertThat(variant.getStock()).isEqualTo(10);

        assertThat(movementsOf(variant))
                .anyMatch(movement -> movement.getType() == StockMovementType.CANCELLATION
                        && movement.getQuantity() == 2);
    }
}
