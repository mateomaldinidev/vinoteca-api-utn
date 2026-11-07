package com.utn.corralon.features.brand.mapper;

import com.utn.corralon.features.brand.dto.BrandRequestDTO;
import com.utn.corralon.features.brand.entity.BrandEntity;
import com.utn.corralon.features.brand.dto.BrandResponseDTO;
import jakarta.validation.constraints.NotNull;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;

@Component
public class BrandMapper {

    private final ModelMapper modelMapper;

    public BrandMapper(ModelMapper modelMapper) {
        this.modelMapper = modelMapper;
    }

    public BrandResponseDTO toResponse(BrandEntity brand) {
        return modelMapper.map(brand, BrandResponseDTO.class);
    }

    public BrandEntity toEntity(@NotNull BrandRequestDTO dto) {
        return modelMapper.map(dto, BrandEntity.class);
    }

    public void updateEntity(BrandEntity brand,
                             BrandRequestDTO dto) {
        Boolean active = brand.getActive();
        modelMapper.map(dto, brand);

        // La desactivacion solo se permite mediante DELETE para garantizar
        // la desactivacion en cascada de productos y variantes.
        brand.setActive(active != null ? active : Boolean.TRUE);
    }



}
