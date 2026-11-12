package com.utn.corralon.features.order.service;

import com.utn.corralon.exception.ResourceNotFoundException;
import com.utn.corralon.exception.UnauthorizedException;
import com.utn.corralon.features.address.repository.AddressRepository;
import com.utn.corralon.features.auth.CredentialsEntity;
import com.utn.corralon.features.auth.CredentialsRepository;
import com.utn.corralon.features.auth.RoleEntity;
import com.utn.corralon.features.auth.Roles;
import com.utn.corralon.features.cart.repository.CartRepository;
import com.utn.corralon.features.notifications.service.IEmailService;
import com.utn.corralon.features.order.enums.CancellationReason;
import com.utn.corralon.features.order.mapper.OrderMapper;
import com.utn.corralon.features.order.repository.OrderRepository;
import com.utn.corralon.features.orderItem.mapper.OrderItemMapper;
import com.utn.corralon.features.pricing.service.PricingService;
import com.utn.corralon.features.stockMovement.service.IStockService;
import com.utn.corralon.features.user.entity.UserEntity;
import com.utn.corralon.features.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderServiceCancellationTest {

    private static final String USERNAME = "user@test.com";
    private static final long USER_ID = 7L;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OrderMapper orderMapper;

    @Mock
    private OrderItemMapper orderItemMapper;

    @Mock
    private UserRepository userRepository;

    @Mock
    private CredentialsRepository credentialsRepository;

    @Mock
    private AddressRepository addressRepository;

    @Mock
    private CartRepository cartRepository;

    @Mock
    private IStockService stockService;

    @Mock
    private IEmailService emailService;

    @Mock
    private PricingService pricingService;

    @Mock
    private IOrderLifecycleService orderLifecycleService;

    @InjectMocks
    private OrderService orderService;

    private UUID orderExternalId;
    private UserEntity user;

    @BeforeEach
    void setUp() {
        orderExternalId = UUID.randomUUID();
        user = UserEntity.builder()
                .id(USER_ID)
                .email(USERNAME)
                .name("Ana")
                .lastName("Perez")
                .build();
    }

    @Test
    void cancelOrder_whenCustomerCancelsOwnOrder_shouldUseUserCancelledReason() {
        when(orderRepository.findOwnerEmailByExternalId(orderExternalId))
                .thenReturn(Optional.of(USERNAME));
        when(credentialsRepository.findByUsername(USERNAME))
                .thenReturn(Optional.of(credentials(Roles.ROLE_CUSTOMER)));
        when(userRepository.findByEmail(USERNAME)).thenReturn(Optional.of(user));

        orderService.cancelOrder(orderExternalId, USERNAME);

        verify(orderLifecycleService).cancelPendingOrder(
                orderExternalId,
                CancellationReason.USER_CANCELLED,
                USER_ID
        );
    }

    @Test
    void cancelOrder_whenAdminCancels_shouldUseAdminCancelledReason() {
        when(orderRepository.findOwnerEmailByExternalId(orderExternalId))
                .thenReturn(Optional.of("customer@test.com"));
        when(credentialsRepository.findByUsername(USERNAME))
                .thenReturn(Optional.of(credentials(Roles.ROLE_ADMIN)));
        when(userRepository.findByEmail(USERNAME)).thenReturn(Optional.of(user));

        orderService.cancelOrder(orderExternalId, USERNAME);

        verify(orderLifecycleService).cancelPendingOrder(
                orderExternalId,
                CancellationReason.ADMIN_CANCELLED,
                USER_ID
        );
    }

    @Test
    void cancelOrder_whenEmployeeCancels_shouldUseAdminCancelledReason() {
        when(orderRepository.findOwnerEmailByExternalId(orderExternalId))
                .thenReturn(Optional.of("customer@test.com"));
        when(credentialsRepository.findByUsername(USERNAME))
                .thenReturn(Optional.of(credentials(Roles.ROLE_EMPLOYEE)));
        when(userRepository.findByEmail(USERNAME)).thenReturn(Optional.of(user));

        orderService.cancelOrder(orderExternalId, USERNAME);

        verify(orderLifecycleService).cancelPendingOrder(
                orderExternalId,
                CancellationReason.ADMIN_CANCELLED,
                USER_ID
        );
    }

    @Test
    void cancelOrder_whenCustomerCancelsAnotherUsersOrder_shouldRejectWithoutDelegating() {
        when(orderRepository.findOwnerEmailByExternalId(orderExternalId))
                .thenReturn(Optional.of("other@test.com"));
        when(credentialsRepository.findByUsername(USERNAME))
                .thenReturn(Optional.of(credentials(Roles.ROLE_CUSTOMER)));

        assertThatThrownBy(() -> orderService.cancelOrder(orderExternalId, USERNAME))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessage("You can only cancel your own orders");

        verifyNoInteractions(orderLifecycleService);
    }

    @Test
    void cancelOrder_whenUserHasNoAllowedRole_shouldRejectWithoutDelegating() {
        when(orderRepository.findOwnerEmailByExternalId(orderExternalId))
                .thenReturn(Optional.of(USERNAME));
        when(credentialsRepository.findByUsername(USERNAME))
                .thenReturn(Optional.of(credentials(Roles.ROLE_GUEST)));

        assertThatThrownBy(() -> orderService.cancelOrder(orderExternalId, USERNAME))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessage("You are not allowed to cancel orders");

        verifyNoInteractions(orderLifecycleService);
    }

    @Test
    void cancelOrder_whenOrderDoesNotExist_shouldThrowNotFound() {
        when(orderRepository.findOwnerEmailByExternalId(orderExternalId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.cancelOrder(orderExternalId, USERNAME))
                .isInstanceOf(ResourceNotFoundException.class);

        verifyNoInteractions(orderLifecycleService);
    }

    private CredentialsEntity credentials(Roles... roles) {
        Set<RoleEntity> roleEntities = Arrays.stream(roles)
                .map(role -> RoleEntity.builder().role(role).build())
                .collect(Collectors.toCollection(HashSet::new));

        return CredentialsEntity.builder()
                .username(USERNAME)
                .roles(roleEntities)
                .build();
    }
}
