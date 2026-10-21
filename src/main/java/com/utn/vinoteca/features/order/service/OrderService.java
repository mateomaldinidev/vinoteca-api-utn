package com.utn.vinoteca.features.order.service;

import com.utn.vinoteca.exception.BusinessRuleException;
import com.utn.vinoteca.exception.ResourceNotFoundException;
import com.utn.vinoteca.exception.UnauthorizedException;
import com.utn.vinoteca.features.auth.CredentialsEntity;
import com.utn.vinoteca.features.auth.CredentialsRepository;
import com.utn.vinoteca.features.auth.Roles;
import com.utn.vinoteca.features.address.entity.AddressEntity;
import com.utn.vinoteca.features.address.repository.AddressRepository;
import com.utn.vinoteca.features.cart.entity.CartEntity;
import com.utn.vinoteca.features.cart.repository.CartRepository;
import com.utn.vinoteca.features.notifications.service.IEmailService;
import com.utn.vinoteca.features.offer_product.repository.OfferProductRepository;
import com.utn.vinoteca.features.order.dto.OrderAdminResponseDTO;
import com.utn.vinoteca.features.order.enums.DeliveryType;
import com.utn.vinoteca.features.order.enums.OrderStatus;
import com.utn.vinoteca.features.order.dto.OrderResponseDTO;
import com.utn.vinoteca.features.order.dto.OrderSummaryDTO;
import com.utn.vinoteca.features.order.entity.OrderEntity;
import com.utn.vinoteca.features.order.mapper.OrderMapper;
import com.utn.vinoteca.features.order.repository.OrderRepository;
import com.utn.vinoteca.features.orderItem.entity.OrderItemEntity;
import com.utn.vinoteca.features.orderItem.mapper.OrderItemMapper;
import com.utn.vinoteca.features.wineVariant.entity.WineVariantEntity;
import com.utn.vinoteca.features.wineVariant.repository.WineVariantRepository;
import com.utn.vinoteca.features.stockMovement.entity.StockMovementEntity;
import com.utn.vinoteca.features.stockMovement.enums.StockMovementType;
import com.utn.vinoteca.features.stockMovement.repository.StockMovementRepository;
import com.utn.vinoteca.features.user.entity.UserEntity;
import com.utn.vinoteca.features.user.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import com.utn.vinoteca.exception.BadRequestException;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class OrderService implements IOrderService {

    private final OrderRepository orderRepository;
    private final OrderMapper orderMapper;
    private final OrderItemMapper orderItemMapper;
    private final WineVariantRepository wineVariantRepository;
    private final UserRepository userRepository;
    private final CredentialsRepository credentialsRepository;
    private final AddressRepository addressRepository;
    private final CartRepository cartRepository;
    private final StockMovementRepository stockMovementRepository;
    private final IEmailService emailService;
    private final OfferProductRepository offerProductRepository;

    @Override
    @Transactional
    public OrderResponseDTO createFromCart(CartEntity cart, UUID addressId, DeliveryType deliveryType) {

        // =========================
        // 1. VALIDACIONES DE DOMINIO
        // =========================

        if (deliveryType == null) {
            throw new BusinessRuleException("Delivery type is required");
        }

        if (cart.getCartItems().isEmpty()) {
            throw new BusinessRuleException("Cart is empty");
        }

        if (deliveryType == DeliveryType.DELIVERY && addressId == null) {
            throw new BusinessRuleException("Address is required for DELIVERY orders");
        }

        if (deliveryType == DeliveryType.PICKUP && addressId != null) {
            throw new BusinessRuleException("Address must be null for PICKUP orders");
        }

        // =========================
        // 2. RESOLUCIÓN DE ADDRESS
        // =========================

        AddressEntity address = null;

        if (deliveryType == DeliveryType.DELIVERY) {

            address = addressRepository.findByExternalId(addressId)
                    .orElseThrow(() ->
                            new ResourceNotFoundException("Address not found with ID: ", addressId)
                    );

            if (!address.getUser().getExternalId()
                    .equals(cart.getUser().getExternalId())) {
                throw new BusinessRuleException("Address does not belong to user");
            }
        }

        // =========================
        // 3. CREAR ORDER BASE
        // =========================

        OrderEntity order = orderMapper.toEntity(
                cart.getUser(),
                address,
                deliveryType
        );

        // =========================
        // 4. CREAR ITEMS + STOCK
        // =========================

        List<OrderItemEntity> items =
                cart.getCartItems()
                        .stream()
                        .map(cartItem -> {

                            WineVariantEntity variant = cartItem.getWineVariant();

                            if (!variant.getActive()) {
                                throw new BusinessRuleException("Wine variant is inactive");
                            }

                            if (variant.getStock() < cartItem.getQuantity()) {
                                throw new BusinessRuleException("Insufficient stock");
                            }

                            BigDecimal unitPrice =
                                    calculateUnitPrice(variant, cartItem.getQuantity());

                            BigDecimal subtotal =
                                    unitPrice.multiply(BigDecimal.valueOf(cartItem.getQuantity()));

                            // descontar stock
                            variant.setStock(variant.getStock() - cartItem.getQuantity());
                            wineVariantRepository.save(variant);

                            // movimiento stock
                            createStockMovement(variant, cartItem.getQuantity(), StockMovementType.SALE, "Stock discounted by order creation");

                            return orderItemMapper.toEntity(order, variant, cartItem.getQuantity(), unitPrice, subtotal);
                        })
                        .toList();

        // =========================
        // 5. TOTAL
        // =========================

        BigDecimal total = items.stream()
                .map(OrderItemEntity::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        order.setItems(items);
        order.setTotal(total);

        // (NO seteamos status acá porque ya lo maneja el mapper)

        // =========================
        // 6. PERSISTENCIA
        // =========================

        OrderEntity savedOrder = orderRepository.save(order);

        OrderResponseDTO responseDTO = orderMapper.toResponseDTO(savedOrder);

        emailService.sendOrderCreatedEmail(
                cart.getUser().getEmail(),
                cart.getUser().getName(),
                responseDTO
        );

        return responseDTO;
    }

    @Override
    public List<OrderAdminResponseDTO> getAll() {

        return orderRepository.findAll()
                .stream()
                .map(orderMapper::toAdminResponse)
                .toList();
    }

    @Override
    public OrderResponseDTO getByExternalId(UUID externalId) {

        OrderEntity order = orderRepository
                .findByExternalId(externalId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with ID: ", externalId));

        return orderMapper.toResponseDTO(order);
    }

    @Override
    public List<OrderSummaryDTO> getOrdersByUser(UUID userExternalId) {
        findUser(userExternalId);

        return orderRepository
                .findByUser_ExternalId(userExternalId)
                .stream()
                .map(orderMapper::toSummary)
                .toList();
    }

    @Override
    public OrderAdminResponseDTO getAdminOrder(UUID externalId) {

        OrderEntity order = findOrder(externalId);

        return orderMapper.toAdminResponse(order);
    }

    private OrderEntity findOrder(UUID externalId) {

        return orderRepository
                .findByExternalId(externalId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with ID: ", externalId));
    }

    @Override
    @Transactional
    public void cancelOrder(UUID externalId, UUID userExternalId) {

        OrderEntity order = orderRepository
                .findByExternalId(externalId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with ID: ", externalId));

        UserEntity requestingUser = userRepository
                .findByExternalId(userExternalId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: ", userExternalId));

        CredentialsEntity credentials = credentialsRepository
                .findByUsername(requestingUser.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("Credentials not found for user: ", userExternalId));

        boolean isCustomer = credentials.getRoles().stream()
                .anyMatch(r -> r.getRole() == Roles.ROLE_CUSTOMER);

        boolean isAdminOrEmployee = credentials.getRoles().stream()
                .anyMatch(r -> r.getRole() == Roles.ROLE_ADMIN || r.getRole() == Roles.ROLE_EMPLOYEE);

        if (isCustomer && !isAdminOrEmployee) {
            if (!order.getUser().getExternalId().equals(userExternalId)) {
                throw new UnauthorizedException("You can only cancel your own orders");
            }
        }

        if (order.getStatus() == OrderStatus.CANCELLED) {
            throw new BadRequestException("Order is already cancelled");
        }

        if (order.getStatus() == OrderStatus.PAID) {
            throw new BusinessRuleException("Paid orders cannot be cancelled");
        }

        restoreStock(order);

        order.setStatus(OrderStatus.CANCELLED);

        orderRepository.save(order);

        OrderResponseDTO orderDTO = orderMapper.toResponseDTO(order);

        emailService.sendOrderCancelledEmail(
                order.getUser().getEmail(),
                order.getUser().getName(),
                orderDTO,
                "Order cancelled"
        );
    }

    private void restoreStock(OrderEntity order) {

        for (OrderItemEntity item : order.getItems()) {

            WineVariantEntity variant = item.getWineVariant();

            variant.setStock(variant.getStock() + item.getQuantity());

            wineVariantRepository.save(variant);

            createStockMovement(
                    variant,
                    item.getQuantity(),
                    StockMovementType.CANCELLATION,
                    "Stock restored by order cancellation"
            );
        }
    }

    private BigDecimal calculateUnitPrice(WineVariantEntity variant, Integer quantity) {

        BigDecimal offerPrice = offerProductRepository
                .findActiveOfferForVariant(variant.getExternalId())
                .map(op -> op.getDiscountedPrice())
                .orElse(null);

        if (offerPrice != null) {
            return offerPrice;
        }

        if (variant.getWholesaleMinQty() != null
                && variant.getWholesalePrice() != null
                && quantity >= variant.getWholesaleMinQty()) {
            return variant.getWholesalePrice();
        }

        return variant.getPrice();
    }

    private UserEntity findUser(UUID externalId) {

        return userRepository
                .findByExternalId(externalId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: ", externalId));
    }

    private void createStockMovement(WineVariantEntity variant, Integer quantity, StockMovementType type, String reason) {
        StockMovementEntity movement = StockMovementEntity.builder()
                .movementDate(LocalDateTime.now())
                .quantity(quantity)
                .type(type)
                .reason(reason)
                .variant(variant)
                .build();

        stockMovementRepository.save(movement);
    }

}
