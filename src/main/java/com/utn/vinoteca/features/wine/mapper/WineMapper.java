package com.utn.vinoteca.features.wine.mapper;

import com.utn.vinoteca.features.distributor.entity.DistributorEntity;
import com.utn.vinoteca.features.grapeVariety.dto.GrapeVarietySummaryDTO;
import com.utn.vinoteca.features.grapeVariety.entity.GrapeVarietyEntity;
import com.utn.vinoteca.features.wine.dto.WineRequestDTO;
import com.utn.vinoteca.features.wine.dto.WineResponseDTO;
import com.utn.vinoteca.features.wine.entity.WineEntity;
import com.utn.vinoteca.features.wineType.entity.WineTypeEntity;
import com.utn.vinoteca.features.winery.entity.WineryEntity;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;

@Component
public class WineMapper {

    private final ModelMapper modelMapper;

    public WineMapper(ModelMapper modelMapper) {
        this.modelMapper = modelMapper;
        modelMapper.createTypeMap(WineEntity.class, WineResponseDTO.class)
                .addMappings(mapping -> mapping.skip(WineResponseDTO::setGrapeVarieties));
    }

    public WineResponseDTO toResponse(WineEntity wine) {
        WineResponseDTO response = modelMapper.map(wine, WineResponseDTO.class);

        response.setDistributorId(wine.getDistributor().getExternalId());
        response.setDistributorName(wine.getDistributor().getName());

        response.setWineTypeId(wine.getWineType().getExternalId());
        response.setWineTypeName(wine.getWineType().getName());

        response.setWineryId(wine.getWinery().getExternalId());
        response.setWineryName(wine.getWinery().getName());

        response.setGrapeVarieties(
                wine.getGrapeVarieties().stream()
                        .sorted(Comparator.comparing(GrapeVarietyEntity::getName))
                        .map(variety -> GrapeVarietySummaryDTO.builder()
                                .externalId(variety.getExternalId())
                                .name(variety.getName())
                                .build())
                        .toList()
        );

        return response;
    }

    public WineEntity toEntity(WineRequestDTO dto,
                               DistributorEntity distributor,
                               WineTypeEntity wineType,
                               WineryEntity winery,
                               Set<GrapeVarietyEntity> grapeVarieties) {
        WineEntity wine = modelMapper.map(dto, WineEntity.class);

        wine.setDistributor(distributor);
        wine.setWineType(wineType);
        wine.setWinery(winery);
        wine.setGrapeVarieties(new HashSet<>(grapeVarieties));

        return wine;
    }

    public void updateEntity(WineEntity wine,
                             WineRequestDTO dto,
                             DistributorEntity distributor,
                             WineTypeEntity wineType,
                             WineryEntity winery,
                             Set<GrapeVarietyEntity> grapeVarieties) {
        modelMapper.map(dto, wine);

        wine.setDistributor(distributor);
        wine.setWineType(wineType);
        wine.setWinery(winery);
        wine.setGrapeVarieties(new HashSet<>(grapeVarieties));
    }
}
