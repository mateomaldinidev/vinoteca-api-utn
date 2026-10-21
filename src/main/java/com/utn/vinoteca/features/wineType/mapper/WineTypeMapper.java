package com.utn.vinoteca.features.wineType.mapper;

import com.utn.vinoteca.features.wineType.dto.WineTypeRequestDTO; // Importar WineTypeRequestDTO
import com.utn.vinoteca.features.wineType.dto.WineTypeResponseDTO;
import com.utn.vinoteca.features.wineType.entity.WineTypeEntity;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;

@Component
public class WineTypeMapper {

    private final ModelMapper modelMapper;

    public WineTypeMapper(ModelMapper modelMapper) {
        this.modelMapper = modelMapper;
    }

    public WineTypeResponseDTO toResponse(WineTypeEntity wineType) {
        return modelMapper.map(wineType, WineTypeResponseDTO.class);
    }

    public WineTypeEntity toEntity(WineTypeRequestDTO dto) {
        return modelMapper.map(dto, WineTypeEntity.class);
    }

    public void updateEntity(WineTypeEntity wineType,
                             WineTypeRequestDTO dto) {
        modelMapper.map(dto, wineType);
    }
}