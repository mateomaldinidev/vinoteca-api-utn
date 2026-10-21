package com.utn.vinoteca.features.category.mapper;

import com.utn.vinoteca.features.category.dto.CategoryRequestDTO; // Importar CategoryRequestDTO
import com.utn.vinoteca.features.category.dto.CategoryResponseDTO;
import com.utn.vinoteca.features.category.entity.CategoryEntity;
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
        modelMapper.map(dto, category);
    }
}