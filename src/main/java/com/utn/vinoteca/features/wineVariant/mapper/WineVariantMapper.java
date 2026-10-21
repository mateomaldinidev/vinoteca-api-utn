package com.utn.vinoteca.features.wineVariant.mapper;

import com.utn.vinoteca.features.wine.entity.WineEntity;
import com.utn.vinoteca.features.wineVariant.dto.WineVariantRequestDTO;
import com.utn.vinoteca.features.wineVariant.dto.WineVariantResponseDTO;
import com.utn.vinoteca.features.wineVariant.entity.WineVariantEntity;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;

@Component
public class WineVariantMapper {
    private final ModelMapper modelMapper;

    public WineVariantMapper(ModelMapper modelMapper) {
        this.modelMapper = modelMapper;
    }

    public WineVariantResponseDTO toResponse(WineVariantEntity variant) {
        WineVariantResponseDTO dto =
                modelMapper.map(
                        variant,
                        WineVariantResponseDTO.class);
        dto.setWineId(variant.getWine().getExternalId());
        dto.setWineName(variant.getWine().getName());
        return dto;
    }

    public WineVariantEntity toEntity(WineVariantRequestDTO dto,
                                         WineEntity wine) {
        WineVariantEntity variant =
                modelMapper.map(
                        dto,
                        WineVariantEntity.class);

        variant.setWine(wine);
        return variant;
    }

    public void updateEntity(WineVariantEntity variant,
                        WineVariantRequestDTO dto,
                        WineEntity wine) {
        modelMapper.map(dto, variant);
        variant.setWine(wine);

    }
}
