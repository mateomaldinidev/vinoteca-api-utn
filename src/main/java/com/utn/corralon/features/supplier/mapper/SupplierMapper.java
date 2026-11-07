package com.utn.corralon.features.supplier.mapper;


import com.utn.corralon.features.supplier.dto.SupplierRequestDTO; // Importar SupplierRequestDTO
import com.utn.corralon.features.supplier.dto.SupplierResponseDTO;
import com.utn.corralon.features.supplier.entity.SupplierEntity;
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
        boolean active = supplier.isActive();
        modelMapper.map(dto, supplier);

        // La desactivacion solo se permite mediante DELETE para garantizar
        // la desactivacion en cascada de productos y variantes.
        supplier.setActive(active);
    }

}