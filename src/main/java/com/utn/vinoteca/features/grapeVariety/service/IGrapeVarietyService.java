package com.utn.vinoteca.features.grapeVariety.service;

import com.utn.vinoteca.features.grapeVariety.dto.GrapeVarietyRequestDTO;
import com.utn.vinoteca.features.grapeVariety.dto.GrapeVarietyResponseDTO;

import java.util.List;
import java.util.UUID;

public interface IGrapeVarietyService {
    GrapeVarietyResponseDTO create(GrapeVarietyRequestDTO dto);
    List<GrapeVarietyResponseDTO> getAll();
    GrapeVarietyResponseDTO getByExternalId(UUID externalId);
    GrapeVarietyResponseDTO update(UUID externalId, GrapeVarietyRequestDTO dto);
    void delete(UUID externalId);
    void activate(UUID externalId);
    List<GrapeVarietyResponseDTO> getInactive();
}
