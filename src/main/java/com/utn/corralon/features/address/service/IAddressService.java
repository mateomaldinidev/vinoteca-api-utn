package com.utn.corralon.features.address.service;

import com.utn.corralon.features.address.dto.AddressRequestDTO;
import com.utn.corralon.features.address.dto.AddressResponseDTO;

import java.util.List;
import java.util.UUID;

public interface IAddressService {

    AddressResponseDTO create(AddressRequestDTO address, String username);

    List<AddressResponseDTO> getAll();

    AddressResponseDTO getByExternalId(UUID externalId, String username);

    AddressResponseDTO update(UUID externalId, AddressRequestDTO dto, String username);

    void delete(UUID externalId, UUID userExternalId, String username);

    List<AddressResponseDTO> getAllByUserExternalId(UUID userExternalId, String username);
}
