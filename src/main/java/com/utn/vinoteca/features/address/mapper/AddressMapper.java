package com.utn.vinoteca.features.address.mapper;

import com.utn.vinoteca.features.address.dto.AddressRequestDTO;
import com.utn.vinoteca.features.address.entity.AddressEntity;
import com.utn.vinoteca.features.address.dto.AddressResponseDTO;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;

@Component
public class AddressMapper {
    private final ModelMapper modelMapper;

    public AddressMapper(ModelMapper modelMapper) {
        this.modelMapper = modelMapper;
    }

    public AddressResponseDTO toResponse(AddressEntity address) {
        AddressResponseDTO dto =
         modelMapper.map(address, AddressResponseDTO.class);
        dto.setUserExternalId(address.getUser().getExternalId());
        return dto;

    }

    public AddressEntity toEntity(AddressRequestDTO dto) {
        AddressEntity entity = modelMapper.map(dto, AddressEntity.class);
        return entity;
    }

    public void updateEntity(AddressEntity entity, AddressRequestDTO dto) {
        modelMapper.map(dto, entity);
    }

}