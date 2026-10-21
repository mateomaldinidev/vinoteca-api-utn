package com.utn.vinoteca.features.wine.service;

import com.utn.vinoteca.features.wine.dto.WineDeleteResponseDTO;
import com.utn.vinoteca.features.wine.dto.WineRequestDTO;
import com.utn.vinoteca.features.wine.dto.WineResponseDTO;


import java.util.List;
import java.util.UUID;

public interface IWineService {
    WineResponseDTO getById(UUID externalId);
    List<WineResponseDTO> search(String name, UUID distributorId, UUID wineTypeId, UUID wineryId);
    WineResponseDTO create(WineRequestDTO wineRequestDTO);
    WineResponseDTO update(UUID externalId,
                              WineRequestDTO wineRequestDTO);
    WineDeleteResponseDTO delete(UUID externalId);
    void activate(UUID externalId);
    List<WineResponseDTO> getInactive(String name, UUID distributorId, UUID wineTypeId, UUID wineryId);
}
