package com.utn.vinoteca.features.wineVariant.service;

import com.utn.vinoteca.features.wineVariant.dto.WineVariantRequestDTO;
import com.utn.vinoteca.features.wineVariant.dto.WineVariantResponseDTO;
import com.utn.vinoteca.features.stockMovement.dto.StockMovementRequestDTO;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public interface IWineVariantService {

    WineVariantResponseDTO getById(UUID externalId);
    WineVariantResponseDTO create(WineVariantRequestDTO wineVariantRequestDTO);
    WineVariantResponseDTO update(UUID externalId,
                                     WineVariantRequestDTO wineVariantRequestDTO);
    void delete(UUID externalId);
    void activate(UUID externalId);
    WineVariantResponseDTO registerEntry(StockMovementRequestDTO dto);
    WineVariantResponseDTO adjustStock(StockMovementRequestDTO dto);
    Integer getAvailableStock(UUID variantId);


    List<WineVariantResponseDTO> search(String attribute,
                                           BigDecimal minPrice,
                                           BigDecimal maxPrice,
                                           Integer minStock,
                                           UUID wineId,
                                           UUID wineTypeId,
                                           UUID wineryId,
                                           String wineName);
    List<WineVariantResponseDTO> getInactive(
            String attribute,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            Integer minStock,
            UUID wineId,
            UUID wineTypeId,
            UUID wineryId,
            String wineName
    );
}
