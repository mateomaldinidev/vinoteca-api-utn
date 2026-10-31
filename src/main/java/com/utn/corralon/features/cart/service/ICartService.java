package com.utn.corralon.features.cart.service;

import com.utn.corralon.features.cart.dto.CartRequestDTO;
import com.utn.corralon.features.cart.dto.CartResponseDTO;
import com.utn.corralon.features.cart.entity.CartEntity;
import com.utn.corralon.features.cart_item.dto.CartItemQuantityUpdateDTO;
import com.utn.corralon.features.order.dto.OrderResponseDTO;
import com.utn.corralon.features.order.enums.DeliveryType;

import java.util.UUID;

public interface ICartService {

    CartResponseDTO createOrUpdateCart(String username, CartRequestDTO cartRequest);

    CartResponseDTO updateCartItemQuantity(
            String username,
            UUID productVariantId,
            CartItemQuantityUpdateDTO updateDTO
    );

    CartResponseDTO getCart(String username);

    void clearCart(String username);

    OrderResponseDTO checkout(
            String username,
            UUID addressId,
            DeliveryType deliveryType
    );

    CartEntity getCartEntity(String username);
}
