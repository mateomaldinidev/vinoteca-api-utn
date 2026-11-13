package com.utn.corralon.features.order.scheduler;

import com.utn.corralon.features.order.enums.OrderStatus;
import com.utn.corralon.features.order.repository.OrderRepository;
import com.utn.corralon.features.order.service.IOrderLifecycleService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderExpirationSchedulerTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private IOrderLifecycleService orderLifecycleService;

    @InjectMocks
    private OrderExpirationScheduler scheduler;

    @Test
    void expirePendingOrders_shouldProcessEachExpiredOrderIndependently() {
        UUID firstOrder = UUID.randomUUID();
        UUID secondOrder = UUID.randomUUID();

        when(orderRepository.findExpiredOrderExternalIds(
                eq(OrderStatus.PENDING_PAYMENT), any(LocalDateTime.class)))
                .thenReturn(List.of(firstOrder, secondOrder));
        when(orderLifecycleService.expireIfDue(eq(firstOrder), any(LocalDateTime.class)))
                .thenReturn(true);
        when(orderLifecycleService.expireIfDue(eq(secondOrder), any(LocalDateTime.class)))
                .thenReturn(false);

        scheduler.expirePendingOrders();

        verify(orderLifecycleService).expireIfDue(eq(firstOrder), any(LocalDateTime.class));
        verify(orderLifecycleService).expireIfDue(eq(secondOrder), any(LocalDateTime.class));
    }

    @Test
    void expirePendingOrders_whenOneOrderFails_shouldContinueWithTheRest() {
        UUID failingOrder = UUID.randomUUID();
        UUID healthyOrder = UUID.randomUUID();

        when(orderRepository.findExpiredOrderExternalIds(
                eq(OrderStatus.PENDING_PAYMENT), any(LocalDateTime.class)))
                .thenReturn(List.of(failingOrder, healthyOrder));
        when(orderLifecycleService.expireIfDue(eq(failingOrder), any(LocalDateTime.class)))
                .thenThrow(new IllegalStateException("database error"));
        when(orderLifecycleService.expireIfDue(eq(healthyOrder), any(LocalDateTime.class)))
                .thenReturn(true);

        scheduler.expirePendingOrders();

        verify(orderLifecycleService).expireIfDue(eq(failingOrder), any(LocalDateTime.class));
        verify(orderLifecycleService).expireIfDue(eq(healthyOrder), any(LocalDateTime.class));
    }

    @Test
    void expirePendingOrders_whenThereAreNoExpiredOrders_shouldNotCallLifecycle() {
        when(orderRepository.findExpiredOrderExternalIds(
                eq(OrderStatus.PENDING_PAYMENT), any(LocalDateTime.class)))
                .thenReturn(List.of());

        scheduler.expirePendingOrders();

        verifyNoInteractions(orderLifecycleService);
    }
}
