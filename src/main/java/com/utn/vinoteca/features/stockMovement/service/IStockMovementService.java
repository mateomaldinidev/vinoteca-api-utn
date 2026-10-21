package com.utn.vinoteca.features.stockMovement.service;

import com.utn.vinoteca.features.stockMovement.dto.StockMovementResponseDTO;

import java.util.List;
import java.util.UUID;

public interface IStockMovementService {
    List<StockMovementResponseDTO> getMovementsByVariant(UUID variantId);
    StockMovementResponseDTO getMovementById(UUID externalId);
}
