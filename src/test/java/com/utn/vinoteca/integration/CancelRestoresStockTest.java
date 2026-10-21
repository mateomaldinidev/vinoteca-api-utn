package com.utn.vinoteca.integration;

import com.utn.vinoteca.features.order.dto.OrderResponseDTO;
import com.utn.vinoteca.features.order.entity.OrderEntity;
import com.utn.vinoteca.features.order.enums.OrderStatus;
import com.utn.vinoteca.features.stockMovement.enums.StockMovementType;
import com.utn.vinoteca.features.user.entity.UserEntity;
import com.utn.vinoteca.features.wineVariant.entity.WineVariantEntity;
import com.utn.vinoteca.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CancelRestoresStockTest extends AbstractIntegrationTest {

    @Test
    void cancellingAnOrderRestoresStockAndRegistersCancellationMovement() {
        UserEntity user = createCustomer();
        WineVariantEntity variant = createVariant(money("100.00"), 10, null, null);

        addToCart(user, variant, 3);
        OrderResponseDTO order = checkout(user);

        assertThat(variant.getStock()).isEqualTo(7);

        orderService.cancelOrder(order.getExternalId(), user.getExternalId());

        assertThat(variant.getStock()).isEqualTo(10);

        OrderEntity stored = orderRepository.findByExternalId(order.getExternalId()).orElseThrow();
        assertThat(stored.getStatus()).isEqualTo(OrderStatus.CANCELLED);

        assertThat(movementsOf(variant))
                .anyMatch(movement -> movement.getType() == StockMovementType.CANCELLATION
                        && movement.getQuantity() == 3);
    }
}
