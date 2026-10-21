package com.utn.vinoteca.integration;

import com.utn.vinoteca.features.cart.dto.CartRequestDTO;
import com.utn.vinoteca.features.cart.dto.CartResponseDTO;
import com.utn.vinoteca.features.cartItem.dto.CartItemRequestDTO;
import com.utn.vinoteca.features.user.entity.UserEntity;
import com.utn.vinoteca.features.wineVariant.entity.WineVariantEntity;
import com.utn.vinoteca.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CartResponseTest extends AbstractIntegrationTest {

    @Test
    void cartResponseIncludesTheOwnerExternalId() {
        UserEntity user = createUser();
        WineVariantEntity variant = createVariant(money("100.00"), 10, null, null);

        CartResponseDTO cart = cartService.createOrUpdateCart(CartRequestDTO.builder()
                .userId(user.getExternalId())
                .items(List.of(CartItemRequestDTO.builder()
                        .wineVariantId(variant.getExternalId())
                        .quantity(1)
                        .build()))
                .build());

        assertThat(cart.getUserExternalId()).isEqualTo(user.getExternalId());
    }
}
