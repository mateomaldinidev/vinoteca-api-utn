package com.utn.corralon.features.cart.service;

import com.utn.corralon.exception.BadRequestException;
import com.utn.corralon.exception.BusinessRuleException;
import com.utn.corralon.exception.ResourceNotFoundException;
import com.utn.corralon.exception.StockInsufficientException;
import com.utn.corralon.features.cart.dto.CartRequestDTO;
import com.utn.corralon.features.cart.dto.CartResponseDTO;
import com.utn.corralon.features.cart.dto.CheckoutResultDTO;
import com.utn.corralon.features.cart.entity.CartEntity;
import com.utn.corralon.features.cart.repository.CartRepository;
import com.utn.corralon.features.cart_item.dto.CartItemRequestDTO;
import com.utn.corralon.features.cart_item.dto.CartItemResponseDTO;
import com.utn.corralon.features.cart_item.dto.CartItemQuantityUpdateDTO;
import com.utn.corralon.features.cart_item.entity.CartItemEntity;
import com.utn.corralon.features.order.dto.OrderResponseDTO;
import com.utn.corralon.features.order.entity.OrderEntity;
import com.utn.corralon.features.order.enums.DeliveryType;
import com.utn.corralon.features.order.mapper.OrderMapper;
import com.utn.corralon.features.order.repository.OrderRepository;
import com.utn.corralon.features.order.service.OrderService;
import com.utn.corralon.features.order.util.CheckoutCanonicalPayload;
import com.utn.corralon.features.pricing.service.PricingService;
import com.utn.corralon.features.productVariant.entity.ProductVariantEntity;
import com.utn.corralon.features.productVariant.repository.ProductVariantRepository;
import com.utn.corralon.features.user.entity.UserEntity;
import com.utn.corralon.features.user.repository.UserRepository;
import org.modelmapper.ModelMapper;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.Map;

@Service
public class CartServiceImpl implements ICartService {

    private final CartRepository cartRepository;
    private final UserRepository userRepository;
    private final ProductVariantRepository productVariantRepository;
    private final ModelMapper modelMapper;
    private final OrderService orderService;
    private final PricingService pricingService;
    private final OrderRepository orderRepository;
    private final OrderMapper orderMapper;
    private final PlatformTransactionManager transactionManager;
    private TransactionTemplate transactionTemplate;

    public CartServiceImpl(
            CartRepository cartRepository,
            UserRepository userRepository,
            ProductVariantRepository productVariantRepository,
            ModelMapper modelMapper,
            OrderService orderService,
            PricingService pricingService,
            OrderRepository orderRepository,
            OrderMapper orderMapper,
            PlatformTransactionManager transactionManager) {
        this.cartRepository = cartRepository;
        this.userRepository = userRepository;
        this.productVariantRepository = productVariantRepository;
        this.modelMapper = modelMapper;
        this.orderService = orderService;
        this.pricingService = pricingService;
        this.orderRepository = orderRepository;
        this.orderMapper = orderMapper;
        this.transactionManager = transactionManager;
        this.transactionTemplate = transactionManager != null ? new TransactionTemplate(transactionManager) : null;
    }

    // Visible para tests unitarios
    void setTransactionTemplate(TransactionTemplate transactionTemplate) {
        this.transactionTemplate = transactionTemplate;
    }

    protected TransactionTemplate getTransactionTemplate() {
        if (this.transactionTemplate != null) {
            return this.transactionTemplate;
        }
        if (this.transactionManager != null) {
            this.transactionTemplate = new TransactionTemplate(this.transactionManager);
            return this.transactionTemplate;
        }
        return new TransactionTemplate();
    }

    @Transactional
    @Override
    public CartResponseDTO createOrUpdateCart(String username, CartRequestDTO cartRequest) {
        UserEntity user = getActiveUser(username);

        validateNoDuplicateVariants(cartRequest);

        CartEntity cart = cartRepository.findByUser(user)
                .orElseGet(() -> CartEntity.builder()
                        .user(user)
                        .cartItems(new ArrayList<>())
                        .build());

        // Mapear los ítems existentes del carrito para una búsqueda eficiente
        Map<UUID, CartItemEntity> existingItemsMap = cart.getCartItems().stream()
                .collect(Collectors.toMap(
                        item -> item.getProductVariant().getExternalId(),
                        item -> item
                ));

        // Mapear los ítems entrantes de la solicitud para una búsqueda eficiente
        Map<UUID, CartItemRequestDTO> incomingItemsMap = cartRequest.getItems().stream()
                .collect(Collectors.toMap(
                        CartItemRequestDTO::getProductVariantId,
                        item -> item
                ));

        // Lista para almacenar los ítems que deben ser eliminados del carrito
        List<CartItemEntity> itemsToRemove = new ArrayList<>();

        // Procesar ítems existentes: actualizar o marcar para eliminación
        for (CartItemEntity existingItem : new ArrayList<>(cart.getCartItems())) {
            UUID productVariantId = existingItem.getProductVariant().getExternalId();
            CartItemRequestDTO incomingItemRequest = incomingItemsMap.get(productVariantId);

            if (incomingItemRequest != null) {
                ProductVariantEntity productVariant = productVariantRepository.findByExternalId(productVariantId)
                        .orElseThrow(() -> new ResourceNotFoundException(
                                "ProductVariant not found with ID: ", productVariantId));

                // Validar si el producto está activo
                if (!productVariant.getActive()) {
                    throw new BusinessRuleException(
                            "Product variant with ID " + productVariantId
                                    + " is not active and cannot be added to cart.");
                }

                if (!productVariant.getProduct().isActive()) {
                    throw new BusinessRuleException(
                            "Product with ID " + productVariant.getProduct().getExternalId()
                                    + " is not active and cannot be added to cart.");
                }

                if (productVariant.getStock() < incomingItemRequest.getQuantity()) {
                    throw new StockInsufficientException(productVariant.getExternalId().toString());
                }

                existingItem.setQuantity(incomingItemRequest.getQuantity());
                incomingItemsMap.remove(productVariantId);
            } else {
                itemsToRemove.add(existingItem);
            }
        }

        // Eliminar ítems marcados
        for (CartItemEntity item : itemsToRemove) {
            cart.removeCartItem(item);
        }

        // Añadir nuevos ítems (los que quedan en incomingItemsMap)
        for (CartItemRequestDTO newItemRequest : incomingItemsMap.values()) {
            ProductVariantEntity productVariant = productVariantRepository.findByExternalId(
                            newItemRequest.getProductVariantId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "ProductVariant not found with ID: ",
                            newItemRequest.getProductVariantId()));

            // Validar si el producto está activo
            if (!productVariant.getActive()) {
                throw new BusinessRuleException(
                        "Product variant with ID " + newItemRequest.getProductVariantId()
                                + " is not active and cannot be added to cart.");
            }

            if (!productVariant.getProduct().isActive()) {
                throw new BusinessRuleException(
                        "Product with ID " + productVariant.getProduct().getExternalId()
                                + " is not active and cannot be added to cart.");
            }

            if (productVariant.getStock() < newItemRequest.getQuantity()) {
                throw new StockInsufficientException(productVariant.getExternalId().toString());
            }

            CartItemEntity newCartItem = CartItemEntity.builder()
                    .productVariant(productVariant)
                    .quantity(newItemRequest.getQuantity())
                    .build();

            cart.addCartItem(newCartItem);
        }

        CartEntity savedCart = cartRepository.save(cart);

        return mapCartToResponseDTO(savedCart);
    }

    @Transactional
    @Override
    public CartResponseDTO updateCartItemQuantity(
            String username,
            UUID productVariantId,
            CartItemQuantityUpdateDTO updateDTO) {

        UserEntity user = getActiveUser(username);

        CartEntity cart = cartRepository.findByUser(user)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Cart not found for user: ", username));

        // Buscar el CartItemEntity específico
        CartItemEntity cartItemToUpdate = cart.getCartItems().stream()
                .filter(item -> item.getProductVariant().getExternalId().equals(productVariantId))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Cart item for product variant not found with ID: ", productVariantId));

        Integer newQuantity = updateDTO.getQuantity();

        // Si la nueva cantidad es 0, eliminar el ítem del carrito
        if (newQuantity <= 0) {
            cart.removeCartItem(cartItemToUpdate);
        } else {
            ProductVariantEntity productVariant = cartItemToUpdate.getProductVariant();

            // Validar si el producto está activo
            if (!productVariant.getActive()) {
                throw new BusinessRuleException(
                        "Product variant with ID " + productVariantId
                                + " is not active and cannot be updated in cart.");
            }

            if (!productVariant.getProduct().isActive()) {
                throw new BusinessRuleException(
                        "Product with ID " + productVariant.getProduct().getExternalId()
                                + " is not active and cannot be updated in cart.");
            }

            // Validar stock para la nueva cantidad
            if (productVariant.getStock() < newQuantity) {
                throw new StockInsufficientException(productVariant.getExternalId().toString());
            }

            cartItemToUpdate.setQuantity(newQuantity);
        }

        CartEntity savedCart = cartRepository.save(cart);

        return mapCartToResponseDTO(savedCart);
    }

    @Override
    public CartResponseDTO getCart(String username) {
        UserEntity user = getActiveUser(username);

        CartEntity cart = cartRepository.findByUser(user)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Cart not found for user: ", username));

        return mapCartToResponseDTO(cart);
    }

    @Transactional
    @Override
    public void clearCart(String username) {
        UserEntity user = getActiveUser(username);

        CartEntity cart = cartRepository.findByUser(user)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Cart not found for user: ", username));

        if (!cart.getCartItems().isEmpty()) {
            cart.getCartItems().clear();
            cartRepository.save(cart);
        }
    }

    private CartResponseDTO mapCartToResponseDTO(CartEntity cart) {
        CartResponseDTO responseDTO = modelMapper.map(cart, CartResponseDTO.class);

        responseDTO.setExternalId(cart.getExternalId());
        responseDTO.setUserExternalId(
                cart.getUser() != null ? cart.getUser().getExternalId() : null);
        responseDTO.setLastUpdated(cart.getLastUpdated());

        List<CartItemResponseDTO> itemDTOs = cart.getCartItems().stream()
                .map(cartItemEntity -> {
                    CartItemResponseDTO itemResponseDTO = modelMapper.map(
                            cartItemEntity, CartItemResponseDTO.class);

                    itemResponseDTO.setCartExternalId(cart.getExternalId());

                    itemResponseDTO.setProductName(
                            cartItemEntity.getProductVariant().getProduct().getName());

                    itemResponseDTO.setVariantAttribute(
                            cartItemEntity.getProductVariant().getAttribute());

                    itemResponseDTO.setProductVariantExternalId(
                            cartItemEntity.getProductVariant().getExternalId());

                    ProductVariantEntity productVariant = cartItemEntity.getProductVariant();

                    BigDecimal unitPrice = pricingService.calculateUnitPrice(
                            productVariant,
                            cartItemEntity.getQuantity());

                    itemResponseDTO.setUnitPrice(unitPrice);

                    return itemResponseDTO;
                })
                .toList();

        responseDTO.setCartItems(itemDTOs);

        BigDecimal totalAmount = itemDTOs.stream()
                .map(item -> item.getUnitPrice().multiply(
                        BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        responseDTO.setTotalAmount(totalAmount);

        return responseDTO;
    }

    private UserEntity getActiveUser(String username) {
        UserEntity user = userRepository.findByEmail(username)
                .orElseThrow(() ->
                        new ResourceNotFoundException("User not found: ", username));

        if (!user.getActive()) {
            throw new BusinessRuleException("User is inactive");
        }

        return user;
    }

    private void validateNoDuplicateVariants(CartRequestDTO cartRequest) {
        if (cartRequest == null || cartRequest.getItems() == null) {
            return;
        }

        Set<UUID> variantIds = new HashSet<>();

        for (CartItemRequestDTO item : cartRequest.getItems()) {
            if (item == null || item.getProductVariantId() == null) {
                throw new BadRequestException("Product variant ID cannot be null.");
            }

            if (!variantIds.add(item.getProductVariantId())) {
                throw new BadRequestException(
                        "Duplicate product variant ID in cart: "
                                + item.getProductVariantId());
            }
        }
    }

    @Transactional
    @Override
    public CartResponseDTO removeCartItem(String username, UUID productVariantId) {
        UserEntity user = getActiveUser(username);

        CartEntity cart = cartRepository.findByUser(user)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Cart not found for user: ", username));

        CartItemEntity cartItemToRemove = cart.getCartItems().stream()
                .filter(item -> item.getProductVariant().getExternalId().equals(productVariantId))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Cart item for product variant not found with ID: ", productVariantId));

        cart.removeCartItem(cartItemToRemove);

        CartEntity savedCart = cartRepository.save(cart);

        return mapCartToResponseDTO(savedCart);
    }

    @Override
    public CartEntity getCartEntity(String username) {
        UserEntity user = getActiveUser(username);

        return cartRepository.findByUser(user)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Cart not found for user: ", username));
    }

    @Override
    public OrderResponseDTO checkout(
            String username,
            UUID addressId,
            DeliveryType deliveryType) {
        // Compatibilidad: delega en el checkout idempotente, que administra
        // la transaccion y los locks pesimistas.
        return checkout(username, addressId, deliveryType, UUID.randomUUID().toString()).getOrder();
    }

    @Override
    public CheckoutResultDTO checkout(
            String username,
            UUID addressId,
            DeliveryType deliveryType,
            String idempotencyKey) {

        validateIdempotencyKey(idempotencyKey);
        UserEntity user = getActiveUser(username);
        String requestHash = CheckoutCanonicalPayload.computeHash(deliveryType, addressId);

        // 1. Idempotencia: buscar primero si existe un pedido para el usuario y esa clave,
        // incluso si el carrito ya esta vacio.
        Optional<OrderEntity> existingOrderOpt = orderRepository.findByUserAndIdempotencyKey(user, idempotencyKey);
        if (existingOrderOpt.isPresent()) {
            return resolveExistingOrder(existingOrderOpt.get(), requestHash);
        }

        // 2. Ejecutar checkout dentro de una transaccion con locks pesimistas
        try {
            TransactionTemplate template = getTransactionTemplate();
            return template.execute(status -> {
                // Bloqueo pesimista sobre el carrito
                CartEntity cart = cartRepository.findByUserForUpdate(user)
                        .orElseThrow(() -> new ResourceNotFoundException("Cart not found for user: ", username));

                // Revalidacion de idempotencia DESPUES de adquirir el lock:
                // otra solicitud concurrente con la misma clave pudo haber
                // creado la orden y vaciado el carrito mientras esperabamos.
                Optional<OrderEntity> concurrentOrder = orderRepository
                        .findByUserAndIdempotencyKey(user, idempotencyKey);
                if (concurrentOrder.isPresent()) {
                    return resolveExistingOrder(concurrentOrder.get(), requestHash);
                }

                if (cart.getCartItems() == null || cart.getCartItems().isEmpty()) {
                    throw new BusinessRuleException("Cart is empty");
                }

                // Bloquear las variantes involucradas en orden deterministico
                lockVariantsInDeterministicOrder(cart);

                // Crear orden con idempotencia y descontar stock
                OrderResponseDTO orderResponse = orderService.createFromCart(
                        cart, addressId, deliveryType, idempotencyKey, requestHash);

                // Vaciar el carrito
                cart.getCartItems().clear();
                cartRepository.save(cart);

                return new CheckoutResultDTO(orderResponse, false);
            });
        } catch (DataIntegrityViolationException ex) {
            // Carrera concurrente en UNIQUE KEY (user_id, idempotency_key): releer y resolver
            return orderRepository.findByUserAndIdempotencyKey(user, idempotencyKey)
                    .map(order -> resolveExistingOrder(order, requestHash))
                    .orElseThrow(() -> ex);
        }
    }

    private void validateIdempotencyKey(String idempotencyKey) {
        if (idempotencyKey == null || idempotencyKey.trim().isEmpty()) {
            throw new BadRequestException("Idempotency-Key header is required");
        }
        if (idempotencyKey.length() > 255) {
            throw new BadRequestException("Idempotency-Key exceeds maximum length of 255 characters");
        }
    }

    private CheckoutResultDTO resolveExistingOrder(OrderEntity order, String requestHash) {
        if (!requestHash.equals(order.getCheckoutRequestHash())) {
            throw new BusinessRuleException("Idempotency-Key already used with different checkout parameters");
        }

        // El repositorio carga anticipadamente items, user y address para que
        // el mapeo no dependa del contexto web (OSIV) ni de una sesion abierta.
        return new CheckoutResultDTO(orderMapper.toResponseDTO(order), true);
    }

    private void lockVariantsInDeterministicOrder(CartEntity cart) {
        List<UUID> sortedVariantIds = cart.getCartItems().stream()
                .map(item -> item.getProductVariant().getExternalId())
                .distinct()
                .sorted()
                .toList();

        for (UUID variantId : sortedVariantIds) {
            productVariantRepository.findByExternalIdForUpdate(variantId)
                    .orElseThrow(() -> new ResourceNotFoundException("Product variant not found with ID: ", variantId));
        }
    }
}