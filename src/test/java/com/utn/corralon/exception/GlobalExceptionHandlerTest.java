package com.utn.corralon.exception;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void handleBadRequest_shouldMapDuplicatedVariantErrorTo400() {
        ResponseEntity<String> response = handler.handleBadRequest(
                new BadRequestException("Duplicate product variant ID in cart: test"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).contains("Duplicate product variant ID");
    }

    @Test
    void handleBusinessRule_shouldMapIdempotencyConflictTo409() {
        ResponseEntity<String> response = handler.handleBusinessRule(
                new BusinessRuleException(
                        "Idempotency-Key already used with different checkout parameters"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).contains("Idempotency-Key");
    }

    @Test
    void handleResourceNotFound_shouldMapMissingCartItemTo404() {
        UUID variantId = UUID.randomUUID();

        ResponseEntity<String> response = handler.handleResourceNotFound(
                new ResourceNotFoundException(
                        "Cart item for product variant not found with ID: ",
                        variantId));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).contains(variantId.toString());
    }
}
