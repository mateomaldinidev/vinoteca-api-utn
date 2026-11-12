package com.utn.corralon.features.order.service;

import com.utn.corralon.exception.BadRequestException;
import com.utn.corralon.exception.BusinessRuleException;
import com.utn.corralon.features.notifications.service.IEmailService;
import com.utn.corralon.features.order.dto.OrderResponseDTO;
import com.utn.corralon.features.order.entity.OrderEntity;
import com.utn.corralon.features.order.enums.CancellationReason;
import com.utn.corralon.features.order.enums.OrderStatus;
import com.utn.corralon.features.order.mapper.OrderMapper;
import com.utn.corralon.features.order.repository.OrderRepository;
import com.utn.corralon.features.orderItem.entity.OrderItemEntity;
import com.utn.corralon.features.productVariant.entity.ProductVariantEntity;
import com.utn.corralon.features.stockMovement.service.IStockService;
import com.utn.corralon.features.user.entity.UserEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderLifecycleServiceTest {

    private static final long ORDER_ID = 1L;
    private static final long ORDER_ITEM_ID = 10L;
    private static final long ACTOR_USER_ID = 7L;
    private static final int ITEM_QUANTITY = 2;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private IStockService stockService;

    @Mock
    private IEmailService emailService;

    @Mock
    private OrderMapper orderMapper;

    @InjectMocks
    private OrderLifecycleService lifecycleService;

    private UUID orderExternalId;
    private UUID variantExternalId;
    private UserEntity user;

    @BeforeEach
    void setUp() {
        orderExternalId = UUID.randomUUID();
        variantExternalId = UUID.randomUUID();
        user = UserEntity.builder()
                .id(ACTOR_USER_ID)
                .email("user@test.com")
                .name("Ana")
                .lastName("Perez")
                .build();

        lenient().when(orderMapper.toResponseDTO(any(OrderEntity.class)))
                .thenReturn(new OrderResponseDTO());
    }

    @Test
    void expireIfDue_whenPendingAndExpired_shouldCancelRestoreStockAndRecordReason() {
        LocalDateTime referenceTime = LocalDateTime.of(2026, 11, 11, 12, 0);
        OrderEntity order = order(OrderStatus.PENDING_PAYMENT, referenceTime.minusSeconds(1));
        when(orderRepository.findByExternalIdForUpdate(orderExternalId))
                .thenReturn(Optional.of(order));

        boolean cancelled = lifecycleService.expireIfDue(orderExternalId, referenceTime);

        assertThat(cancelled).isTrue();
        assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);
        assertThat(order.getCancellationReason()).isEqualTo(CancellationReason.EXPIRED.name());
        assertThat(order.getCancelledAt()).isEqualTo(referenceTime);

        verify(stockService).restoreStock(
                eq(variantExternalId),
                eq(ITEM_QUANTITY),
                eq(ORDER_ID),
                eq(ORDER_ITEM_ID),
                isNull(),
                contains(CancellationReason.EXPIRED.name())
        );
        verify(orderRepository).save(order);
        verify(emailService).sendOrderCancelledEmail(
                eq("user@test.com"),
                eq("Ana"),
                any(OrderResponseDTO.class),
                eq("Order expired")
        );
    }

    @Test
    void expireIfDue_whenStillValid_shouldNotCancelNorRestoreStock() {
        LocalDateTime referenceTime = LocalDateTime.of(2026, 11, 11, 12, 0);
        OrderEntity order = order(OrderStatus.PENDING_PAYMENT, referenceTime.plusSeconds(1));
        when(orderRepository.findByExternalIdForUpdate(orderExternalId))
                .thenReturn(Optional.of(order));

        boolean cancelled = lifecycleService.expireIfDue(orderExternalId, referenceTime);

        assertThat(cancelled).isFalse();
        assertThat(order.getStatus()).isEqualTo(OrderStatus.PENDING_PAYMENT);
        assertThat(order.getCancellationReason()).isNull();

        verifyNoInteractions(stockService);
        verify(orderRepository, never()).save(any(OrderEntity.class));
        verifyNoInteractions(emailService);
    }

    @Test
    void expireIfDue_whenAlreadyPaid_shouldNotRestoreStock() {
        LocalDateTime referenceTime = LocalDateTime.of(2026, 11, 11, 12, 0);
        OrderEntity order = order(OrderStatus.PAID, referenceTime.minusSeconds(1));
        when(orderRepository.findByExternalIdForUpdate(orderExternalId))
                .thenReturn(Optional.of(order));

        boolean cancelled = lifecycleService.expireIfDue(orderExternalId, referenceTime);

        assertThat(cancelled).isFalse();
        assertThat(order.getStatus()).isEqualTo(OrderStatus.PAID);

        verifyNoInteractions(stockService);
        verify(orderRepository, never()).save(any(OrderEntity.class));
        verifyNoInteractions(emailService);
    }

    @Test
    void expireIfDue_whenAlreadyCancelled_shouldNotRestoreStock() {
        LocalDateTime referenceTime = LocalDateTime.of(2026, 11, 11, 12, 0);
        OrderEntity order = order(OrderStatus.CANCELLED, referenceTime.minusSeconds(1));
        order.setCancellationReason(CancellationReason.USER_CANCELLED.name());
        when(orderRepository.findByExternalIdForUpdate(orderExternalId))
                .thenReturn(Optional.of(order));

        boolean cancelled = lifecycleService.expireIfDue(orderExternalId, referenceTime);

        assertThat(cancelled).isFalse();
        assertThat(order.getCancellationReason())
                .isEqualTo(CancellationReason.USER_CANCELLED.name());

        verifyNoInteractions(stockService);
        verify(orderRepository, never()).save(any(OrderEntity.class));
        verifyNoInteractions(emailService);
    }

    @Test
    void expireIfDue_whenSecondProcessRevalidatesAfterFirst_shouldRestoreStockOnlyOnce() {
        LocalDateTime referenceTime = LocalDateTime.of(2026, 11, 11, 12, 0);
        OrderEntity order = order(OrderStatus.PENDING_PAYMENT, referenceTime.minusSeconds(1));
        when(orderRepository.findByExternalIdForUpdate(orderExternalId))
                .thenReturn(Optional.of(order));

        boolean firstProcess = lifecycleService.expireIfDue(orderExternalId, referenceTime);
        boolean secondProcess = lifecycleService.expireIfDue(orderExternalId, referenceTime);

        assertThat(firstProcess).isTrue();
        assertThat(secondProcess).isFalse();

        verify(stockService, times(1)).restoreStock(
                eq(variantExternalId),
                eq(ITEM_QUANTITY),
                eq(ORDER_ID),
                eq(ORDER_ITEM_ID),
                isNull(),
                contains(CancellationReason.EXPIRED.name())
        );
        verify(orderRepository, times(1)).save(order);
    }

    @Test
    void cancelPendingOrder_whenPending_shouldCancelWithRequestedReason() {
        OrderEntity order = order(OrderStatus.PENDING_PAYMENT,
                LocalDateTime.now().plusMinutes(10));
        when(orderRepository.findByExternalIdForUpdate(orderExternalId))
                .thenReturn(Optional.of(order));

        OrderEntity cancelled = lifecycleService.cancelPendingOrder(
                orderExternalId,
                CancellationReason.USER_CANCELLED,
                ACTOR_USER_ID
        );

        assertThat(cancelled.getStatus()).isEqualTo(OrderStatus.CANCELLED);
        assertThat(cancelled.getCancellationReason())
                .isEqualTo(CancellationReason.USER_CANCELLED.name());
        assertThat(cancelled.getCancelledAt()).isNotNull();

        verify(stockService).restoreStock(
                eq(variantExternalId),
                eq(ITEM_QUANTITY),
                eq(ORDER_ID),
                eq(ORDER_ITEM_ID),
                eq(ACTOR_USER_ID),
                contains(CancellationReason.USER_CANCELLED.name())
        );
        verify(orderRepository).save(order);
    }

    @Test
    void cancelPendingOrder_whenAlreadyPaid_shouldThrowAndNotRestoreStock() {
        OrderEntity order = order(OrderStatus.PAID, LocalDateTime.now().plusMinutes(10));
        when(orderRepository.findByExternalIdForUpdate(orderExternalId))
                .thenReturn(Optional.of(order));

        assertThatThrownBy(() -> lifecycleService.cancelPendingOrder(
                orderExternalId,
                CancellationReason.USER_CANCELLED,
                ACTOR_USER_ID))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("Paid orders cannot be cancelled");

        verifyNoInteractions(stockService);
        verify(orderRepository, never()).save(any(OrderEntity.class));
        verifyNoInteractions(emailService);
    }

    @Test
    void cancelPendingOrder_whenAlreadyCancelled_shouldThrowAndNotRestoreStock() {
        OrderEntity order = order(OrderStatus.CANCELLED, LocalDateTime.now().plusMinutes(10));
        when(orderRepository.findByExternalIdForUpdate(orderExternalId))
                .thenReturn(Optional.of(order));

        assertThatThrownBy(() -> lifecycleService.cancelPendingOrder(
                orderExternalId,
                CancellationReason.USER_CANCELLED,
                ACTOR_USER_ID))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Order is already cancelled");

        verifyNoInteractions(stockService);
        verify(orderRepository, never()).save(any(OrderEntity.class));
        verifyNoInteractions(emailService);
    }

    @Test
    void cancelLockedOrder_whenPending_shouldCancelWithRequestedReason() {
        OrderEntity order = order(OrderStatus.PENDING_PAYMENT,
                LocalDateTime.now().plusMinutes(10));

        lifecycleService.cancelLockedOrder(
                order,
                CancellationReason.PAYMENT_REJECTED,
                ACTOR_USER_ID
        );

        assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);
        assertThat(order.getCancellationReason())
                .isEqualTo(CancellationReason.PAYMENT_REJECTED.name());
        assertThat(order.getCancelledAt()).isNotNull();

        verify(stockService).restoreStock(
                eq(variantExternalId),
                eq(ITEM_QUANTITY),
                eq(ORDER_ID),
                eq(ORDER_ITEM_ID),
                eq(ACTOR_USER_ID),
                contains(CancellationReason.PAYMENT_REJECTED.name())
        );
        verify(orderRepository).save(order);
        verify(emailService).sendOrderCancelledEmail(
                eq("user@test.com"),
                eq("Ana"),
                any(OrderResponseDTO.class),
                eq("Payment was rejected")
        );
    }

    @Test
    void cancelLockedOrder_whenAlreadyPaid_shouldThrowAndNotRestoreStock() {
        OrderEntity order = order(OrderStatus.PAID, LocalDateTime.now().plusMinutes(10));

        assertThatThrownBy(() -> lifecycleService.cancelLockedOrder(
                order,
                CancellationReason.PAYMENT_REJECTED,
                ACTOR_USER_ID))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("Paid orders cannot be cancelled");

        verifyNoInteractions(stockService);
        verify(orderRepository, never()).save(any(OrderEntity.class));
        verifyNoInteractions(emailService);
    }

    @Test
    void expireIfDue_whenStockRestoreFails_shouldPropagateAndNotPersistCancellation() {
        LocalDateTime referenceTime = LocalDateTime.of(2026, 11, 11, 12, 0);
        OrderEntity order = order(OrderStatus.PENDING_PAYMENT, referenceTime.minusSeconds(1));
        when(orderRepository.findByExternalIdForUpdate(orderExternalId))
                .thenReturn(Optional.of(order));
        doThrow(new IllegalStateException("stock failure"))
                .when(stockService)
                .restoreStock(any(), any(), any(), any(), any(), any());

        assertThatThrownBy(() -> lifecycleService.expireIfDue(orderExternalId, referenceTime))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("stock failure");

        // La transaccion hace rollback: el pedido no queda marcado como cancelado.
        assertThat(order.getStatus()).isEqualTo(OrderStatus.PENDING_PAYMENT);
        assertThat(order.getCancellationReason()).isNull();
        assertThat(order.getCancelledAt()).isNull();

        verify(orderRepository, never()).save(any(OrderEntity.class));
        verifyNoInteractions(emailService);
    }

    private OrderEntity order(OrderStatus status, LocalDateTime expiresAt) {
        ProductVariantEntity variant = ProductVariantEntity.builder()
                .id(100L)
                .externalId(variantExternalId)
                .attribute("750ml")
                .stock(50)
                .active(true)
                .build();

        OrderItemEntity item = OrderItemEntity.builder()
                .id(ORDER_ITEM_ID)
                .externalId(UUID.randomUUID())
                .productVariant(variant)
                .quantity(ITEM_QUANTITY)
                .build();

        OrderEntity order = OrderEntity.builder()
                .id(ORDER_ID)
                .externalId(orderExternalId)
                .status(status)
                .expiresAt(expiresAt)
                .createdAt(LocalDateTime.now().minusHours(2))
                .user(user)
                .items(List.of(item))
                .build();

        item.setOrder(order);
        return order;
    }
}
