package com.utn.vinoteca.features.order.service;

import com.utn.vinoteca.features.cart.entity.CartEntity;
import com.utn.vinoteca.features.order.dto.OrderAdminResponseDTO;
import com.utn.vinoteca.features.order.dto.OrderResponseDTO;
import com.utn.vinoteca.features.order.dto.OrderSummaryDTO;
import com.utn.vinoteca.features.order.enums.DeliveryType;

import java.util.List;
import java.util.UUID;

public interface IOrderService {

    OrderResponseDTO createFromCart(
            CartEntity cart, UUID addressId,
             DeliveryType deliveryType  // null = retiro en sucursal
    );

    List<OrderAdminResponseDTO> getAll();

    OrderResponseDTO getByExternalId(UUID externalId);

    List<OrderSummaryDTO> getOrdersByUser(UUID userExternalId);

    OrderAdminResponseDTO getAdminOrder(UUID externalId);

    void cancelOrder(UUID externalId, UUID userExternalId);
}
