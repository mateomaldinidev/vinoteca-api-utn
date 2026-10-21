package com.utn.vinoteca.features.winery.service;

import com.utn.vinoteca.features.winery.dto.WineryRequestDTO;
import com.utn.vinoteca.features.winery.dto.WineryResponseDTO;

import java.util.List;
import java.util.UUID;

public interface IWineryService {
    WineryResponseDTO create (WineryRequestDTO dto);
    List<WineryResponseDTO> getAll();
    WineryResponseDTO getByExternalId(UUID externalId);
    WineryResponseDTO update(UUID externalId, WineryRequestDTO dto);
    void delete(UUID externalId);
    void activate(UUID externalId); // Nuevo metodo
    List<WineryResponseDTO> getInactive(); // Nuevo metodo
}