package com.utn.vinoteca.features.grapeVariety.mapper;

import com.utn.vinoteca.features.grapeVariety.dto.GrapeVarietyRequestDTO;
import com.utn.vinoteca.features.grapeVariety.dto.GrapeVarietyResponseDTO;
import com.utn.vinoteca.features.grapeVariety.entity.GrapeVarietyEntity;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class GrapeVarietyMapper {

    private final ModelMapper modelMapper;

    public GrapeVarietyResponseDTO toResponse(GrapeVarietyEntity entity) {
        return modelMapper.map(entity, GrapeVarietyResponseDTO.class);
    }

    public GrapeVarietyEntity toEntity(GrapeVarietyRequestDTO dto) {
        return modelMapper.map(dto, GrapeVarietyEntity.class);
    }

    public void updateEntity(GrapeVarietyEntity entity, GrapeVarietyRequestDTO dto) {
        modelMapper.map(dto, entity);
    }
}
