package com.utn.vinoteca.features.order.mapper;

import com.utn.vinoteca.features.address.entity.AddressEntity;
import com.utn.vinoteca.features.order.dto.OrderAdminResponseDTO;
import com.utn.vinoteca.features.order.dto.OrderSummaryDTO;
import com.utn.vinoteca.features.order.enums.DeliveryType;
import com.utn.vinoteca.features.order.enums.OrderStatus;
import com.utn.vinoteca.features.order.dto.OrderResponseDTO;
import com.utn.vinoteca.features.order.entity.OrderEntity;
import com.utn.vinoteca.features.orderItem.mapper.OrderItemMapper;
import com.utn.vinoteca.features.user.entity.UserEntity;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class OrderMapper {

    private final ModelMapper modelMapper;
    private final OrderItemMapper orderItemMapper;


    public OrderEntity toEntity(UserEntity user, AddressEntity address, DeliveryType deliveryType)
    {

        OrderEntity order =
                OrderEntity.builder()
                        .user(user)
                        .address(address)
                        .deliveryType(deliveryType)
                        .createdAt(LocalDateTime.now())
                        .status(OrderStatus.PENDING_PAYMENT)
                        .build();

        return order;
    }


    public OrderResponseDTO toResponseDTO(OrderEntity orderEntity) {

        OrderResponseDTO dto = modelMapper.map(orderEntity, OrderResponseDTO.class);

        dto.setUserExternalId(orderEntity.getUser().getExternalId());

        dto.setDeliveryType(orderEntity.getDeliveryType());

        //null check -> permite address null
        dto.setAddressExternalId(
                orderEntity.getAddress() != null
                        ? orderEntity.getAddress().getExternalId()
                        : null
        );

        dto.setItems(
                orderEntity.getItems()
                        .stream()
                        .map(orderItemMapper::toResponseDTO)
                        .toList()
        );

        return dto;
    }

    public OrderAdminResponseDTO toAdminResponse(OrderEntity order) {

        OrderAdminResponseDTO dto = modelMapper.map(order, OrderAdminResponseDTO.class);

        dto.setUserExternalId(order.getUser().getExternalId());

        dto.setDeliveryType(order.getDeliveryType());

        dto.setCustomerName(order.getUser().getName() + " " + order.getUser().getLastName());

        //null check -> permite address null
        dto.setAddressExternalId(
                order.getAddress() != null
                        ? order.getAddress().getExternalId()
                        : null
        );

        dto.setItems(order
                .getItems()
                .stream()
                .map(orderItemMapper::toResponseDTO)
                .toList()
        );

        return dto;
    }


    public OrderSummaryDTO toSummary(OrderEntity order) {
        OrderSummaryDTO dto = new OrderSummaryDTO();

        dto.setExternalId(order.getExternalId());
        dto.setTotal(order.getTotal());
        dto.setStatus(order.getStatus());
        dto.setCreatedAt(order.getCreatedAt());

        return dto;
    }

}
