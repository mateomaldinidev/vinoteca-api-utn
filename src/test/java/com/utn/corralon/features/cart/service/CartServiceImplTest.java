package com.utn.corralon.features.cart.service;

import com.utn.corralon.exception.BadRequestException;
import com.utn.corralon.exception.ResourceNotFoundException;
import com.utn.corralon.features.cart.dto.CartRequestDTO;
import com.utn.corralon.features.cart.dto.CartResponseDTO;
import com.utn.corralon.features.cart.entity.CartEntity;
import com.utn.corralon.features.cart.repository.CartRepository;
import com.utn.corralon.features.cart_item.dto.CartItemQuantityUpdateDTO;
import com.utn.corralon.features.cart_item.dto.CartItemRequestDTO;
import com.utn.corralon.features.cart_item.dto.CartItemResponseDTO;
import com.utn.corralon.features.cart_item.entity.CartItemEntity;
import com.utn.corralon.features.order.service.OrderService;
import com.utn.corralon.features.pricing.service.PricingService;
import com.utn.corralon.features.product.entity.ProductEntity;
import com.utn.corralon.features.productVariant.entity.ProductVariantEntity;
import com.utn.corralon.features.productVariant.repository.ProductVariantRepository;
import com.utn.corralon.features.user.entity.UserEntity;
import com.utn.corralon.features.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

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
}
