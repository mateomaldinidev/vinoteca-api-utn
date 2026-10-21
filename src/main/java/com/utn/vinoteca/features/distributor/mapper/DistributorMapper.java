package com.utn.vinoteca.features.distributor.mapper;


import com.utn.vinoteca.features.distributor.dto.DistributorRequestDTO; // Importar DistributorRequestDTO
import com.utn.vinoteca.features.distributor.dto.DistributorResponseDTO;
import com.utn.vinoteca.features.distributor.entity.DistributorEntity;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;

@Component
public class DistributorMapper {

    private final ModelMapper modelMapper;

    public DistributorMapper(ModelMapper modelMapper) {
        this.modelMapper = modelMapper;
    }

    public DistributorResponseDTO toResponse(DistributorEntity distributor) {
        return modelMapper.map(distributor, DistributorResponseDTO.class);
    }


    public DistributorEntity toEntity(DistributorRequestDTO dto) {
        return modelMapper.map(dto, DistributorEntity.class);
    }


    public void updateEntity(DistributorEntity distributor, DistributorRequestDTO dto) { // Cambiado DistributorResponseDTO a DistributorRequestDTO
        modelMapper.map(dto, distributor);
    }

}