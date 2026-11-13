package com.utn.corralon.features.order.scheduler;

import com.utn.corralon.features.order.enums.OrderStatus;
import com.utn.corralon.features.order.repository.OrderRepository;
import com.utn.corralon.features.order.service.IOrderLifecycleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(
        prefix = "app.orders.expiration",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true
)
public class OrderExpirationScheduler {

    private final OrderRepository orderRepository;
    private final IOrderLifecycleService orderLifecycleService;

    @Scheduled(
            fixedDelayString = "${app.orders.expiration.interval-ms:60000}",
            initialDelayString = "${app.orders.expiration.initial-delay-ms:60000}"
    )
    public void expirePendingOrders() {

        LocalDateTime referenceTime = LocalDateTime.now();
        List<UUID> expiredOrderIds = orderRepository
                .findExpiredOrderExternalIds(OrderStatus.PENDING_PAYMENT, referenceTime);

        if (expiredOrderIds.isEmpty()) {
            return;
        }

        log.info("Found {} expired order(s) to cancel", expiredOrderIds.size());

        for (UUID orderId : expiredOrderIds) {
            try {
                // Cada llamada abre su propia transaccion. Un fallo aislado
                // no impide procesar los demas pedidos vencidos.
                boolean cancelled = orderLifecycleService.expireIfDue(orderId, referenceTime);
                if (cancelled) {
                    log.info("Order {} expired and was cancelled", orderId);
                }
            } catch (Exception ex) {
                log.error("Failed to expire order {}", orderId, ex);
            }
        }
    }
}
