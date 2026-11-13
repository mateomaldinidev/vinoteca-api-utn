package com.utn.corralon.features.order.service;

import com.utn.corralon.exception.BadRequestException;
import com.utn.corralon.exception.BusinessRuleException;
import com.utn.corralon.exception.ResourceNotFoundException;
import com.utn.corralon.features.notifications.service.IEmailService;
import com.utn.corralon.features.order.dto.OrderResponseDTO;
import com.utn.corralon.features.order.entity.OrderEntity;
import com.utn.corralon.features.order.enums.CancellationReason;
import com.utn.corralon.features.order.enums.OrderStatus;
import com.utn.corralon.features.order.mapper.OrderMapper;
import com.utn.corralon.features.order.repository.OrderRepository;
import com.utn.corralon.features.orderItem.entity.OrderItemEntity;
import com.utn.corralon.features.stockMovement.service.IStockService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OrderLifecycleService implements IOrderLifecycleService {

    private final OrderRepository orderRepository;
    private final IStockService stockService;
    private final IEmailService emailService;
    private final OrderMapper orderMapper;

    @Override
    @Transactional
    public OrderEntity cancelPendingOrder(UUID externalId,
                                          CancellationReason reason,
                                          Long actorUserId) {

        OrderEntity order = lockOrder(externalId);
        ensureCancellable(order);
        applyCancellation(order, reason, actorUserId, LocalDateTime.now());
        return order;
    }

    @Override
    @Transactional
    public boolean expireIfDue(UUID externalId, LocalDateTime referenceTime) {

        OrderEntity order = lockOrder(externalId);

        // Revalidacion bajo lock: otro proceso pudo haber pagado o cancelado
        // el pedido mientras el scheduler lo recolectaba.
        if (order.getStatus() != OrderStatus.PENDING_PAYMENT) {
            return false;
        }

        if (order.getExpiresAt() == null
                || referenceTime.isBefore(order.getExpiresAt())) {
            return false;
        }

        applyCancellation(order, CancellationReason.EXPIRED, null, referenceTime);
        return true;
    }

    @Override
    @Transactional
    public void cancelLockedOrder(OrderEntity order,
                                  CancellationReason reason,
                                  Long actorUserId) {

        ensureCancellable(order);
        applyCancellation(order, reason, actorUserId, LocalDateTime.now());
    }

    private OrderEntity lockOrder(UUID externalId) {
        return orderRepository.findByExternalIdForUpdate(externalId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Order not found with ID: ", externalId));
    }

    private void ensureCancellable(OrderEntity order) {
        if (order.getStatus() == OrderStatus.CANCELLED) {
            throw new BadRequestException("Order is already cancelled");
        }

        if (order.getStatus() == OrderStatus.PAID) {
            throw new BusinessRuleException("Paid orders cannot be cancelled");
        }

        if (order.getStatus() != OrderStatus.PENDING_PAYMENT) {
            throw new BusinessRuleException("Order is not pending payment");
        }
    }

    private void applyCancellation(OrderEntity order,
                                   CancellationReason reason,
                                   Long actorUserId,
                                   LocalDateTime cancelledAt) {

        if (reason == null) {
            throw new BusinessRuleException("Cancellation reason is required");
        }

        // Se restaura antes de marcar el pedido como cancelado. Si el stock
        // falla, la transaccion completa hace rollback y el pedido sigue
        // PENDING_PAYMENT para un nuevo intento.
        restoreStock(order, actorUserId, reason);

        order.setStatus(OrderStatus.CANCELLED);
        order.setCancellationReason(reason.name());
        order.setCancelledAt(cancelledAt);
        orderRepository.save(order);

        OrderResponseDTO orderDTO = orderMapper.toResponseDTO(order);

        emailService.sendOrderCancelledEmail(
                order.getUser().getEmail(),
                order.getUser().getName(),
                orderDTO,
                displayReason(reason)
        );
    }

    private void restoreStock(OrderEntity order,
                              Long actorUserId,
                              CancellationReason reason) {

        List<OrderItemEntity> items = order.getItems();
        if (items == null) {
            return;
        }

        for (OrderItemEntity item : items) {
            stockService.restoreStock(
                    item.getProductVariant().getExternalId(),
                    item.getQuantity(),
                    order.getId(),
                    item.getId(),
                    actorUserId,
                    "Stock restored by order cancellation: " + reason.name()
            );
        }
    }

    private String displayReason(CancellationReason reason) {
        return switch (reason) {
            case EXPIRED -> "Order expired";
            case USER_CANCELLED -> "Order cancelled by customer";
            case ADMIN_CANCELLED -> "Order cancelled by administrator";
            case PAYMENT_REJECTED -> "Payment was rejected";
            case OTHER -> "Order cancelled";
        };
    }
}
