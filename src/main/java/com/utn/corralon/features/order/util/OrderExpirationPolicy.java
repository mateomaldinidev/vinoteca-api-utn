package com.utn.corralon.features.order.util;

import java.time.Duration;
import java.time.LocalDateTime;

/**
 * Politica unica de vencimiento de pedidos pendientes de pago.
 */
public final class OrderExpirationPolicy {

    public static final Duration PAYMENT_WINDOW = Duration.ofMinutes(60);

    private OrderExpirationPolicy() {
    }

    public static LocalDateTime calculateExpiresAt(LocalDateTime createdAt) {
        return createdAt.plus(PAYMENT_WINDOW);
    }
}
