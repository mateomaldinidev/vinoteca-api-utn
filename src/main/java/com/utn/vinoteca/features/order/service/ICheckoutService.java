package com.utn.vinoteca.features.order.service;

import com.utn.vinoteca.features.cart.entity.CartEntity;
import com.utn.vinoteca.features.order.dto.OrderResponseDTO;
import com.utn.vinoteca.features.order.enums.DeliveryType;

import java.util.UUID;

public interface ICheckoutService {

    OrderResponseDTO createFromCart(CartEntity cart, UUID addressId, DeliveryType deliveryType);
}
