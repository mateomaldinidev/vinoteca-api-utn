package com.utn.vinoteca.features.order.service;

import com.utn.vinoteca.exception.BusinessRuleException;
import com.utn.vinoteca.exception.ResourceNotFoundException;
import com.utn.vinoteca.features.address.entity.AddressEntity;
import com.utn.vinoteca.features.address.repository.AddressRepository;
import com.utn.vinoteca.features.cart.entity.CartEntity;
import com.utn.vinoteca.features.notifications.service.IEmailService;
import com.utn.vinoteca.features.order.dto.OrderResponseDTO;
import com.utn.vinoteca.features.order.entity.OrderEntity;
import com.utn.vinoteca.features.order.enums.DeliveryType;
import com.utn.vinoteca.features.order.factory.OrderFactory;
import com.utn.vinoteca.features.order.mapper.OrderMapper;
import com.utn.vinoteca.features.order.repository.OrderRepository;
import com.utn.vinoteca.features.orderItem.entity.OrderItemEntity;
import com.utn.vinoteca.features.pricing.service.PricingService;
import com.utn.vinoteca.features.stock.service.StockService;
import com.utn.vinoteca.features.wineVariant.entity.WineVariantEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CheckoutService implements ICheckoutService {

    private final OrderRepository orderRepository;
    private final AddressRepository addressRepository;
    private final OrderFactory orderFactory;
    private final OrderMapper orderMapper;
    private final PricingService pricingService;
    private final StockService stockService;
    private final IEmailService emailService;

    @Override
    @Transactional
    public OrderResponseDTO createFromCart(CartEntity cart, UUID addressId, DeliveryType deliveryType) {

        validateDelivery(cart, addressId, deliveryType);

        AddressEntity address = resolveAddress(cart, addressId, deliveryType);

        OrderEntity order = orderFactory.create(cart.getUser(), address, deliveryType);

        List<OrderItemEntity> items = cart.getCartItems()
                .stream()
                .map(cartItem -> {
                    WineVariantEntity variant = cartItem.getWineVariant();

                    if (!variant.getActive()) {
                        throw new BusinessRuleException("Wine variant is inactive");
                    }

                    BigDecimal unitPrice =
                            pricingService.calculateUnitPrice(variant, cartItem.getQuantity());

                    stockService.discount(
                            variant,
                            cartItem.getQuantity(),
                            "Stock discounted by order creation"
                    );

                    return orderFactory.createItem(
                            order,
                            variant,
                            cartItem.getQuantity(),
                            unitPrice
                    );
                })
                .toList();

        BigDecimal total = items.stream()
                .map(OrderItemEntity::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        order.setItems(items);
        order.setTotal(total);

        OrderEntity savedOrder = orderRepository.save(order);

        OrderResponseDTO responseDTO = orderMapper.toResponseDTO(savedOrder);

        emailService.sendOrderCreatedEmail(
                cart.getUser().getEmail(),
                cart.getUser().getName(),
                responseDTO
        );

        return responseDTO;
    }

    private void validateDelivery(CartEntity cart, UUID addressId, DeliveryType deliveryType) {
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
    }

    private AddressEntity resolveAddress(CartEntity cart, UUID addressId, DeliveryType deliveryType) {
        if (deliveryType != DeliveryType.DELIVERY) {
            return null;
        }

        AddressEntity address = addressRepository.findByExternalId(addressId)
                .orElseThrow(() -> new ResourceNotFoundException("Address not found with ID: ", addressId));

        if (!address.getUser().getExternalId().equals(cart.getUser().getExternalId())) {
            throw new BusinessRuleException("Address does not belong to user");
        }

        return address;
    }
}
