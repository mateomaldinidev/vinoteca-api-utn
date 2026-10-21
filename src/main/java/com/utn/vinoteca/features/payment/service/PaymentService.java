package com.utn.vinoteca.features.payment.service;

import com.utn.vinoteca.exception.BusinessRuleException;
import com.utn.vinoteca.exception.ResourceNotFoundException;
import com.utn.vinoteca.features.notifications.service.IEmailService;
import com.utn.vinoteca.features.order.dto.OrderResponseDTO;
import com.utn.vinoteca.features.order.entity.OrderEntity;
import com.utn.vinoteca.features.order.enums.OrderStatus;
import com.utn.vinoteca.features.order.mapper.OrderMapper;
import com.utn.vinoteca.features.order.repository.OrderRepository;
import com.utn.vinoteca.features.orderItem.entity.OrderItemEntity;
import com.utn.vinoteca.features.payment.dto.PaymentRequestDTO;
import com.utn.vinoteca.features.payment.dto.PaymentResponseDTO;
import com.utn.vinoteca.features.payment.entity.PaymentEntity;
import com.utn.vinoteca.features.payment.entity.PaymentStatus;
import com.utn.vinoteca.features.payment.mapper.PaymentMapper;
import com.utn.vinoteca.features.payment.repository.PaymentRepository;
import com.utn.vinoteca.features.wineVariant.entity.WineVariantEntity;
import com.utn.vinoteca.features.wineVariant.repository.WineVariantRepository;
import com.utn.vinoteca.features.stockMovement.entity.StockMovementEntity;
import com.utn.vinoteca.features.stockMovement.enums.StockMovementType;
import com.utn.vinoteca.features.stockMovement.repository.StockMovementRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class PaymentService implements IPaymentService {

    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;
    private final WineVariantRepository wineVariantRepository;
    private final PaymentMapper paymentMapper;
    private final OrderMapper orderMapper;
    private final IEmailService emailService;
    private final StockMovementRepository stockMovementRepository;

    @Override
    public PaymentResponseDTO pay(PaymentRequestDTO request) {

        OrderEntity order = orderRepository.findByExternalId(request.getOrderId())
                .orElseThrow(() -> new ResourceNotFoundException("Order not found wit ID: ", request.getOrderId()));

        if (order.getStatus() != OrderStatus.PENDING_PAYMENT) {
            throw new BusinessRuleException("Order is not pending payment");
        }

        if (request.getAmount().compareTo(order.getTotal()) != 0) {
            throw new BusinessRuleException("Invalid payment amount");
        }

        // Crear payment en estado PENDING
        PaymentEntity payment = PaymentEntity.builder()
                .order(order)
                .amount(order.getTotal())
                .paymentMethod(request.getPaymentMethod())
                .paymentStatus(PaymentStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .build();

        payment = paymentRepository.save(payment);

        // Simular pago
        PaymentStatus result = simulateCard(request);

        // CASO APPROVED
        if (result == PaymentStatus.APPROVED) {

            payment.setPaymentStatus(PaymentStatus.APPROVED);
            order.setStatus(OrderStatus.PAID);

            orderRepository.save(order);
            paymentRepository.save(payment);

            OrderResponseDTO orderDTO = orderMapper.toResponseDTO(order);

            emailService.sendOrderStatusChangedEmail(
                    order.getUser().getEmail(),
                    order.getUser().getName(),
                    orderDTO,
                    "PAID"
            );

            return paymentMapper.toDTO(payment);
        }

        // CASO REJECTED → rollback stock + cancelar orden
        payment.setPaymentStatus(PaymentStatus.REJECTED);
        order.setStatus(OrderStatus.CANCELLED);

        restoreStock(order);

        orderRepository.save(order);
        paymentRepository.save(payment);

        OrderResponseDTO orderDTO = orderMapper.toResponseDTO(order);

        emailService.sendOrderCancelledEmail(
                order.getUser().getEmail(),
                order.getUser().getName(),
                orderDTO,
                "Payment was rejected"
        );

        return paymentMapper.toDTO(payment);
    }

    // STOCK ROLLBACK
    private void restoreStock(OrderEntity order) {

        for (OrderItemEntity item : order.getItems()) {

            WineVariantEntity variant = item.getWineVariant();

            variant.setStock(
                    variant.getStock() + item.getQuantity()
            );

            wineVariantRepository.save(variant);

            createStockMovement(
                    variant,
                    item.getQuantity(),
                    StockMovementType.CANCELLATION,
                    "Stock restored by payment rejection"
            );
        }
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


    // SIMULACIÓN TARJETA
    private PaymentStatus simulateCard(PaymentRequestDTO request) {

        return Math.random() > 0.3
                ? PaymentStatus.APPROVED
                : PaymentStatus.REJECTED;
    }

    public PaymentResponseDTO getByOrderId(UUID orderId) {

        PaymentEntity payment = paymentRepository.findByOrder_ExternalId(orderId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Payment not found for order ID: ", orderId));

        return paymentMapper.toDTO(payment);
    }
}