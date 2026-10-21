package com.utn.vinoteca.features.order.service;

import com.utn.vinoteca.features.order.dto.OrderAdminResponseDTO;
import com.utn.vinoteca.features.order.dto.OrderResponseDTO;
import com.utn.vinoteca.features.order.dto.OrderSummaryDTO;

import java.util.List;
import java.util.UUID;

public interface IOrderService {

    List<OrderAdminResponseDTO> getAll();

    OrderResponseDTO getByExternalId(UUID externalId);

    List<OrderSummaryDTO> getOrdersByUser(UUID userExternalId);

    OrderAdminResponseDTO getAdminOrder(UUID externalId);

    void cancelOrder(UUID externalId, UUID userExternalId);
}
