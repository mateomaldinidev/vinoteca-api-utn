package com.utn.corralon.features.category.service;

import com.utn.corralon.exception.BusinessRuleException;
import com.utn.corralon.exception.ResourceNotFoundException;
import com.utn.corralon.features.category.dto.CategoryRequestDTO;
import com.utn.corralon.features.category.dto.CategoryResponseDTO;
import com.utn.corralon.features.category.entity.CategoryEntity;
import com.utn.corralon.features.category.mapper.CategoryMapper;
import com.utn.corralon.features.category.repository.CategoryRepository;
import com.utn.corralon.features.product.service.ProductCascadeService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CategoryService implements ICategoryService {

    private final CategoryRepository categoryRepository;
    private final CategoryMapper categoryMapper;
    private final ProductCascadeService productCascadeService;

    @Override
    @Transactional
    public CategoryResponseDTO create(CategoryRequestDTO dto) {
        if (categoryRepository.existsByNameAndActiveTrue(dto.getName())) {
            throw new BusinessRuleException("Category with name '" + dto.getName() + "' already exists.");
        }
        CategoryEntity entity = categoryMapper.toEntity(dto);
        if (entity.getActive() == null) {
            entity.setActive(true);
        }
        CategoryEntity saved = categoryRepository.save(entity);
        return categoryMapper.toResponse(saved);
    }

    @Override
    public List<CategoryResponseDTO> getAll() {
        return categoryRepository.findAllByActiveTrue().stream()
                .map(categoryMapper::toResponse)
                .toList();
    }

    @Override
    public CategoryResponseDTO getByExternalId(UUID externalId) {
        return categoryRepository.findByExternalIdAndActiveTrue(externalId)
                .map(categoryMapper::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with ID: ", externalId));
    }

    @Override
    @Transactional
    public CategoryResponseDTO update(UUID externalId, CategoryRequestDTO dto) {
        CategoryEntity entity = categoryRepository.findByExternalIdAndActiveTrue(externalId)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with ID: ", externalId));

        if (categoryRepository.existsByNameAndActiveTrue(dto.getName()) &&
            !entity.getName().equalsIgnoreCase(dto.getName())) {
            throw new BusinessRuleException("Category with name '" + dto.getName() + "' already exists.");
        }

        categoryMapper.updateEntity(entity, dto);
        CategoryEntity updated = categoryRepository.save(entity);
        return categoryMapper.toResponse(updated);
    }

    @Override
    @Transactional
    public void delete(UUID externalId) {
        CategoryEntity entity = categoryRepository.findByExternalIdAndActiveTrue(externalId)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with ID: ", externalId));

        // Primero se desactivan los productos relacionados y todas sus variantes.
        productCascadeService.deactivateByCategory(externalId);

        entity.setActive(false);
        categoryRepository.save(entity);
    }

    @Override
    public long countAssociatedProducts(UUID externalId) {
        categoryRepository.findByExternalIdAndActiveTrue(externalId)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with ID: ", externalId));

        return productCascadeService.countByCategory(externalId);
    }

    @Override
    @Transactional
    public void activate(UUID externalId) {
        CategoryEntity entity = categoryRepository.findByExternalId(externalId)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with ID: ", externalId));

        if (entity.getActive()) {
            throw new BusinessRuleException("Category with ID: " + externalId + " is already active.");
        }
        entity.setActive(true);
        categoryRepository.save(entity);
    }

    @Override
    public List<CategoryResponseDTO> getInactive() {
        return categoryRepository.findAllByActiveFalse().stream()
                .map(categoryMapper::toResponse)
                .toList();
    }
}