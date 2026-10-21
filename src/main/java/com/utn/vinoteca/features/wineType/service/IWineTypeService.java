package com.utn.vinoteca.features.wineType.service;

import com.utn.vinoteca.features.wineType.dto.WineTypeRequestDTO;
import com.utn.vinoteca.features.wineType.dto.WineTypeResponseDTO;

import java.util.List;
import java.util.UUID;

public interface IWineTypeService {
    WineTypeResponseDTO create(WineTypeRequestDTO dto);
    List<WineTypeResponseDTO> getAll();
    WineTypeResponseDTO getByExternalId(UUID externalId);
    WineTypeResponseDTO update(UUID externalId, WineTypeRequestDTO dto);
    void delete(UUID externalId);
    void activate(UUID externalId);
    List<WineTypeResponseDTO> getInactive();
}