package com.utn.vinoteca.features.payment.service;

import com.utn.vinoteca.exception.BusinessRuleException;
import com.utn.vinoteca.exception.ResourceNotFoundException;
import com.utn.vinoteca.features.notifications.service.IEmailService;
import com.utn.vinoteca.features.order.dto.OrderResponseDTO;
import com.utn.vinoteca.features.order.entity.OrderEntity;
import com.utn.vinoteca.features.order.enums.OrderStatus;
import com.utn.vinoteca.features.order.mapper.OrderMapper;
import com.utn.vinoteca.features.order.policy.OrderStatusPolicy;
import com.utn.vinoteca.features.order.repository.OrderRepository;
import com.utn.vinoteca.features.orderItem.entity.OrderItemEntity;
import com.utn.vinoteca.features.payment.dto.PaymentRequestDTO;
import com.utn.vinoteca.features.payment.dto.PaymentResponseDTO;
import com.utn.vinoteca.features.payment.entity.PaymentEntity;
import com.utn.vinoteca.features.payment.entity.PaymentStatus;
import com.utn.vinoteca.features.payment.mapper.PaymentMapper;
import com.utn.vinoteca.features.payment.repository.PaymentRepository;
import com.utn.vinoteca.features.stock.service.StockService;
import com.utn.vinoteca.features.wineVariant.entity.WineVariantEntity;
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
    private final PaymentMapper paymentMapper;
    private final OrderMapper orderMapper;
    private final IEmailService emailService;
    private final StockService stockService;
    private final CardPaymentSimulator cardPaymentSimulator;
    private final OrderStatusPolicy orderStatusPolicy;

    @Override
    public PaymentResponseDTO pay(PaymentRequestDTO request) {

        OrderEntity order = orderRepository.findByExternalId(request.getOrderId())
                .orElseThrow(() -> new ResourceNotFoundException("Order not found wit ID: ", request.getOrderId()));

        orderStatusPolicy.assertCanPay(order.getStatus());

        if (request.getAmount().compareTo(order.getTotal()) != 0) {
            throw new BusinessRuleException("Invalid payment amount");
        }

        if (paymentRepository.findByOrder_ExternalId(order.getExternalId()).isPresent()) {
            throw new BusinessRuleException("Order already has a payment");
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
        PaymentStatus result = cardPaymentSimulator.simulate(request);

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
        orderStatusPolicy.assertCanCancel(order.getStatus());
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

    private void restoreStock(OrderEntity order) {
        for (OrderItemEntity item : order.getItems()) {
            stockService.restore(
                    item.getWineVariant(),
                    item.getQuantity(),
                    "Stock restored by payment rejection"
            );
        }
    }


    public PaymentResponseDTO getByOrderId(UUID orderId) {

        PaymentEntity payment = paymentRepository.findByOrder_ExternalId(orderId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Payment not found for order ID: ", orderId));

        return paymentMapper.toDTO(payment);
    }
}