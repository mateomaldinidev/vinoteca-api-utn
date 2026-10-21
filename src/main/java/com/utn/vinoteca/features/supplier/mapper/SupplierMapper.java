package com.utn.vinoteca.features.supplier.mapper;


import com.utn.vinoteca.features.supplier.dto.SupplierRequestDTO; // Importar SupplierRequestDTO
import com.utn.vinoteca.features.supplier.dto.SupplierResponseDTO;
import com.utn.vinoteca.features.supplier.entity.SupplierEntity;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;

@Component
public class SupplierMapper {

    private final ModelMapper modelMapper;

    public SupplierMapper(ModelMapper modelMapper) {
        this.modelMapper = modelMapper;
    }

    public SupplierResponseDTO toResponse(SupplierEntity supplier) {
        return modelMapper.map(supplier, SupplierResponseDTO.class);
    }


    public SupplierEntity toEntity(SupplierRequestDTO dto) {
        return modelMapper.map(dto, SupplierEntity.class);
    }


    public void updateEntity(SupplierEntity supplier, SupplierRequestDTO dto) { // Cambiado SupplierResponseDTO a SupplierRequestDTO
        modelMapper.map(dto, supplier);
    }

}