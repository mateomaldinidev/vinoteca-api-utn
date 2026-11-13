package com.utn.corralon.features.stockMovement.service;

import com.utn.corralon.exception.ResourceNotFoundException;
import com.utn.corralon.features.stockMovement.dto.StockMovementResponseDTO;
import com.utn.corralon.features.stockMovement.entity.StockMovementEntity;
import com.utn.corralon.features.stockMovement.mapper.StockMovementMapper;
import com.utn.corralon.features.stockMovement.repository.StockMovementRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class StockMovementService implements IStockMovementService {

    private final StockMovementRepository stockMovementRepository;
    private final StockMovementMapper stockMovementMapper;

    // Los movimientos se crean desde StockService.
    // Este servicio es solo para consulta.
    @Override
    public List<StockMovementResponseDTO> getMovementsByVariant(UUID variantId) {
        return stockMovementRepository.findByVariantExternalId(variantId).stream()
                .map(stockMovementMapper::toResponse)
                .toList();
    }

    @Override
    public StockMovementResponseDTO getMovementById(UUID externalId) {
        StockMovementEntity entity = stockMovementRepository.findByExternalId(externalId);
        if (entity == null) {
            throw new ResourceNotFoundException("Stock movement not found with ID: ", externalId);
        }
        return stockMovementMapper.toResponse(entity);
    }
}
