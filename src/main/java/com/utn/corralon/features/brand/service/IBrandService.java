package com.utn.corralon.features.brand.service;

import com.utn.corralon.features.brand.dto.BrandRequestDTO;
import com.utn.corralon.features.brand.dto.BrandResponseDTO;

import java.util.List;
import java.util.UUID;

public interface IBrandService {
    BrandResponseDTO create (BrandRequestDTO dto);
    List<BrandResponseDTO> getAll();
    BrandResponseDTO getByExternalId(UUID externalId);
    BrandResponseDTO update(UUID externalId, BrandRequestDTO dto);
    void delete(UUID externalId);
    long countAssociatedProducts(UUID externalId);
    void activate(UUID externalId); // Nuevo metodo
    List<BrandResponseDTO> getInactive(); // Nuevo metodo
}