package com.utn.vinoteca.features.product.service;

import com.utn.vinoteca.features.product.dto.ProductDeleteResponseDTO;
import com.utn.vinoteca.features.product.dto.ProductRequestDTO;
import com.utn.vinoteca.features.product.dto.ProductResponseDTO;


import java.util.List;
import java.util.UUID;

public interface IProductService {
    ProductResponseDTO getById(UUID externalId);
    List<ProductResponseDTO> search(String name, UUID supplierId, UUID categoryId, UUID brandId);
    ProductResponseDTO create(ProductRequestDTO productRequestDTO);
    ProductResponseDTO update(UUID externalId,
                              ProductRequestDTO productRequestDTO);
    ProductDeleteResponseDTO delete(UUID externalId);
    void activate(UUID externalId);
    List<ProductResponseDTO> getInactive(String name, UUID supplierId, UUID categoryId, UUID brandId);
}
