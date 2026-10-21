package com.utn.vinoteca.features.distributor.service;

import com.utn.vinoteca.features.distributor.dto.DistributorRequestDTO;
import com.utn.vinoteca.features.distributor.dto.DistributorResponseDTO;

import java.util.List;
import java.util.UUID;

public interface IDistributorService {
    DistributorResponseDTO create(DistributorRequestDTO dto);
    List<DistributorResponseDTO> getAll();
    DistributorResponseDTO getByExternalId(UUID externalId);
    DistributorResponseDTO update(UUID externalId, DistributorRequestDTO dto);
    void delete(UUID externalId);
    void activate(UUID externalId);
    List<DistributorResponseDTO> getInactive();
}