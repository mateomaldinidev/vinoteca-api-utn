package com.utn.corralon.features.order.service;

import com.utn.corralon.exception.BusinessRuleException;
import com.utn.corralon.exception.ResourceNotFoundException;
import com.utn.corralon.exception.UnauthorizedException;
import com.utn.corralon.features.auth.CredentialsEntity;
import com.utn.corralon.features.auth.CredentialsRepository;
import com.utn.corralon.features.auth.Roles;
import com.utn.corralon.features.address.entity.AddressEntity;
import com.utn.corralon.features.address.repository.AddressRepository;
import com.utn.corralon.features.cart.entity.CartEntity;
import com.utn.corralon.features.cart.repository.CartRepository;
import com.utn.corralon.features.notifications.service.IEmailService;
import com.utn.corralon.features.order.dto.OrderAdminResponseDTO;
import com.utn.corralon.features.order.enums.DeliveryType;
import com.utn.corralon.features.order.enums.OrderStatus;
import com.utn.corralon.features.order.dto.OrderResponseDTO;
import com.utn.corralon.features.order.dto.OrderSummaryDTO;
import com.utn.corralon.features.order.entity.OrderEntity;
import com.utn.corralon.features.order.mapper.OrderMapper;
import com.utn.corralon.features.order.repository.OrderRepository;
import com.utn.corralon.features.orderItem.entity.OrderItemEntity;
import com.utn.corralon.features.orderItem.mapper.OrderItemMapper;
import com.utn.corralon.features.pricing.service.PricingService;
import com.utn.corralon.features.productVariant.entity.ProductVariantEntity;
import com.utn.corralon.features.stockMovement.service.IStockService;
import com.utn.corralon.features.user.entity.UserEntity;
import com.utn.corralon.features.user.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import com.utn.corralon.exception.BadRequestException;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class OrderService implements IOrderService {

    private final OrderRepository orderRepository;
    private final OrderMapper orderMapper;
    private final OrderItemMapper orderItemMapper;
    private final UserRepository userRepository;
    private final CredentialsRepository credentialsRepository;
    private final AddressRepository addressRepository;
    private final CartRepository cartRepository;
    private final IStockService stockService;
    private final IEmailService emailService;
    private final PricingService pricingService;

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
        // 4. CREAR ITEMS
        // =========================

        List<OrderItemEntity> items =
                cart.getCartItems()
                        .stream()
                        .map(cartItem -> {

                            ProductVariantEntity variant = cartItem.getProductVariant();

                            if (!variant.getActive()) {
                                throw new BusinessRuleException("Product variant is inactive");
                            }

                            BigDecimal unitPrice =
                                    pricingService.calculateUnitPrice(variant, cartItem.getQuantity());

                            BigDecimal subtotal =
                                    unitPrice.multiply(BigDecimal.valueOf(cartItem.getQuantity()));

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
        // 6. PERSISTENCIA + STOCK
        // =========================

        OrderEntity savedOrder = orderRepository.save(order);

        // El pedido y sus items ya tienen ID: el descuento de stock queda
        // trazado con orderId y orderItemId en la misma transaccion.
        for (OrderItemEntity item : savedOrder.getItems()) {
            stockService.registerSale(
                    item.getProductVariant().getExternalId(),
                    item.getQuantity(),
                    savedOrder.getId(),
                    item.getId(),
                    savedOrder.getUser().getId(),
                    "Stock discounted by order creation"
            );
        }

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
    public OrderResponseDTO getByExternalId(UUID externalId, String username) {

        OrderEntity order = orderRepository
                .findByExternalId(externalId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Order not found with ID: ", externalId));

        CredentialsEntity credentials = credentialsRepository
                .findByUsername(username)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Credentials not found id: ", externalId));

        boolean isAdminOrEmployee = credentials.getRoles().stream()
                .anyMatch(r -> r.getRole() == Roles.ROLE_ADMIN
                        || r.getRole() == Roles.ROLE_EMPLOYEE);

        if (!isAdminOrEmployee &&
                !order.getUser().getEmail().equals(username)) {
            throw new UnauthorizedException("You can only access your own orders");
        }

        return orderMapper.toResponseDTO(order);
    }

    @Override
    public List<OrderSummaryDTO> getOrdersByUser(String username) {

        UserEntity user = userRepository.findByEmail(username)
                .orElseThrow(() ->
                        new ResourceNotFoundException("User not found", username));

        return orderRepository
                .findByUser_ExternalId(user.getExternalId())
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
    public void cancelOrder(UUID externalId, String username) {

        OrderEntity order = orderRepository
                .findByExternalId(externalId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Order not found with ID: ", externalId));

        CredentialsEntity credentials = credentialsRepository
                .findByUsername(username)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Credentials not found id: ", externalId));

        boolean isAdminOrEmployee = credentials.getRoles().stream()
                .anyMatch(r -> r.getRole() == Roles.ROLE_ADMIN
                        || r.getRole() == Roles.ROLE_EMPLOYEE);

        boolean isCustomer = credentials.getRoles().stream()
                .anyMatch(r -> r.getRole() == Roles.ROLE_CUSTOMER);

        if (isCustomer && !isAdminOrEmployee) {
            if (!order.getUser().getEmail().equals(username)) {
                throw new UnauthorizedException("You can only cancel your own orders");
            }
        }

        if (!isCustomer && !isAdminOrEmployee) {
            throw new UnauthorizedException("You are not allowed to cancel orders");
        }

        if (order.getStatus() == OrderStatus.CANCELLED) {
            throw new BadRequestException("Order is already cancelled");
        }

        if (order.getStatus() == OrderStatus.PAID) {
            throw new BusinessRuleException("Paid orders cannot be cancelled");
        }

        restoreStock(order, resolveUserId(username));
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

    private void restoreStock(OrderEntity order, Long createdByUserId) {

        for (OrderItemEntity item : order.getItems()) {

            stockService.restoreStock(
                    item.getProductVariant().getExternalId(),
                    item.getQuantity(),
                    order.getId(),
                    item.getId(),
                    createdByUserId,
                    "Stock restored by order cancellation"
            );
        }
    }

    private UserEntity findUser(UUID externalId) {

        return userRepository
                .findByExternalId(externalId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: ", externalId));
    }

    private Long resolveUserId(String username) {
        if (username == null) {
            return null;
        }
        return userRepository.findByEmail(username)
                .map(UserEntity::getId)
                .orElse(null);
    }
    @Override
    public List<OrderSummaryDTO> getOrdersByUserId(UUID userExternalId) {

        UserEntity user = userRepository.findByExternalId(userExternalId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("User not found with ID: ", userExternalId));

        return orderRepository.findByUser_ExternalId(userExternalId)
                .stream()
                .map(orderMapper::toSummary)
                .toList();
    }

}
