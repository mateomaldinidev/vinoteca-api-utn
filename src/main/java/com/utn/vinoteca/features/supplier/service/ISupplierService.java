package com.utn.vinoteca.features.supplier.service;

import com.utn.vinoteca.features.supplier.dto.SupplierRequestDTO;
import com.utn.vinoteca.features.supplier.dto.SupplierResponseDTO;

import java.util.List;
import java.util.UUID;

public interface ISupplierService {
    SupplierResponseDTO create(SupplierRequestDTO dto);
    List<SupplierResponseDTO> getAll();
    SupplierResponseDTO getByExternalId(UUID externalId);
    SupplierResponseDTO update(UUID externalId, SupplierRequestDTO dto);
    void delete(UUID externalId);
    void activate(UUID externalId);
    List<SupplierResponseDTO> getInactive();
}