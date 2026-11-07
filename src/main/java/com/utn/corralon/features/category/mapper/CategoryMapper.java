package com.utn.corralon.features.category.mapper;

import com.utn.corralon.features.category.dto.CategoryRequestDTO; // Importar CategoryRequestDTO
import com.utn.corralon.features.category.dto.CategoryResponseDTO;
import com.utn.corralon.features.category.entity.CategoryEntity;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;

@Component
public class CategoryMapper {

    private final ModelMapper modelMapper;

    public CategoryMapper(ModelMapper modelMapper) {
        this.modelMapper = modelMapper;
    }

    public CategoryResponseDTO toResponse(CategoryEntity category) {
        return modelMapper.map(category, CategoryResponseDTO.class);
    }

    public CategoryEntity toEntity(CategoryRequestDTO dto) {
        return modelMapper.map(dto, CategoryEntity.class);
    }

    public void updateEntity(CategoryEntity category,
                             CategoryRequestDTO dto) {
        Boolean active = category.getActive();
        modelMapper.map(dto, category);

        // La desactivacion solo se permite mediante DELETE para garantizar
        // la desactivacion en cascada de productos y variantes.
        category.setActive(active != null ? active : Boolean.TRUE);
    }
}