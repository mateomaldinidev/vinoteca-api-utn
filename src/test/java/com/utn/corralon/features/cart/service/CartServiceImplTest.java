package com.utn.corralon.features.cart.service;

import com.utn.corralon.exception.BadRequestException;
import com.utn.corralon.exception.BusinessRuleException;
import com.utn.corralon.exception.ResourceNotFoundException;
import com.utn.corralon.features.cart.dto.CartRequestDTO;
import com.utn.corralon.features.cart.dto.CartResponseDTO;
import com.utn.corralon.features.cart.dto.CheckoutResultDTO;
import com.utn.corralon.features.cart.entity.CartEntity;
import com.utn.corralon.features.cart.repository.CartRepository;
import com.utn.corralon.features.cart_item.dto.CartItemQuantityUpdateDTO;
import com.utn.corralon.features.cart_item.dto.CartItemRequestDTO;
import com.utn.corralon.features.cart_item.dto.CartItemResponseDTO;
import com.utn.corralon.features.cart_item.entity.CartItemEntity;
import com.utn.corralon.features.order.dto.OrderResponseDTO;
import com.utn.corralon.features.order.entity.OrderEntity;
import com.utn.corralon.features.order.enums.DeliveryType;
import com.utn.corralon.features.order.enums.OrderStatus;
import com.utn.corralon.features.order.mapper.OrderMapper;
import com.utn.corralon.features.order.repository.OrderRepository;
import com.utn.corralon.features.order.service.OrderService;
import com.utn.corralon.features.order.util.CheckoutCanonicalPayload;
import com.utn.corralon.features.pricing.service.PricingService;
import com.utn.corralon.features.product.entity.ProductEntity;
import com.utn.corralon.features.productVariant.entity.ProductVariantEntity;
import com.utn.corralon.features.productVariant.repository.ProductVariantRepository;
import com.utn.corralon.features.user.entity.UserEntity;
import com.utn.corralon.features.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CartServiceImplTest {

    private static final String USER_EMAIL = "user@test.com";

    @Mock
    private CartRepository cartRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ProductVariantRepository productVariantRepository;

    @Mock
    private ModelMapper modelMapper;

    @Mock
    private OrderService orderService;

    @Mock
    private PricingService pricingService;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OrderMapper orderMapper;

    @Mock
    private PlatformTransactionManager transactionManager;

    @InjectMocks
    private CartServiceImpl cartService;

    private UserEntity user;

    @BeforeEach
    void setUp() {
        user = UserEntity.builder()
                .id(1L)
                .externalId(UUID.randomUUID())
                .email(USER_EMAIL)
                .name("Usuario")
                .lastName("Prueba")
                .password("secret")
                .active(true)
                .build();

        TransactionTemplate synchronousTemplate = new TransactionTemplate() {
            @Override
            public <T> T execute(TransactionCallback<T> action) {
                return action.doInTransaction(null);
            }
        };
        cartService.setTransactionTemplate(synchronousTemplate);
    }

    @Test
    void createOrUpdateCart_shouldCreateCartAndExposeUserAndCartIdentifiers() {
        UUID variantId = UUID.randomUUID();
        ProductVariantEntity variant = variant(variantId, "750ml", "100.00", 10, true);

        when(userRepository.findByEmail(USER_EMAIL)).thenReturn(Optional.of(user));
        when(cartRepository.findByUser(user)).thenReturn(Optional.empty());
        when(productVariantRepository.findByExternalId(variantId)).thenReturn(Optional.of(variant));
        when(cartRepository.save(any(CartEntity.class))).thenAnswer(invocation -> {
            CartEntity savedCart = invocation.getArgument(0, CartEntity.class);
            if (savedCart.getExternalId() == null) {
                savedCart.setExternalId(UUID.randomUUID());
            }
            if (savedCart.getLastUpdated() == null) {
                savedCart.setLastUpdated(LocalDateTime.now());
            }
            return savedCart;
        });
        stubModelMapper();
        when(pricingService.calculateUnitPrice(variant, 2)).thenReturn(new BigDecimal("100.00"));

        CartRequestDTO request = CartRequestDTO.builder()
                .items(List.of(itemRequest(variantId, 2)))
                .build();

        CartResponseDTO response = cartService.createOrUpdateCart(USER_EMAIL, request);

        assertThat(response.getExternalId()).isNotNull();
        assertThat(response.getUserExternalId()).isEqualTo(user.getExternalId());
        assertThat(response.getLastUpdated()).isNotNull();
        assertThat(response.getCartItems()).hasSize(1);

        CartItemResponseDTO responseItem = response.getCartItems().get(0);
        assertThat(responseItem.getCartExternalId()).isEqualTo(response.getExternalId());
        assertThat(responseItem.getProductVariantExternalId()).isEqualTo(variantId);
        assertThat(responseItem.getQuantity()).isEqualTo(2);
        assertThat(responseItem.getUnitPrice()).isEqualByComparingTo("100.00");
        assertThat(response.getTotalAmount()).isEqualByComparingTo("200.00");

        verify(cartRepository).save(any(CartEntity.class));
    }

    @Test
    void createOrUpdateCart_shouldRejectDuplicatedVariantsWithBadRequest() {
        UUID variantId = UUID.randomUUID();

        when(userRepository.findByEmail(USER_EMAIL)).thenReturn(Optional.of(user));

        CartRequestDTO request = CartRequestDTO.builder()
                .items(List.of(
                        itemRequest(variantId, 1),
                        itemRequest(variantId, 2)
                ))
                .build();

        assertThatThrownBy(() -> cartService.createOrUpdateCart(USER_EMAIL, request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Duplicate product variant ID");

        verifyNoInteractions(cartRepository, productVariantRepository, pricingService);
    }

    @Test
    void updateCartItemQuantity_shouldUpdateOnlyRequestedItem() {
        ProductVariantEntity variantA = variant(UUID.randomUUID(), "A", "100.00", 10, true);
        ProductVariantEntity variantB = variant(UUID.randomUUID(), "B", "50.00", 10, true);
        CartEntity cart = cart(List.of(
                cartItem(variantA, 1),
                cartItem(variantB, 2)
        ));

        when(userRepository.findByEmail(USER_EMAIL)).thenReturn(Optional.of(user));
        when(cartRepository.findByUser(user)).thenReturn(Optional.of(cart));
        when(cartRepository.save(cart)).thenReturn(cart);
        stubModelMapper();
        when(pricingService.calculateUnitPrice(variantA, 3)).thenReturn(new BigDecimal("90.00"));
        when(pricingService.calculateUnitPrice(variantB, 2)).thenReturn(new BigDecimal("50.00"));

        CartResponseDTO response = cartService.updateCartItemQuantity(
                USER_EMAIL,
                variantA.getExternalId(),
                new CartItemQuantityUpdateDTO(3));

        assertThat(response.getCartItems()).hasSize(2);
        assertThat(quantityOf(cart, variantA)).isEqualTo(3);
        assertThat(quantityOf(cart, variantB)).isEqualTo(2);
        assertThat(response.getTotalAmount()).isEqualByComparingTo("370.00");

        verify(cartRepository).save(cart);
    }

    @Test
    void removeCartItem_shouldRemoveOnlyRequestedVariantAndKeepOtherItems() {
        ProductVariantEntity removedVariant = variant(UUID.randomUUID(), "A", "100.00", 10, true);
        ProductVariantEntity keptVariant = variant(UUID.randomUUID(), "B", "50.00", 10, true);
        CartItemEntity removedItem = cartItem(removedVariant, 1);
        CartEntity cart = cart(List.of(
                removedItem,
                cartItem(keptVariant, 2)
        ));

        when(userRepository.findByEmail(USER_EMAIL)).thenReturn(Optional.of(user));
        when(cartRepository.findByUser(user)).thenReturn(Optional.of(cart));
        when(cartRepository.save(cart)).thenReturn(cart);
        stubModelMapper();
        when(pricingService.calculateUnitPrice(keptVariant, 2)).thenReturn(new BigDecimal("50.00"));

        CartResponseDTO response = cartService.removeCartItem(
                USER_EMAIL,
                removedVariant.getExternalId());

        assertThat(cart.getCartItems()).hasSize(1);
        assertThat(cart.getCartItems().get(0).getProductVariant().getExternalId())
                .isEqualTo(keptVariant.getExternalId());
        assertThat(removedItem.getCart()).isNull();

        assertThat(response.getCartItems()).hasSize(1);
        CartItemResponseDTO responseItem = response.getCartItems().get(0);
        assertThat(responseItem.getProductVariantExternalId())
                .isEqualTo(keptVariant.getExternalId());
        assertThat(responseItem.getCartExternalId()).isEqualTo(cart.getExternalId());
        assertThat(response.getTotalAmount()).isEqualByComparingTo("100.00");

        verify(cartRepository).save(cart);
    }

    @Test
    void removeCartItem_shouldNotRemoveItemBelongingToAnotherUserCart() {
        ProductVariantEntity currentUserVariant = variant(UUID.randomUUID(), "A", "100.00", 10, true);
        ProductVariantEntity otherUserVariant = variant(UUID.randomUUID(), "B", "50.00", 10, true);
        CartEntity currentUserCart = cart(List.of(cartItem(currentUserVariant, 1)));

        when(userRepository.findByEmail(USER_EMAIL)).thenReturn(Optional.of(user));
        when(cartRepository.findByUser(user)).thenReturn(Optional.of(currentUserCart));

        assertThatThrownBy(() -> cartService.removeCartItem(
                USER_EMAIL,
                otherUserVariant.getExternalId()))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining(otherUserVariant.getExternalId().toString());

        assertThat(currentUserCart.getCartItems()).hasSize(1);
        assertThat(currentUserCart.getCartItems().get(0).getProductVariant().getExternalId())
                .isEqualTo(currentUserVariant.getExternalId());

        verify(cartRepository, never()).save(any(CartEntity.class));
    }

    @Test
    void removeCartItem_shouldThrowNotFoundWhenVariantIsNotInCart() {
        ProductVariantEntity variant = variant(UUID.randomUUID(), "A", "100.00", 10, true);
        CartEntity cart = cart(List.of(cartItem(variant, 1)));
        UUID missingVariantId = UUID.randomUUID();

        when(userRepository.findByEmail(USER_EMAIL)).thenReturn(Optional.of(user));
        when(cartRepository.findByUser(user)).thenReturn(Optional.of(cart));

        assertThatThrownBy(() -> cartService.removeCartItem(USER_EMAIL, missingVariantId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining(missingVariantId.toString());

        assertThat(cart.getCartItems()).hasSize(1);

        verify(cartRepository, never()).save(any(CartEntity.class));
    }

    private CartItemRequestDTO itemRequest(UUID productVariantId, int quantity) {
        return CartItemRequestDTO.builder()
                .productVariantId(productVariantId)
                .quantity(quantity)
                .build();
    }

    private ProductVariantEntity variant(
            UUID externalId,
            String attribute,
            String price,
            int stock,
            boolean active) {

        ProductEntity product = ProductEntity.builder()
                .externalId(UUID.randomUUID())
                .name("Producto " + attribute)
                .active(true)
                .build();

        return ProductVariantEntity.builder()
                .externalId(externalId)
                .attribute(attribute)
                .price(new BigDecimal(price))
                .stock(stock)
                .active(active)
                .product(product)
                .build();
    }

    private CartItemEntity cartItem(ProductVariantEntity variant, int quantity) {
        return CartItemEntity.builder()
                .externalId(UUID.randomUUID())
                .productVariant(variant)
                .quantity(quantity)
                .createdAt(LocalDateTime.now())
                .build();
    }

    private CartEntity cart(List<CartItemEntity> items) {
        CartEntity cart = CartEntity.builder()
                .externalId(UUID.randomUUID())
                .user(user)
                .lastUpdated(LocalDateTime.now())
                .cartItems(new ArrayList<>(items))
                .build();

        items.forEach(item -> item.setCart(cart));

        return cart;
    }

    private Integer quantityOf(CartEntity cart, ProductVariantEntity variant) {
        return cart.getCartItems().stream()
                .filter(item -> item.getProductVariant().getExternalId()
                        .equals(variant.getExternalId()))
                .findFirst()
                .map(CartItemEntity::getQuantity)
                .orElse(null);
    }

    private void stubModelMapper() {
        when(modelMapper.map(any(CartEntity.class), eq(CartResponseDTO.class)))
                .thenAnswer(invocation -> new CartResponseDTO());

        when(modelMapper.map(any(CartItemEntity.class), eq(CartItemResponseDTO.class)))
                .thenAnswer(invocation -> {
                    CartItemEntity item = invocation.getArgument(0, CartItemEntity.class);
                    CartItemResponseDTO response = new CartItemResponseDTO();
                    response.setExternalId(item.getExternalId());
                    response.setQuantity(item.getQuantity());
                    response.setCreatedAt(item.getCreatedAt());
                    return response;
                });
    }

    @Test
    void checkout_whenValidNewOrder_shouldCreateOrderEmptyCartAndReturnCreated() {
        UUID addressId = UUID.randomUUID();
        String idempotencyKey = "key-checkout-001";
        String expectedHash = CheckoutCanonicalPayload.computeHash(DeliveryType.DELIVERY, addressId);

        UUID var1Id = UUID.fromString("00000000-0000-0000-0000-000000000002");
        UUID var2Id = UUID.fromString("00000000-0000-0000-0000-000000000001");

        ProductVariantEntity var1 = variant(var1Id, "750ml", "100.00", 10, true);
        ProductVariantEntity var2 = variant(var2Id, "1500ml", "200.00", 5, true);

        CartEntity cart = CartEntity.builder()
                .id(10L)
                .externalId(UUID.randomUUID())
                .user(user)
                .cartItems(new ArrayList<>(List.of(
                        CartItemEntity.builder().id(101L).productVariant(var1).quantity(2).build(),
                        CartItemEntity.builder().id(102L).productVariant(var2).quantity(1).build()
                )))
                .build();

        OrderResponseDTO createdOrderDTO = new OrderResponseDTO();
        createdOrderDTO.setExternalId(UUID.randomUUID());

        when(userRepository.findByEmail(USER_EMAIL)).thenReturn(Optional.of(user));
        when(orderRepository.findByUserAndIdempotencyKey(user, idempotencyKey)).thenReturn(Optional.empty());
        when(cartRepository.findByUserForUpdate(user)).thenReturn(Optional.of(cart));
        when(productVariantRepository.findByExternalIdForUpdate(var1Id)).thenReturn(Optional.of(var1));
        when(productVariantRepository.findByExternalIdForUpdate(var2Id)).thenReturn(Optional.of(var2));
        when(orderService.createFromCart(cart, addressId, DeliveryType.DELIVERY, idempotencyKey, expectedHash))
                .thenReturn(createdOrderDTO);

        CheckoutResultDTO result = cartService.checkout(USER_EMAIL, addressId, DeliveryType.DELIVERY, idempotencyKey);

        assertThat(result).isNotNull();
        assertThat(result.isReplay()).isFalse();
        assertThat(result.getOrder()).isEqualTo(createdOrderDTO);
        assertThat(cart.getCartItems()).isEmpty();
        verify(cartRepository).save(cart);

        // Verifica que los locks pesimistas se solicitaron en orden deterministico (var2Id < var1Id)
        InOrder inOrder = Mockito.inOrder(productVariantRepository);
        inOrder.verify(productVariantRepository).findByExternalIdForUpdate(var2Id);
        inOrder.verify(productVariantRepository).findByExternalIdForUpdate(var1Id);
    }

    @Test
    void checkout_whenReplayWithSameKeyAndSameParams_shouldReturnOriginalOrderWithoutDeductingStockOrTouchingCart() {
        UUID addressId = UUID.randomUUID();
        String idempotencyKey = "key-replay-001";
        String requestHash = CheckoutCanonicalPayload.computeHash(DeliveryType.DELIVERY, addressId);

        OrderEntity existingOrder = OrderEntity.builder()
                .id(55L)
                .externalId(UUID.randomUUID())
                .user(user)
                .idempotencyKey(idempotencyKey)
                .checkoutRequestHash(requestHash)
                .status(OrderStatus.PENDING_PAYMENT)
                .build();

        OrderResponseDTO existingDTO = new OrderResponseDTO();
        existingDTO.setExternalId(existingOrder.getExternalId());

        when(userRepository.findByEmail(USER_EMAIL)).thenReturn(Optional.of(user));
        when(orderRepository.findByUserAndIdempotencyKey(user, idempotencyKey)).thenReturn(Optional.of(existingOrder));
        when(orderMapper.toResponseDTO(existingOrder)).thenReturn(existingDTO);

        CheckoutResultDTO result = cartService.checkout(USER_EMAIL, addressId, DeliveryType.DELIVERY, idempotencyKey);

        assertThat(result).isNotNull();
        assertThat(result.isReplay()).isTrue();
        assertThat(result.getOrder()).isEqualTo(existingDTO);

        verify(cartRepository, never()).findByUserForUpdate(any());
        verify(productVariantRepository, never()).findByExternalIdForUpdate(any());
        verify(orderService, never()).createFromCart(any(), any(), any(), any(), any());
    }

    @Test
    void checkout_whenSameKeyWithDifferentParameters_shouldThrowConflict409() {
        UUID originalAddressId = UUID.randomUUID();
        String idempotencyKey = "key-conflict-001";
        String originalHash = CheckoutCanonicalPayload.computeHash(DeliveryType.DELIVERY, originalAddressId);

        OrderEntity existingOrder = OrderEntity.builder()
                .id(55L)
                .externalId(UUID.randomUUID())
                .user(user)
                .idempotencyKey(idempotencyKey)
                .checkoutRequestHash(originalHash)
                .build();

        when(userRepository.findByEmail(USER_EMAIL)).thenReturn(Optional.of(user));
        when(orderRepository.findByUserAndIdempotencyKey(user, idempotencyKey)).thenReturn(Optional.of(existingOrder));

        // Intento con diferente tipo de entrega (PICKUP sin direccion)
        assertThatThrownBy(() -> cartService.checkout(USER_EMAIL, null, DeliveryType.PICKUP, idempotencyKey))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Idempotency-Key already used with different");

        verify(orderService, never()).createFromCart(any(), any(), any(), any(), any());
    }

    @Test
    void checkout_whenReplayAndCartAlreadyEmpty_shouldReturnOriginalOrder() {
        String idempotencyKey = "key-empty-cart-replay";
        String requestHash = CheckoutCanonicalPayload.computeHash(DeliveryType.PICKUP, null);

        OrderEntity existingOrder = OrderEntity.builder()
                .id(88L)
                .externalId(UUID.randomUUID())
                .user(user)
                .idempotencyKey(idempotencyKey)
                .checkoutRequestHash(requestHash)
                .build();

        OrderResponseDTO existingDTO = new OrderResponseDTO();
        existingDTO.setExternalId(existingOrder.getExternalId());

        when(userRepository.findByEmail(USER_EMAIL)).thenReturn(Optional.of(user));
        when(orderRepository.findByUserAndIdempotencyKey(user, idempotencyKey)).thenReturn(Optional.of(existingOrder));
        when(orderMapper.toResponseDTO(existingOrder)).thenReturn(existingDTO);

        CheckoutResultDTO result = cartService.checkout(USER_EMAIL, null, DeliveryType.PICKUP, idempotencyKey);

        assertThat(result).isNotNull();
        assertThat(result.isReplay()).isTrue();
        assertThat(result.getOrder()).isEqualTo(existingDTO);
        verify(cartRepository, never()).findByUserForUpdate(any());
    }

    @Test
    void checkout_whenNewKeyAndCartIsEmpty_shouldThrowCartEmptyConflict() {
        String idempotencyKey = "key-new-empty-cart";

        CartEntity emptyCart = CartEntity.builder()
                .id(10L)
                .user(user)
                .cartItems(new ArrayList<>())
                .build();

        when(userRepository.findByEmail(USER_EMAIL)).thenReturn(Optional.of(user));
        when(orderRepository.findByUserAndIdempotencyKey(user, idempotencyKey)).thenReturn(Optional.empty());
        when(cartRepository.findByUserForUpdate(user)).thenReturn(Optional.of(emptyCart));

        assertThatThrownBy(() -> cartService.checkout(USER_EMAIL, null, DeliveryType.PICKUP, idempotencyKey))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("Cart is empty");

        verify(orderService, never()).createFromCart(any(), any(), any(), any(), any());
    }

    @Test
    void checkout_whenConcurrentRequestCreatedOrderWhileWaitingForCartLock_shouldReturnExistingOrder() {
        UUID addressId = UUID.randomUUID();
        String idempotencyKey = "key-concurrent-after-lock";
        String requestHash = CheckoutCanonicalPayload.computeHash(DeliveryType.DELIVERY, addressId);

        // Simula que el primer request ya vacio el carrito y commiteo su orden
        // mientras este request esperaba el lock pesimista.
        CartEntity emptyCart = CartEntity.builder()
                .id(10L)
                .user(user)
                .cartItems(new ArrayList<>())
                .build();

        OrderEntity existingOrder = OrderEntity.builder()
                .id(77L)
                .externalId(UUID.randomUUID())
                .user(user)
                .idempotencyKey(idempotencyKey)
                .checkoutRequestHash(requestHash)
                .status(OrderStatus.PENDING_PAYMENT)
                .build();

        OrderResponseDTO existingDTO = new OrderResponseDTO();
        existingDTO.setExternalId(existingOrder.getExternalId());

        when(userRepository.findByEmail(USER_EMAIL)).thenReturn(Optional.of(user));
        when(orderRepository.findByUserAndIdempotencyKey(user, idempotencyKey))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.of(existingOrder));
        when(cartRepository.findByUserForUpdate(user)).thenReturn(Optional.of(emptyCart));
        when(orderMapper.toResponseDTO(existingOrder)).thenReturn(existingDTO);

        CheckoutResultDTO result = cartService.checkout(USER_EMAIL, addressId, DeliveryType.DELIVERY, idempotencyKey);

        assertThat(result.isReplay()).isTrue();
        assertThat(result.getOrder()).isEqualTo(existingDTO);

        verify(orderService, never()).createFromCart(any(), any(), any(), any(), any());
        verify(productVariantRepository, never()).findByExternalIdForUpdate(any());
        verify(cartRepository, never()).save(any(CartEntity.class));
    }

    @Test
    void checkout_whenStockInsufficient_shouldPropagateExceptionAndRollback() {
        UUID addressId = UUID.randomUUID();
        String idempotencyKey = "key-stock-insufficient";
        String expectedHash = CheckoutCanonicalPayload.computeHash(DeliveryType.DELIVERY, addressId);

        UUID varId = UUID.randomUUID();
        ProductVariantEntity var = variant(varId, "750ml", "100.00", 1, true);

        CartEntity cart = CartEntity.builder()
                .id(10L)
                .user(user)
                .cartItems(new ArrayList<>(List.of(
                        CartItemEntity.builder().id(101L).productVariant(var).quantity(5).build()
                )))
                .build();

        when(userRepository.findByEmail(USER_EMAIL)).thenReturn(Optional.of(user));
        when(orderRepository.findByUserAndIdempotencyKey(user, idempotencyKey)).thenReturn(Optional.empty());
        when(cartRepository.findByUserForUpdate(user)).thenReturn(Optional.of(cart));
        when(productVariantRepository.findByExternalIdForUpdate(varId)).thenReturn(Optional.of(var));
        when(orderService.createFromCart(cart, addressId, DeliveryType.DELIVERY, idempotencyKey, expectedHash))
                .thenThrow(new BusinessRuleException("Insufficient stock"));

        assertThatThrownBy(() -> cartService.checkout(USER_EMAIL, addressId, DeliveryType.DELIVERY, idempotencyKey))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("Insufficient stock");

        assertThat(cart.getCartItems()).isNotEmpty();
        verify(cartRepository, never()).save(cart);
    }

    @Test
    void checkout_whenConcurrentRaceOnUniqueKey_shouldCatchConstraintAndReturnExistingOrder() {
        UUID addressId = UUID.randomUUID();
        String idempotencyKey = "key-race-001";
        String requestHash = CheckoutCanonicalPayload.computeHash(DeliveryType.DELIVERY, addressId);

        OrderEntity existingOrder = OrderEntity.builder()
                .id(99L)
                .externalId(UUID.randomUUID())
                .user(user)
                .idempotencyKey(idempotencyKey)
                .checkoutRequestHash(requestHash)
                .build();

        OrderResponseDTO existingDTO = new OrderResponseDTO();
        existingDTO.setExternalId(existingOrder.getExternalId());

        when(userRepository.findByEmail(USER_EMAIL)).thenReturn(Optional.of(user));
        // Primera busqueda: no existe todavia (carrera)
        // Segunda busqueda (post DataIntegrityViolationException): ya fue commiteada por el otro hilo
        when(orderRepository.findByUserAndIdempotencyKey(user, idempotencyKey))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.of(existingOrder));

        when(orderMapper.toResponseDTO(existingOrder)).thenReturn(existingDTO);

        // Simulamos que la transaccion de checkout falla por la UNIQUE constraint.
        // La relectura posterior (nueva transaccion) debe poder ejecutarse.
        AtomicInteger transactionAttempts = new AtomicInteger(0);
        TransactionTemplate failingTemplate = new TransactionTemplate() {
            @Override
            public <T> T execute(TransactionCallback<T> action) {
                if (transactionAttempts.getAndIncrement() == 0) {
                    throw new DataIntegrityViolationException(
                            "Duplicate entry '1-key-race-001' for key 'uk_orders_user_idempotency'");
                }
                return action.doInTransaction(null);
            }
        };
        cartService.setTransactionTemplate(failingTemplate);

        CheckoutResultDTO result = cartService.checkout(USER_EMAIL, addressId, DeliveryType.DELIVERY, idempotencyKey);

        assertThat(result).isNotNull();
        assertThat(result.isReplay()).isTrue();
        assertThat(result.getOrder()).isEqualTo(existingDTO);
    }

    @Test
    void checkout_whenTwoConcurrentRequestsWithSameKey_shouldSafelyProduceOnlyOneCreatedAndOneReplay() throws Exception {
        UUID addressId = UUID.randomUUID();
        String idempotencyKey = "key-concurrent-multi";
        String requestHash = CheckoutCanonicalPayload.computeHash(DeliveryType.DELIVERY, addressId);

        UUID varId = UUID.randomUUID();
        ProductVariantEntity var = variant(varId, "750ml", "100.00", 10, true);

        CartEntity cart = CartEntity.builder()
                .id(10L)
                .user(user)
                .cartItems(new ArrayList<>(List.of(
                        CartItemEntity.builder().id(101L).productVariant(var).quantity(2).build()
                )))
                .build();

        OrderEntity savedOrder = OrderEntity.builder()
                .id(100L)
                .externalId(UUID.randomUUID())
                .user(user)
                .idempotencyKey(idempotencyKey)
                .checkoutRequestHash(requestHash)
                .build();

        OrderResponseDTO orderDTO = new OrderResponseDTO();
        orderDTO.setExternalId(savedOrder.getExternalId());

        AtomicInteger executionCounter = new AtomicInteger(0);

        when(userRepository.findByEmail(USER_EMAIL)).thenReturn(Optional.of(user));
        when(cartRepository.findByUserForUpdate(user)).thenReturn(Optional.of(cart));
        when(productVariantRepository.findByExternalIdForUpdate(varId)).thenReturn(Optional.of(var));
        when(orderMapper.toResponseDTO(savedOrder)).thenReturn(orderDTO);

        when(orderRepository.findByUserAndIdempotencyKey(user, idempotencyKey)).thenAnswer(inv -> {
            if (executionCounter.get() > 0) {
                return Optional.of(savedOrder);
            }
            return Optional.empty();
        });

        when(orderService.createFromCart(any(), any(), any(), any(), any())).thenAnswer(inv -> {
            executionCounter.incrementAndGet();
            return orderDTO;
        });

        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch startLatch = new CountDownLatch(1);

        Future<CheckoutResultDTO> future1 = executor.submit(() -> {
            startLatch.await();
            return cartService.checkout(USER_EMAIL, addressId, DeliveryType.DELIVERY, idempotencyKey);
        });

        Future<CheckoutResultDTO> future2 = executor.submit(() -> {
            startLatch.await();
            // Leve defer para sincronizar con la ejecucion del primero
            Thread.sleep(10);
            return cartService.checkout(USER_EMAIL, addressId, DeliveryType.DELIVERY, idempotencyKey);
        });

        startLatch.countDown();

        CheckoutResultDTO res1 = future1.get(5, TimeUnit.SECONDS);
        CheckoutResultDTO res2 = future2.get(5, TimeUnit.SECONDS);

        executor.shutdown();

        // Exactamente una orden creada y la otra recuperada por replay
        List<CheckoutResultDTO> results = List.of(res1, res2);
        long replayCount = results.stream().filter(CheckoutResultDTO::isReplay).count();
        long newCount = results.stream().filter(r -> !r.isReplay()).count();

        assertThat(newCount).isEqualTo(1);
        assertThat(replayCount).isEqualTo(1);
        assertThat(executionCounter.get()).isEqualTo(1);
        verify(orderService, Mockito.times(1)).createFromCart(any(), any(), any(), any(), any());
    }

    @Test
    void checkout_whenInvalidIdempotencyKey_shouldThrowBadRequest() {
        // Header nulo
        assertThatThrownBy(() -> cartService.checkout(USER_EMAIL, null, DeliveryType.PICKUP, null))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Idempotency-Key header is required");

        // Header vacio
        assertThatThrownBy(() -> cartService.checkout(USER_EMAIL, null, DeliveryType.PICKUP, "   "))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Idempotency-Key header is required");

        // Header mayor a 255 caracteres
        String longKey = "a".repeat(256);
        assertThatThrownBy(() -> cartService.checkout(USER_EMAIL, null, DeliveryType.PICKUP, longKey))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Idempotency-Key exceeds maximum length of 255 characters");
    }
}
