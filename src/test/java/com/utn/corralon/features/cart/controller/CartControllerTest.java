package com.utn.corralon.features.cart.controller;

import com.utn.corralon.features.cart.dto.CheckoutResultDTO;
import com.utn.corralon.features.cart.service.ICartService;
import com.utn.corralon.features.order.dto.OrderResponseDTO;
import com.utn.corralon.features.order.enums.DeliveryType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CartControllerTest {

    private static final String USER_EMAIL = "user@test.com";
    private static final String IDEMPOTENCY_KEY = "key-controller-001";

    @Mock
    private ICartService cartService;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private CartController cartController;

    @Test
    void checkout_whenNewOrder_shouldReturn201WithOrder() {
        UUID addressId = UUID.randomUUID();
        OrderResponseDTO order = new OrderResponseDTO();
        order.setExternalId(UUID.randomUUID());

        when(authentication.getName()).thenReturn(USER_EMAIL);
        when(cartService.checkout(USER_EMAIL, addressId, DeliveryType.DELIVERY, IDEMPOTENCY_KEY))
                .thenReturn(new CheckoutResultDTO(order, false));

        ResponseEntity<OrderResponseDTO> response = cartController.checkout(
                IDEMPOTENCY_KEY, DeliveryType.DELIVERY, addressId, authentication);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isEqualTo(order);
    }

    @Test
    void checkout_whenIdempotentReplay_shouldReturn200WithExistingOrder() {
        OrderResponseDTO order = new OrderResponseDTO();
        order.setExternalId(UUID.randomUUID());

        when(authentication.getName()).thenReturn(USER_EMAIL);
        when(cartService.checkout(USER_EMAIL, null, DeliveryType.PICKUP, IDEMPOTENCY_KEY))
                .thenReturn(new CheckoutResultDTO(order, true));

        ResponseEntity<OrderResponseDTO> response = cartController.checkout(
                IDEMPOTENCY_KEY, DeliveryType.PICKUP, null, authentication);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(order);
    }
}
