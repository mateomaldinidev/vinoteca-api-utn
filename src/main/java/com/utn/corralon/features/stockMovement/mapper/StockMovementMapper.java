package com.utn.corralon.features.stockMovement.mapper;

import com.utn.corralon.features.stockMovement.dto.StockMovementResponseDTO;
import com.utn.corralon.features.stockMovement.entity.StockMovementEntity;
import org.springframework.stereotype.Component;

@Component
public class StockMovementMapper {

    // Los movimientos se crean desde StockService.
    // Este mapper es solo para consulta.
    public StockMovementResponseDTO toResponse(StockMovementEntity entity) {
        return new StockMovementResponseDTO(
                entity.getExternalId(),
                entity.getType(),
                entity.getQuantity(),
                entity.getMovementDate(),
                entity.getReason(),
                entity.getVariant().getExternalId()
        );
    }
}
