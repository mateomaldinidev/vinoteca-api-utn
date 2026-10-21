package com.utn.vinoteca.features.winery.mapper;

import com.utn.vinoteca.features.winery.dto.WineryRequestDTO;
import com.utn.vinoteca.features.winery.entity.WineryEntity;
import com.utn.vinoteca.features.winery.dto.WineryResponseDTO;
import jakarta.validation.constraints.NotNull;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;

@Component
public class WineryMapper {

    private final ModelMapper modelMapper;

    public WineryMapper(ModelMapper modelMapper) {
        this.modelMapper = modelMapper;
    }

    public WineryResponseDTO toResponse(WineryEntity winery) {
        return modelMapper.map(winery, WineryResponseDTO.class);
    }

    public WineryEntity toEntity(@NotNull WineryRequestDTO dto) {
        return modelMapper.map(dto, WineryEntity.class);
    }

    public void updateEntity(WineryEntity winery,
                             WineryRequestDTO dto) {
        modelMapper.map(dto, winery);
    }



}
