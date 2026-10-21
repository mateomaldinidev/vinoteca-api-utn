package com.utn.vinoteca.features.order.service;

import com.utn.vinoteca.exception.BadRequestException;
import com.utn.vinoteca.exception.ResourceNotFoundException;
import com.utn.vinoteca.exception.UnauthorizedException;
import com.utn.vinoteca.security.entity.CredentialsEntity;
import com.utn.vinoteca.security.repository.CredentialsRepository;
import com.utn.vinoteca.security.enums.Roles;
import com.utn.vinoteca.features.notifications.service.IEmailService;
import com.utn.vinoteca.features.order.dto.OrderAdminResponseDTO;
import com.utn.vinoteca.features.order.dto.OrderResponseDTO;
import com.utn.vinoteca.features.order.dto.OrderSummaryDTO;
import com.utn.vinoteca.features.order.entity.OrderEntity;
import com.utn.vinoteca.features.order.enums.OrderStatus;
import com.utn.vinoteca.features.order.mapper.OrderMapper;
import com.utn.vinoteca.features.order.policy.OrderStatusPolicy;
import com.utn.vinoteca.features.order.repository.OrderRepository;
import com.utn.vinoteca.features.orderItem.entity.OrderItemEntity;
import com.utn.vinoteca.features.stock.service.StockService;
import com.utn.vinoteca.features.user.entity.UserEntity;
import com.utn.vinoteca.features.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class OrderService implements IOrderService {

    private final OrderRepository orderRepository;
    private final OrderMapper orderMapper;
    private final UserRepository userRepository;
    private final CredentialsRepository credentialsRepository;
    private final StockService stockService;
    private final IEmailService emailService;
    private final OrderStatusPolicy orderStatusPolicy;

    @Override
    public List<OrderAdminResponseDTO> getAll() {

        return orderRepository.findAll()
                .stream()
                .map(orderMapper::toAdminResponse)
                .toList();
    }

    @Override
    public OrderResponseDTO getByExternalId(UUID externalId) {

        OrderEntity order = findOrder(externalId);

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

    @Override
    @Transactional
    public void cancelOrder(UUID externalId, UUID userExternalId) {

        OrderEntity order = findOrder(externalId);

        UserEntity requestingUser = findUser(userExternalId);

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

        orderStatusPolicy.assertCanCancel(order.getStatus());

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
            stockService.restore(
                    item.getWineVariant(),
                    item.getQuantity(),
                    "Stock restored by order cancellation"
            );
        }
    }

    private OrderEntity findOrder(UUID externalId) {

        return orderRepository
                .findByExternalId(externalId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with ID: ", externalId));
    }

    private UserEntity findUser(UUID externalId) {

        return userRepository
                .findByExternalId(externalId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: ", externalId));
    }
}
