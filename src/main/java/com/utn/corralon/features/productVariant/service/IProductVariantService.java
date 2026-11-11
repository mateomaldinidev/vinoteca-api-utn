package com.utn.corralon.features.productVariant.service;

import com.utn.corralon.features.productVariant.dto.ProductVariantRequestDTO;
import com.utn.corralon.features.productVariant.dto.ProductVariantResponseDTO;
import com.utn.corralon.features.stockMovement.dto.StockMovementRequestDTO;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public interface IProductVariantService {

    ProductVariantResponseDTO getById(UUID externalId);
    ProductVariantResponseDTO create(ProductVariantRequestDTO productVariantRequestDTO);
    ProductVariantResponseDTO update(UUID externalId,
                                     ProductVariantRequestDTO productVariantRequestDTO);
    void delete(UUID externalId);
    void activate(UUID externalId);
    ProductVariantResponseDTO registerEntry(StockMovementRequestDTO dto, String username);
    ProductVariantResponseDTO adjustStock(StockMovementRequestDTO dto, String username);
    Integer getAvailableStock(UUID variantId);


    List<ProductVariantResponseDTO> search(String attribute,
                                           BigDecimal minPrice,
                                           BigDecimal maxPrice,
                                           Integer minStock,
                                           UUID productId,
                                           UUID categoryId,
                                           UUID brandId,
                                           String productName);
    List<ProductVariantResponseDTO> getInactive(
            String attribute,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            Integer minStock,
            UUID productId,
            UUID categoryId,
            UUID brandId,
            String productName
    );
}
