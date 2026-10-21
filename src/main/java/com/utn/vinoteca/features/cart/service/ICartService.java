package com.utn.vinoteca.features.cart.service;

import com.utn.vinoteca.features.cart.dto.CartRequestDTO;
import com.utn.vinoteca.features.cart.dto.CartResponseDTO;
import com.utn.vinoteca.features.cart.entity.CartEntity;
import com.utn.vinoteca.features.cart_item.dto.CartItemQuantityUpdateDTO;
import com.utn.vinoteca.features.order.dto.OrderResponseDTO;
import com.utn.vinoteca.features.order.enums.DeliveryType;

import java.util.UUID;

public interface ICartService {

    CartResponseDTO createOrUpdateCart(CartRequestDTO cartRequest);

    CartResponseDTO updateCartItemQuantity(UUID userExternalId, UUID productVariantId, CartItemQuantityUpdateDTO updateDTO);

    CartResponseDTO getCartByUserId(UUID userExternalId);

    void clearCart(UUID userExternalId);

    OrderResponseDTO checkout(UUID userId, UUID addressId, DeliveryType deliveryType);

    CartEntity getCartEntityByUserId(UUID userExternalId);
}
