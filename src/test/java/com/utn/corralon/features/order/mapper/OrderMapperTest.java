package com.utn.corralon.features.order.mapper;

import com.utn.corralon.features.order.entity.OrderEntity;
import com.utn.corralon.features.order.enums.DeliveryType;
import com.utn.corralon.features.order.enums.OrderStatus;
import com.utn.corralon.features.order.util.OrderExpirationPolicy;
import com.utn.corralon.features.user.entity.UserEntity;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class OrderMapperTest {

    @Test
    void toEntity_shouldCreatePendingPaymentOrderThatExpiresInSixtyMinutes() {
        OrderMapper mapper = new OrderMapper(null, null);
        UserEntity user = UserEntity.builder().id(1L).build();

        OrderEntity order = mapper.toEntity(user, null, DeliveryType.PICKUP);

        assertThat(order.getStatus()).isEqualTo(OrderStatus.PENDING_PAYMENT);
        assertThat(order.getCreatedAt()).isNotNull();
        assertThat(order.getExpiresAt())
                .isEqualTo(order.getCreatedAt().plus(OrderExpirationPolicy.PAYMENT_WINDOW));
        assertThat(order.getExpiresAt())
                .isEqualTo(order.getCreatedAt().plusMinutes(60));
    }
}
