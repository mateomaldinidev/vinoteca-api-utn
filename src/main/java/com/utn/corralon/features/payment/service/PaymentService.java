package com.utn.corralon.features.payment.service;

import com.utn.corralon.exception.BusinessRuleException;
import com.utn.corralon.exception.ResourceNotFoundException;
import com.utn.corralon.exception.UnauthorizedException;
import com.utn.corralon.features.notifications.service.IEmailService;
import com.utn.corralon.features.order.dto.OrderResponseDTO;
import com.utn.corralon.features.order.entity.OrderEntity;
import com.utn.corralon.features.order.enums.OrderStatus;
import com.utn.corralon.features.order.mapper.OrderMapper;
import com.utn.corralon.features.order.repository.OrderRepository;
import com.utn.corralon.features.orderItem.entity.OrderItemEntity;
import com.utn.corralon.features.payment.dto.PaymentRequestDTO;
import com.utn.corralon.features.payment.dto.PaymentResponseDTO;
import com.utn.corralon.features.payment.entity.PaymentEntity;
import com.utn.corralon.features.payment.entity.PaymentStatus;
import com.utn.corralon.features.payment.mapper.PaymentMapper;
import com.utn.corralon.features.payment.repository.PaymentRepository;
import com.utn.corralon.features.productVariant.entity.ProductVariantEntity;
import com.utn.corralon.features.productVariant.repository.ProductVariantRepository;
import com.utn.corralon.features.stockMovement.entity.StockMovementEntity;
import com.utn.corralon.features.stockMovement.enums.StockMovementType;
import com.utn.corralon.features.stockMovement.repository.StockMovementRepository;
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
    private final ProductVariantRepository productVariantRepository;
    private final PaymentMapper paymentMapper;
    private final OrderMapper orderMapper;
    private final IEmailService emailService;
    private final StockMovementRepository stockMovementRepository;

    @Override
    public PaymentResponseDTO pay(
            PaymentRequestDTO request,
            String username,
            boolean isAdmin) {

        OrderEntity order = orderRepository.findByExternalId(request.getOrderId())
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Order not found with ID: ",
                                request.getOrderId()
                        ));

        // Verificar que la orden pertenezca al usuario autenticado
        if (!isAdmin && !order.getUser().getEmail().equals(username)) {
            throw new UnauthorizedException("You are not authorized to pay this order");
        }

        // Verificar que la orden esté pendiente de pago
        if (order.getStatus() != OrderStatus.PENDING_PAYMENT) {
            throw new BusinessRuleException("Order is not pending payment");
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

        // CASO REJECTED: restaurar stock y cancelar orden
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

            ProductVariantEntity variant = item.getProductVariant();

            variant.setStock(
                    variant.getStock() + item.getQuantity()
            );

            productVariantRepository.save(variant);

            createStockMovement(
                    variant,
                    item.getQuantity(),
                    StockMovementType.CANCELLATION,
                    "Stock restored by payment rejection"
            );
        }
    }

    private void createStockMovement(
            ProductVariantEntity variant,
            Integer quantity,
            StockMovementType type,
            String reason) {

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

    @Override
    public PaymentResponseDTO getByOrderId(
            UUID orderId,
            String username,
            boolean isAdmin) {

        PaymentEntity payment = paymentRepository.findByOrder_ExternalId(orderId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Payment not found for order ID: ",
                                orderId
                        ));

        // Verificar que el pago pertenezca a una orden del usuario autenticado
        if (!isAdmin && !payment.getOrder().getUser().getEmail().equals(username)) {
            throw new UnauthorizedException(
                    "You are not authorized to access this payment"
            );
        }

        return paymentMapper.toDTO(payment);
    }
}