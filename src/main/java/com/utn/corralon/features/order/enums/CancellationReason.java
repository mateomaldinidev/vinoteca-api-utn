package com.utn.corralon.features.order.enums;

/**
 * Motivos de cancelacion acordados para el ciclo de vida del pedido.
 * Se persisten como texto en orders.cancellation_reason.
 */
public enum CancellationReason {
    EXPIRED,
    USER_CANCELLED,
    ADMIN_CANCELLED,
    PAYMENT_REJECTED,
    OTHER
}
