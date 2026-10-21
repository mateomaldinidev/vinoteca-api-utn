package com.utn.vinoteca.features.cart.service;

import com.utn.vinoteca.exception.BusinessRuleException;
import com.utn.vinoteca.exception.ResourceNotFoundException;
import com.utn.vinoteca.exception.StockInsufficientException;
import com.utn.vinoteca.features.cart.dto.CartRequestDTO;
import com.utn.vinoteca.features.cart.dto.CartResponseDTO;
import com.utn.vinoteca.features.cart.entity.CartEntity;
import com.utn.vinoteca.features.cart.repository.CartRepository;
import com.utn.vinoteca.features.cart_item.dto.CartItemRequestDTO;
import com.utn.vinoteca.features.cart_item.dto.CartItemResponseDTO;
import com.utn.vinoteca.features.cart_item.dto.CartItemQuantityUpdateDTO;
import com.utn.vinoteca.features.cart_item.entity.CartItemEntity;
import com.utn.vinoteca.features.pricing.service.PricingService;
import com.utn.vinoteca.features.order.dto.OrderResponseDTO;
import com.utn.vinoteca.features.order.enums.DeliveryType;
import com.utn.vinoteca.features.order.service.ICheckoutService;
import com.utn.vinoteca.features.wineVariant.entity.WineVariantEntity;
import com.utn.vinoteca.features.wineVariant.repository.WineVariantRepository;
import com.utn.vinoteca.features.user.entity.UserEntity;
import com.utn.vinoteca.features.user.repository.UserRepository;
import lombok.AllArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.Map;

@AllArgsConstructor

@Service
public class CartServiceImpl implements ICartService {
    private final CartRepository cartRepository;
    private final UserRepository userRepository;
    private final WineVariantRepository wineVariantRepository;
    private final ModelMapper modelMapper;
    private final ICheckoutService checkoutService;
    private final PricingService pricingService;

    @Transactional
    public CartResponseDTO createOrUpdateCart(CartRequestDTO cartRequest) {
        UserEntity user = getActiveUser(cartRequest.getUserId());

        CartEntity cart = cartRepository.findByUser(user)
                .orElseGet(() -> CartEntity.builder()
                        .user(user)
                        .cartItems(new ArrayList<>())
                        .build());

        // Mapear los ítems existentes del carrito para una búsqueda eficiente
        Map<UUID, CartItemEntity> existingItemsMap = cart.getCartItems().stream()
                .collect(Collectors.toMap(
                        item -> item.getWineVariant().getExternalId(),
                        item -> item
                ));

        // Mapear los ítems entrantes de la solicitud para una búsqueda eficiente
        Map<UUID, CartItemRequestDTO> incomingItemsMap = cartRequest.getItems().stream()
                .collect(Collectors.toMap(
                        CartItemRequestDTO::getWineVariantId,
                        item -> item
                ));

        // Lista para almacenar los ítems que deben ser eliminados del carrito
        List<CartItemEntity> itemsToRemove = new ArrayList<>();

        // Procesar ítems existentes: actualizar o marcar para eliminación
        for (CartItemEntity existingItem : new ArrayList<>(cart.getCartItems())) {
            UUID wineVariantId = existingItem.getWineVariant().getExternalId();
            CartItemRequestDTO incomingItemRequest = incomingItemsMap.get(wineVariantId);

            if (incomingItemRequest != null) {
                WineVariantEntity wineVariant = wineVariantRepository.findByExternalId(wineVariantId)
                        .orElseThrow(() -> new ResourceNotFoundException("WineVariant not found with ID: ", wineVariantId));

                // Validar si el vino está activo
                if (!wineVariant.getActive()) {
                    throw new BusinessRuleException("Wine variant with ID " + wineVariantId + " is not active and cannot be added to cart.");
                }

                if (wineVariant.getStock() < incomingItemRequest.getQuantity()) {
                    throw new StockInsufficientException(wineVariant.getExternalId().toString());
                }
                existingItem.setQuantity(incomingItemRequest.getQuantity());
                incomingItemsMap.remove(wineVariantId);
            } else {
                itemsToRemove.add(existingItem);
            }
        }

        //Eliminar ítems marcados
        for (CartItemEntity item : itemsToRemove) {
            cart.removeCartItem(item);
        }

        //Añadir nuevos ítems (los que quedan en incomingItemsMap)
        for (CartItemRequestDTO newItemRequest : incomingItemsMap.values()) {
            WineVariantEntity wineVariant = wineVariantRepository.findByExternalId(newItemRequest.getWineVariantId())
                    .orElseThrow(() -> new ResourceNotFoundException("WineVariant not found with ID:", newItemRequest.getWineVariantId()));

            // Validar si el vino está activo
            if (!wineVariant.getActive()) {
                throw new BusinessRuleException("Wine variant with ID " + newItemRequest.getWineVariantId() + " is not active and cannot be added to cart.");
            }

            if (wineVariant.getStock() < newItemRequest.getQuantity()) {
                throw new StockInsufficientException(wineVariant.getExternalId().toString());
            }

            CartItemEntity newCartItem = CartItemEntity.builder()
                    .wineVariant(wineVariant)
                    .quantity(newItemRequest.getQuantity())
                    .build();
            cart.addCartItem(newCartItem);
        }

        CartEntity savedCart = cartRepository.save(cart);

        return mapCartToResponseDTO(savedCart);
    }

    @Transactional
    public CartResponseDTO updateCartItemQuantity(UUID userExternalId, UUID wineVariantId, CartItemQuantityUpdateDTO updateDTO) {
        UserEntity user = getActiveUser(userExternalId);

        CartEntity cart = cartRepository.findByUser(user)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID:", user.getExternalId()));

        // Buscar el CartItemEntity específico
        CartItemEntity cartItemToUpdate = cart.getCartItems().stream()
                .filter(item -> item.getWineVariant().getExternalId().equals(wineVariantId))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Cart item for wine variant not found with ID: ", wineVariantId ));

        Integer newQuantity = updateDTO.getQuantity();

        // Si la nueva cantidad es 0, eliminar el ítem del carrito
        if (newQuantity <= 0) {
            cart.removeCartItem(cartItemToUpdate);
        } else {
            WineVariantEntity wineVariant = cartItemToUpdate.getWineVariant();

            // Validar si el vino está activo (aunque ya esté en el carrito, podría haberse desactivado)
            if (!wineVariant.getActive()) {
                throw new BusinessRuleException("Wine variant with ID " + wineVariantId + " is not active and cannot be updated in cart.");
            }

            // Validar stock para la nueva cantidad
            if (wineVariant.getStock() < newQuantity) {
                throw new StockInsufficientException(wineVariant.getExternalId().toString());
            }
            cartItemToUpdate.setQuantity(newQuantity);
        }

        CartEntity savedCart = cartRepository.save(cart);
        return mapCartToResponseDTO(savedCart);
    }


    public CartResponseDTO getCartByUserId(UUID userExternalId) {
        UserEntity user = getActiveUser(userExternalId);

        CartEntity cart = cartRepository.findByUser(user)
                .orElseThrow(() -> new ResourceNotFoundException("Cart not found with ID: ", userExternalId));

        return mapCartToResponseDTO(cart);
    }

    @Transactional
    public void clearCart(UUID userExternalId) {
        UserEntity user = getActiveUser(userExternalId);

        CartEntity cart = cartRepository.findByUser(user)
                .orElseThrow(() -> new ResourceNotFoundException("Cart not found with ID: ", userExternalId));

        if (!cart.getCartItems().isEmpty()) {
            cart.getCartItems().clear();
            cartRepository.save(cart);
        }
    }

    private CartResponseDTO mapCartToResponseDTO(CartEntity cart) {
        CartResponseDTO responseDTO = modelMapper.map(cart, CartResponseDTO.class);

        List<CartItemResponseDTO> itemDTOs = cart.getCartItems().stream()
                .map(cartItemEntity -> {
                    CartItemResponseDTO itemResponseDTO = modelMapper.map(cartItemEntity, CartItemResponseDTO.class);
                    itemResponseDTO.setWineName(cartItemEntity.getWineVariant().getWine().getName());
                    itemResponseDTO.setVariantAttribute(cartItemEntity.getWineVariant().getAttribute());

                    WineVariantEntity wineVariant = cartItemEntity.getWineVariant();
                    BigDecimal unitPrice = pricingService.calculateUnitPrice(
                            wineVariant,
                            cartItemEntity.getQuantity());

                    itemResponseDTO.setUnitPrice(unitPrice);

                    return itemResponseDTO;
                })
                .toList();
        responseDTO.setCartItems(itemDTOs);

        BigDecimal totalAmount = itemDTOs.stream()
                .map(item -> item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        responseDTO.setTotalAmount(totalAmount);

        return responseDTO;
    }

    private UserEntity getActiveUser(UUID userId) {
        UserEntity user = userRepository.findByExternalId(userId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("User not found with ID: ", userId)
                );

        if (!user.getActive()) {
            throw new BusinessRuleException("User is inactive");
        }

        return user;
    }
    public CartEntity getCartEntityByUserId(UUID userExternalId) {
        UserEntity user = getActiveUser(userExternalId);

        return cartRepository.findByUser(user)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Cart not found with ID: ", userExternalId));
    }

    @Transactional
    public OrderResponseDTO checkout(UUID userId, UUID addressId, DeliveryType deliveryType) {
        CartEntity cart = getCartEntityByUserId(userId);

        if(cart.getCartItems().isEmpty()) {
            throw new BusinessRuleException("Cart is empty");
        }

        OrderResponseDTO order = checkoutService.createFromCart(cart, addressId, deliveryType);

        cart.getCartItems().clear();
        cartRepository.save(cart);

        return order;
    }

}