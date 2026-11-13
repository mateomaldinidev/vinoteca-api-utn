package com.utn.corralon.features.payment.service;

import com.utn.corralon.exception.BusinessRuleException;
import com.utn.corralon.exception.UnauthorizedException;
import com.utn.corralon.features.notifications.service.IEmailService;
import com.utn.corralon.features.order.dto.OrderResponseDTO;
import com.utn.corralon.features.order.entity.OrderEntity;
import com.utn.corralon.features.order.enums.CancellationReason;
import com.utn.corralon.features.order.enums.OrderStatus;
import com.utn.corralon.features.order.mapper.OrderMapper;
import com.utn.corralon.features.order.repository.OrderRepository;
import com.utn.corralon.features.order.service.IOrderLifecycleService;
import com.utn.corralon.features.payment.dto.PaymentRequestDTO;
import com.utn.corralon.features.payment.dto.PaymentResponseDTO;
import com.utn.corralon.features.payment.entity.PaymentEntity;
import com.utn.corralon.features.payment.entity.PaymentMethod;
import com.utn.corralon.features.payment.entity.PaymentStatus;
import com.utn.corralon.features.payment.mapper.PaymentMapper;
import com.utn.corralon.features.payment.repository.PaymentRepository;
import com.utn.corralon.features.user.entity.UserEntity;
import com.utn.corralon.features.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    private static final String USERNAME = "user@test.com";
    private static final long USER_ID = 7L;

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PaymentMapper paymentMapper;

    @Mock
    private OrderMapper orderMapper;

    @Mock
    private IEmailService emailService;

    @Mock
    private IOrderLifecycleService orderLifecycleService;

    @Spy
    @InjectMocks
    private PaymentService paymentService;

    private UserEntity user;

    @BeforeEach
    void setUp() {
        user = UserEntity.builder()
                .id(USER_ID)
                .email(USERNAME)
                .name("Ana")
                .lastName("Perez")
                .build();

        lenient().when(orderMapper.toResponseDTO(any(OrderEntity.class)))
                .thenReturn(new OrderResponseDTO());

        lenient().when(paymentMapper.toDTO(any(PaymentEntity.class)))
                .thenAnswer(invocation -> {
                    PaymentEntity payment = invocation.getArgument(0);
                    return new PaymentResponseDTO(
                            payment.getExternalId(),
                            payment.getOrder().getExternalId(),
                            payment.getPaymentMethod(),
                            payment.getAmount(),
                            payment.getPaymentStatus(),
                            payment.getCreatedAt()
                    );
                });
    }

    @Test
    void pay_whenOrderIsExpired_shouldRejectWithoutCreatingPaymentOrCancelling() {
        OrderEntity order = order(OrderStatus.PENDING_PAYMENT,
                LocalDateTime.now().minusMinutes(1));
        PaymentRequestDTO request = requestFor(order);
        when(orderRepository.findByExternalIdForUpdate(order.getExternalId()))
                .thenReturn(Optional.of(order));

        assertThatThrownBy(() -> paymentService.pay(request, USERNAME, false))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("expired");

        assertThat(order.getStatus()).isEqualTo(OrderStatus.PENDING_PAYMENT);
        verify(paymentRepository, never()).save(any(PaymentEntity.class));
        verifyNoInteractions(orderLifecycleService);
    }

    @Test
    void pay_whenOrderIsAlreadyPaid_shouldRejectWithoutRestoringStock() {
        OrderEntity order = order(OrderStatus.PAID, LocalDateTime.now().plusMinutes(10));
        PaymentRequestDTO request = requestFor(order);
        when(orderRepository.findByExternalIdForUpdate(order.getExternalId()))
                .thenReturn(Optional.of(order));

        assertThatThrownBy(() -> paymentService.pay(request, USERNAME, false))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("Order is not pending payment");

        verify(paymentRepository, never()).save(any(PaymentEntity.class));
        verifyNoInteractions(orderLifecycleService);
    }

    @Test
    void pay_whenExpirationWonTheRace_shouldRejectPaymentOnCancelledOrder() {
        OrderEntity order = order(OrderStatus.CANCELLED,
                LocalDateTime.now().minusMinutes(1));
        PaymentRequestDTO request = requestFor(order);
        when(orderRepository.findByExternalIdForUpdate(order.getExternalId()))
                .thenReturn(Optional.of(order));

        assertThatThrownBy(() -> paymentService.pay(request, USERNAME, false))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("Order is not pending payment");

        verify(paymentRepository, never()).save(any(PaymentEntity.class));
        verifyNoInteractions(orderLifecycleService);
    }

    @Test
    void pay_whenRejected_shouldDelegateCancellationWithPaymentRejectedReason() {
        OrderEntity order = order(OrderStatus.PENDING_PAYMENT,
                LocalDateTime.now().plusMinutes(10));
        PaymentRequestDTO request = requestFor(order);

        when(orderRepository.findByExternalIdForUpdate(order.getExternalId()))
                .thenReturn(Optional.of(order));
        when(paymentRepository.save(any(PaymentEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(userRepository.findByEmail(USERNAME)).thenReturn(Optional.of(user));
        doReturn(PaymentStatus.REJECTED).when(paymentService).simulateCard(any());

        PaymentResponseDTO response = paymentService.pay(request, USERNAME, false);

        assertThat(response.status()).isEqualTo(PaymentStatus.REJECTED);
        verify(orderLifecycleService).cancelLockedOrder(
                order,
                CancellationReason.PAYMENT_REJECTED,
                USER_ID
        );
        verify(orderRepository, never()).save(any(OrderEntity.class));
        verify(paymentRepository, org.mockito.Mockito.times(2))
                .save(any(PaymentEntity.class));
    }

    @Test
    void pay_whenApproved_shouldMarkOrderPaidWithoutCancelling() {
        OrderEntity order = order(OrderStatus.PENDING_PAYMENT,
                LocalDateTime.now().plusMinutes(10));
        PaymentRequestDTO request = requestFor(order);

        when(orderRepository.findByExternalIdForUpdate(order.getExternalId()))
                .thenReturn(Optional.of(order));
        when(paymentRepository.save(any(PaymentEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        doReturn(PaymentStatus.APPROVED).when(paymentService).simulateCard(any());

        PaymentResponseDTO response = paymentService.pay(request, USERNAME, false);

        assertThat(order.getStatus()).isEqualTo(OrderStatus.PAID);
        assertThat(response.status()).isEqualTo(PaymentStatus.APPROVED);
        verify(orderRepository).save(order);
        verifyNoInteractions(orderLifecycleService);
        verify(emailService).sendOrderStatusChangedEmail(
                eq(USERNAME),
                eq("Ana"),
                any(OrderResponseDTO.class),
                eq("PAID")
        );
    }

    @Test
    void pay_whenOrderBelongsToAnotherUser_shouldRejectBeforePayment() {
        OrderEntity order = order(OrderStatus.PENDING_PAYMENT,
                LocalDateTime.now().plusMinutes(10));
        order.setUser(UserEntity.builder()
                .id(99L)
                .email("other@test.com")
                .name("Other")
                .lastName("User")
                .build());
        PaymentRequestDTO request = requestFor(order);
        when(orderRepository.findByExternalIdForUpdate(order.getExternalId()))
                .thenReturn(Optional.of(order));

        assertThatThrownBy(() -> paymentService.pay(request, USERNAME, false))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessage("You are not authorized to pay this order");

        verify(paymentRepository, never()).save(any(PaymentEntity.class));
        verifyNoInteractions(orderLifecycleService);
    }

    private OrderEntity order(OrderStatus status, LocalDateTime expiresAt) {
        return OrderEntity.builder()
                .id(1L)
                .externalId(UUID.randomUUID())
                .status(status)
                .expiresAt(expiresAt)
                .total(new BigDecimal("100.00"))
                .user(user)
                .build();
    }

    private PaymentRequestDTO requestFor(OrderEntity order) {
        return PaymentRequestDTO.builder()
                .orderId(order.getExternalId())
                .paymentMethod(PaymentMethod.CREDIT_CARD)
                .build();
    }
}
