package com.utn.vinoteca.features.wine.mapper;

import com.utn.vinoteca.features.winery.entity.WineryEntity;
import com.utn.vinoteca.features.wineType.entity.WineTypeEntity;
import com.utn.vinoteca.features.wine.dto.WineRequestDTO;
import com.utn.vinoteca.features.wine.dto.WineResponseDTO;
import com.utn.vinoteca.features.wine.entity.WineEntity;
import com.utn.vinoteca.features.distributor.entity.DistributorEntity;
import lombok.AllArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;

@AllArgsConstructor

@Component
public class WineMapper {
    private final ModelMapper modelMapper;


    public WineResponseDTO toResponse(WineEntity wine) {
        WineResponseDTO response = modelMapper.map(wine, WineResponseDTO.class);

        response.setDistributorId(wine.getDistributor().getExternalId());
        response.setDistributorName(wine.getDistributor().getName());

        response.setWineTypeId(wine.getWineType().getExternalId());
        response.setWineTypeName(wine.getWineType().getName());

        response.setWineryId(wine.getWinery().getExternalId());
        response.setWineryName(wine.getWinery().getName());

        return response;
    }

    public WineEntity toEntity(WineRequestDTO dto,
                                  DistributorEntity distributor,
                                  WineTypeEntity wineType,
                                  WineryEntity winery) {
        WineEntity wine = modelMapper.map(dto, WineEntity.class);

        wine.setDistributor(distributor);
        wine.setWineType(wineType);
        wine.setWinery(winery);

        return wine;
    }

    public void updateEntity(WineEntity wine,
                             WineRequestDTO dto,
                             DistributorEntity distributor,
                             WineTypeEntity wineType,
                             WineryEntity winery) {
        modelMapper.map(dto, wine);

        wine.setDistributor(distributor);
        wine.setWineType(wineType);
        wine.setWinery(winery);
    }


}