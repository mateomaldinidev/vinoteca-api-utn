package com.utn.vinoteca.features.address.service;

import com.utn.vinoteca.features.address.dto.AddressRequestDTO;
import com.utn.vinoteca.features.address.dto.AddressResponseDTO;

import java.util.List;
import java.util.UUID;

public interface IAddressService {

    AddressResponseDTO create(AddressRequestDTO address);

    List<AddressResponseDTO> getAll();

    AddressResponseDTO getByExternalId(UUID externalId);

    AddressResponseDTO update(UUID externalId, AddressRequestDTO dto);

    void delete(UUID externalId, UUID userExternalId);

    List<AddressResponseDTO> getAllByUserExternalId(UUID userExternalId);
}
