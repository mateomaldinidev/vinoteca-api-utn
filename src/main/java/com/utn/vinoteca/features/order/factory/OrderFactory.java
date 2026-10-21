package com.utn.vinoteca.features.order.factory;

import com.utn.vinoteca.features.address.entity.AddressEntity;
import com.utn.vinoteca.features.order.entity.OrderEntity;
import com.utn.vinoteca.features.order.enums.DeliveryType;
import com.utn.vinoteca.features.order.enums.OrderStatus;
import com.utn.vinoteca.features.orderItem.entity.OrderItemEntity;
import com.utn.vinoteca.features.user.entity.UserEntity;
import com.utn.vinoteca.features.wineVariant.entity.WineVariantEntity;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Component
public class OrderFactory {

    public OrderEntity create(UserEntity user, AddressEntity address, DeliveryType deliveryType) {
        return OrderEntity.builder()
                .user(user)
                .address(address)
                .deliveryType(deliveryType)
                .createdAt(LocalDateTime.now())
                .status(OrderStatus.PENDING_PAYMENT)
                .build();
    }

    public OrderItemEntity createItem(OrderEntity order,
                                      WineVariantEntity variant,
                                      int quantity,
                                      BigDecimal unitPrice) {
        return OrderItemEntity.builder()
                .order(order)
                .wineVariant(variant)
                .quantity(quantity)
                .unitPrice(unitPrice)
                .subtotal(unitPrice.multiply(BigDecimal.valueOf(quantity)))
                .build();
    }
}
