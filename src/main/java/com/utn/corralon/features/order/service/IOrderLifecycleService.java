package com.utn.corralon.features.order.service;

import com.utn.corralon.features.order.entity.OrderEntity;
import com.utn.corralon.features.order.enums.CancellationReason;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Punto unico para las transiciones de cancelacion del pedido.
 * Toda cancelacion (manual, rechazo de pago o vencimiento) pasa por aca,
 * de modo que el stock se restaura exactamente una vez.
 */
public interface IOrderLifecycleService {

    /**
     * Bloquea el pedido, revalida que siga pendiente de pago y lo cancela
     * con el motivo indicado.
     */
    OrderEntity cancelPendingOrder(UUID externalId, CancellationReason reason, Long actorUserId);

    /**
     * Bloquea el pedido y lo cancela solo si sigue PENDING_PAYMENT y ya
     * alcanzo su fecha de vencimiento.
     *
     * @return true si el pedido fue cancelado por vencimiento.
     */
    boolean expireIfDue(UUID externalId, LocalDateTime referenceTime);

    /**
     * Aplica la cancelacion sobre un pedido que el llamador ya bloqueo
     * (por ejemplo, el flujo de pago rechazado). No vuelve a buscarlo.
     */
    void cancelLockedOrder(OrderEntity order, CancellationReason reason, Long actorUserId);
}
