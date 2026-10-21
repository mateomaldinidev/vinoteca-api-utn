package com.utn.vinoteca.features.category.service;

import com.utn.vinoteca.features.category.dto.CategoryRequestDTO;
import com.utn.vinoteca.features.category.dto.CategoryResponseDTO;

import java.util.List;
import java.util.UUID;

public interface ICategoryService {
    CategoryResponseDTO create(CategoryRequestDTO dto);
    List<CategoryResponseDTO> getAll();
    CategoryResponseDTO getByExternalId(UUID externalId);
    CategoryResponseDTO update(UUID externalId, CategoryRequestDTO dto);
    void delete(UUID externalId);
    void activate(UUID externalId);
    List<CategoryResponseDTO> getInactive();
}